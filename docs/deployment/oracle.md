# Calfit Oracle ARM64 포트폴리오·시연 배포

이 문서는 설정 변경과 로컬 검증 결과를 기록한다. AWS 자원 삭제, Oracle 실제 배포,
GitHub/레지스트리 Push는 수행하지 않는다. 로컬 H2와 기존 AWS Compose/Actions는 유지한다.

운영 목적은 포트폴리오와 프로젝트 시연이다. **기존 AWS DB 데이터는 이전하지 않는다.**
Oracle에는 `mysql:8.4.13`과 **신규 external 볼륨 `calfit_oracle_mysql84`**로 빈 DB를 만든다.
기존 AWS 자원과 MySQL 8.0/이전 테스트 볼륨은 삭제·업그레이드·재사용하지 않는다.

## 변경 전후

| 항목 | 기존 AWS 구성 | Oracle 전용 구성 |
| --- | --- | --- |
| Java 이미지 | Java 17 Alpine | Java 17 Jammy, ARM64 런타임 |
| 패키지 | 별도 설치 없음 | apt로 curl/ca-certificates 설치, apt 캐시 제거 |
| MySQL | mysql:8.0 | **mysql:8.4.13 고정**, 신규 DB |
| DB 포트 | 호스트 3306 공개 | 포트 매핑 없음, internal Docker 네트워크 |
| API 포트 | 호스트 8080 공개 | 127.0.0.1:8080만 바인딩 |
| 관리 엔드포인트 | API 포트에 actuator | 컨테이너 내부 8081, health만 노출 |
| DB 볼륨 | 기존 AWS/개발 볼륨 | **calfit_oracle_mysql84**, 신규 external 볼륨 |
| 초기화 | prod Hibernate update | 기본 validate, 새 DB에 한해 명시적으로 bootstrap |
| 로그 | 기존 prod Sentry YOUR_DSN 설정 | Oracle 전용 콘솔 로그, Docker 로그 회전 |
| 외부 접속 | EC2 직접 접속 | 호스트 cloudflared → 127.0.0.1:8080 |

`Dockerfile`은 BuildKit의 BUILDPLATFORM에서 Java bootJar를 빌드하고, 최종 JRE는
`--platform linux/arm64` 대상으로 선택한다. Java 애플리케이션과 의존성 버전은 바꾸지 않는다.
빌드 단계에는 기존 sed/chmod/Gradle만 필요하며 apk 명령은 없다. 런타임 curl은
관리 포트 healthcheck에 사용한다. 실행 시 exec로 Java에 종료 신호를 전달한다.

## 운영 설정과 로컬 개발 분리

Oracle에서는 **docker-compose.prod.yml 하나만** 사용한다. 기존 docker-compose.yml과
여러 `-f` 옵션으로 합치면 기존 3306/8080 공개 포트가 병합될 수 있으므로 합치지 않는다.
기존 로컬 실행 명령과 `local` H2 설정은 그대로다.

Compose가 `SPRING_PROFILES_ACTIVE=prod`를 전달해 application.yml의 local 기본값보다
우선한다. db-core.yml의 사용자 정의 HikariDataSource는 아래 기존 속성을 사용한다.

- STORAGE_DATABASE_CORE_DB_URL → mysql:3306/DB이름 (jdbc:mysql:// 접두사는 YAML에서 추가)
- STORAGE_DATABASE_CORE_DB_USERNAME / STORAGE_DATABASE_CORE_DB_PASSWORD → 애플리케이션 계정
- STORAGE_DATASOURCE_CORE_MAXIMUMPOOLSIZE → Hikari 최대 풀 (기본 10)
- STORAGE_DATASOURCE_CORE_CONNECTIONTIMEOUT → 최초 연결 대기 30초 (기존 prod 1100ms보다 여유 확보)
- SPRING_JPA_HIBERNATE_DDLAUTO → validate 기본값
- CORS_ALLOWEDORIGINS → Pages 운영 Origin 목록 (템플릿 입력 변수는 CORS_ALLOWED_ORIGINS)

대시가 있는 Spring 설정의 컨테이너 환경변수 이름은 대시를 제거하는 공식 바인딩 규칙을
따른다. 로컬 파일과 Java 설정 클래스는 수정하지 않는다.

기존 prod 로그의 Sentry DSN은 YOUR_DSN placeholder라 Oracle 전용 로그 설정을
읽기 전용으로 마운트했다. Sentry 연동은 별도 유효한 DSN과 운영 검증 후 설정한다.

기존 `spring.sql.init.mode=always`와 `shedlock.sql`은 유지된다. 따라서 JPA validate와
별개로 ShedLock 테이블의 `CREATE TABLE IF NOT EXISTS`는 실행된다. 이를 포함한 초기화
권한이 필요하다. validate가 애플리케이션의 모든 DDL/업무 쓰기를 금지하는 것은 아니다.

## Oracle에서 실행할 명령 (아직 실행하지 않음)

다음은 저장소의 BE 디렉터리에서 실행하는 Ubuntu 명령이다.

### 1. 환경변수 준비

```bash
cp -n .env.prod.example .env.prod
chmod 600 .env.prod
```

.env.prod의 비밀번호, JWT/watch secret, 실제 Pages/API Origin, S3/외부 연동 값을
직접 채운다. 비밀번호가 `$`를 포함하면 Compose 보간을 막도록 작은따옴표로 감싼다.
JWT secret은 최소 32바이트 이상의 별도 난수로 만든다. 예: `openssl rand -base64 64`.
검증 시 전체 `docker compose config` 출력에는 비밀이 포함될 수 있으므로 `--quiet`를 쓴다.

메일은 기본 false다. 실제 회원가입 인증/초대 메일을 쓰기 전에 SMTP 값을 채우고 true로
바꿔 실제 수신까지 검증한다. Google/Toss 값이 비어 있으면 해당 기능은 사용할 수 없다.

### 2. 신규 8.4 볼륨 준비 (기존 볼륨 보존)

```bash
docker volume ls
# 이 이름이 이미 있으면 신규 생성인지/사용 이력이 있는지 먼저 확인한다.
docker volume inspect calfit_oracle_mysql84
```

처음 준비하는 서버에서 위 이름이 없음을 확인한 뒤 명시적으로 생성한다.
이미 있다면 삭제하지 말고 초기화 완료 여부와 데이터 사용 이력을 확인한다.

```bash
docker volume create calfit_oracle_mysql84
```

external 볼륨은 Compose가 자동 생성/삭제하지 않는다. 기존 볼륨 삭제, `down -v`,
`docker volume prune`은 사용하지 않는다. 환경변수의 MYSQL_PASSWORD를 바꿔도 기존 볼륨의
MySQL 계정 비밀번호는 바뀌지 않는다. **.env.prod를 과거 예시로 만들었다면
MYSQL_VOLUME_NAME=calfit_oracle_mysql84로 수정한다. 이전 8.0 볼륨 이름을 지정하면 안 된다.**
AWS 데이터 덤프/복원과 기존 볼륨 연결은 이번 배포 절차에 포함하지 않는다.

### 3. 설정 확인 및 이미지 준비

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml config --quiet
docker buildx build --platform linux/arm64 --load -t calfit-backend:oracle-arm64 .
docker image inspect calfit-backend:oracle-arm64 --format '{{.Os}}/{{.Architecture}}'
```

사전 빌드 이미지를 사용하는 경우 BACKEND_IMAGE에 검증한 불변 태그나 digest를 설정하고
서버에서 pull한다. 생성한 로컬 태그와 .env.prod의 BACKEND_IMAGE가 같아야 한다.

### 4. 최초 한 번 update로 초기화한 뒤 validate로 시작

.env.prod의 **JPA_DDL_AUTO=validate를 계속 유지**한다. 빈 DB 최초 초기화 명령에만
`JPA_DDL_AUTO=update`를 앞에 붙여 일회성으로 적용한다. 명령이 끝나면 다음 일반 실행은
자동으로 validate를 사용한다. 이미 사용 중인 DB의 validate 실패를 update로 덮지 않는다.

```bash
# DB를 먼저 시작하고 신규 DB임을 확인한다.
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --wait mysql
docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE" --execute="SELECT VERSION(); SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema=DATABASE();"'
# 처음 만든 빈 DB에서만 실행한다. 예상 버전 8.4.13 / 최초 table_count=0.
JPA_DDL_AUTO=update docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-build --wait core-api
# .env.prod는 validate 그대로이므로 API가 validate 설정으로 재생성된다.
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-build --wait core-api
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=100 core-api
```

위 일회성 환경변수 문법은 Ubuntu bash 기준이다. PowerShell에서는 프로세스 환경변수를
설정했다면 반드시 복원/제거한 뒤 validate 명령을 실행한다. `prod`에서는 local 전용
DataInitializer/시연 데이터가 생성되지 않는다. 회원가입·워크스페이스 생성 등 정상 API로
시연용 계정을 준비한다. 로컬 H2 시드와 비즈니스 로직은 변경하지 않는다.

## Windows에서 Oracle로 소스 전송

이미 운영 중인 서버에서 없는 URL의 500을 404로 고치는 변경은
[수정 파일만 반영하는 절차](oracle-endpoint-fix.md)를 따른다. 기존 환경변수/볼륨을 유지하고
백엔드만 재빌드·재생성한다. 최초 DB 초기화용 update 절차를 다시 실행하지 않는다.

현재 수정/미추적 파일도 포함하는 allowlist 패키징 스크립트와 SCP·SHA-256 검증 절차는
[소스 전송 가이드](oracle-source-transfer.md)에 있다. 폴더 전체 `scp -r`나 HEAD만 담는
git archive 대신, 검토한 소스 압축본을 관리자 IP의 SSH 22번으로 전송한다. 실제 환경변수와
개인키는 압축에 포함하지 않고 .env.prod는 서버에서 별도로 작성한다. 이번 작업에서 원격
SSH/SCP와 실제 서버 배포는 실행하지 않는다.

MySQL은 root ping 대신 **애플리케이션 계정의 인증된 SELECT 1**로 초기화 완료를 확인한다.
core-api는 DB healthy 이후 시작한다. API healthcheck는 별도 8081 관리 포트에서 DB 등 상태를
확인하며, 8081은 호스트에 공개하지 않는다. restart: unless-stopped는 프로세스 종료/호스트
재부팅에 적용되며 **unhealthy만으로 컨테이너를 자동 재시작하지 않는다**. 장애 알림과 복구는
별도 운영 작업이다. [Compose 시작 순서](https://docs.docker.com/compose/how-tos/startup-order/)

## Cloudflare Tunnel / Pages

이 Compose는 **Ubuntu 호스트의 systemd cloudflared**를 전제로 한다. Tunnel 대시보드의
공개 API hostname에 origin service `http://127.0.0.1:8080`을 연결한다. Tunnel 생성·인증·실행은
이번 작업에서 하지 않았다. Docker 컨테이너의 cloudflared를 쓴다면 127.0.0.1은 그 컨테이너
자신이므로, application 네트워크에 연결하고 `http://core-api:8080`으로 구성해야 한다.

고정 공개 hostname으로 게시하려면 Cloudflare에 추가한 도메인이 필요하다. 프론트의
pages.dev 주소가 API Tunnel용 도메인까지 제공하는 것은 아니다.
[Tunnel 준비 조건](https://developers.cloudflare.com/tunnel/get-started/)

NSG 인바운드는 관리자 SSH 22만 유지한다. Tunnel의 아웃바운드 연결, DNS, 외부 API/SMTP
송신은 허용돼야 한다. database 네트워크는 internal이며 MySQL에는 외부 경로/ports가 없다.
backend는 별도 application 브리지로 외부 API에 나간다. 서버의 `ss -ltn`과 외부 접속 검사로
8080의 loopback 바인딩과 3306 미공개를 최종 확인한다.
Tunnel은 TCP/UDP 7844로 Cloudflare에 연결하므로 송신 방화벽을 제한할 경우 해당 경로를
허용한다. [Cloudflare 방화벽 조건](https://developers.cloudflare.com/tunnel/configuration/)

Pages에는 FE 저장소를 연결하고 `npm run build`, 출력 dist, Node 22.12 이상을 사용한다.
VITE_API_BASE_URL=https://API주소/api/v1로 빌드한다. 필요하면 VITE_PUBLIC_API_BASE_URL도
같은 API로 맞춘다. PUBLIC_* Origin에는 /api/v1과 끝의 /를 넣지 않는다. CORS 허용 목록은
정확한 운영 Origin만 사용하며 Preview 주소를 wildcard로 열지 않는다.

Google OAuth redirect URI는 `https://프론트주소/auth/callback`, Calendar webhook은
`https://API주소/api/v1/calendars/google/notifications`로 등록한다. 메일 링크와 Toss 결제
콜백도 새 주소에서 확인한다. 파일 저장은 현재 AWS S3를 유지하며 VM 이전만으로 R2로 바뀌지
않는다. EC2 IAM role은 OCI에 전달되지 않으므로 최소 권한 자격증명이나 별도 워크로드 인증을
준비해야 한다. .env.prod에는 실제 키를 넣되 Git/Docker build context에 포함하지 않는다.
S3 버킷 CORS에 새 Pages Origin이 허용되는지도 실제 연동 단계에서 확인한다.
이번 작업에서는 S3 코드·버킷 설정·AWS 자원을 변경하지 않는다.

## 기존 GitHub Actions 분석과 제안

BE/.github/workflows/ci.yml의 현재 흐름은 build → DockerHub latest push → SCP/SSH EC2
배포다. main은 테스트 job을 생략하며 Docker job은 build에만 의존한다. buildx는 있지만
platforms/QEMU 설정이 없어 기존 산출물이 ARM64라고 보장할 수 없다. FE workflow는 main에서
S3 sync --delete와 CloudFront invalidation을 수행한다. **이번 변경은 두 workflow를 수정하지
않는다. 추후 main Push 전 AWS 배포 job을 비활성화/분리해야 한다.**

관리자 IP만 허용된 NSG에서는 GitHub-hosted runner가 Oracle 22번에 바로 SSH할 수 없다.
GitHub IP 전체 대역 허용은 권장하지 않는다.
[GitHub runner IP 공식 안내](https://docs.github.com/en/actions/reference/runners/github-hosted-runners#ip-addresses)

권장 순서는 다음과 같다.

1. GitHub-hosted runner에서 test 후 불변 commit SHA 태그로 ARM 이미지 빌드·발행.
2. 초기에는 관리자가 기존 SSH로 접속해 해당 태그 pull 및 compose up --no-build 수행.
3. 자동화가 필요하면 내부 전용/보호된 self-hosted runner가 outbound로 작업을 받거나,
   서버가 승인된 이미지 태그를 pull하는 방식을 별도 구성한다. PR/외부 입력을 운영 VM에서
   직접 실행하는 runner는 사용하지 않는다. Cloudflare HTTP API Tunnel만으로 SSH CI가
   자동 연결되는 것은 아니며 Access/SSH 별도 구성 없이 가정하지 않는다.

이미지 빌드 job의 제안 예시(아직 workflow로 활성화하지 않음):

```yaml
# test 통과를 needs로 연결하고, GitHub environment 승인 후 이미지 발행
- uses: docker/setup-qemu-action@v3
- uses: docker/setup-buildx-action@v3
- uses: docker/build-push-action@v6
  with:
    context: .
    file: ./Dockerfile
    platforms: linux/amd64,linux/arm64
    push: true
    tags: ${{ secrets.DOCKERHUB_USERNAME }}/final-backend:${{ github.sha }}
```

BUILDPLATFORM 빌드는 JAR 컴파일을 native로 처리하지만 ARM 런타임의 apt 단계/실행 검증에는
QEMU 또는 ARM runner가 필요하다. 테스트는 H2뿐 아니라 MySQL에서 수행하고 배포는
검증한 SHA/digest를 선택한다. API만 갱신할 때 전체 down/볼륨 prune을 하지 않는다.

## 백업과 남은 작업

AWS DB 데이터는 이전하지 않는다. 아래 백업은 이후 새로 생성한 **Oracle 시연 DB**를
보존하기 위한 예시다. 기존 AWS DB/볼륨의 삭제나 재사용을 전제로 하지 않는다.

```bash
# 서버에서 실행하는 예시. 파일에는 업무 데이터가 있으므로 안전하게 보관한다.
umask 077
docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --routines --events --triggers --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"' \
  > calfit-backup.sql
```

백업은 VM 밖에 보관하고 별도 볼륨에서 복원 검사한다. 8.4 사전 검증 범위는
[MySQL 호환성 분석](mysql-8.4-compatibility.md)을 따른다. 실제 Oracle 실행, Pages/Tunnel
설정, S3·메일·Google·결제·SSE 통합 확인, 최초 운영 계정 구성은 남은 작업이다.

## 앞선 8.0/8.4 비교 검증 기록

검증일: 2026-11-25~26. 로컬 Windows Docker Desktop의 x86_64 엔진에서 QEMU로 ARM64를
실행했다. 실제 Oracle A1 하드웨어에서의 성능/기동시간은 아직 측정하지 않았다.

| 검사 | 결과 |
| --- | --- |
| buildx --platform linux/arm64 --load | 성공, 전체 bootJar 컴파일 포함 |
| 최종 이미지 / uname / Java / curl | linux/arm64, aarch64, Java 17.0.21, ARM64 curl 실행 확인 |
| Compose config 및 필수 secret 누락 | 정상 설정 통과, 빈 필수 secret 거부 |
| 실제 포트 | MySQL 3306/33060 호스트 매핑 없음, API는 loopback만 바인딩 |
| 볼륨·의존성 | external 볼륨/내부 DB 네트워크/service_healthy 확인 |
| Spring prod + MySQL 8.0.46 | healthy, 관리 포트 health=UP |
| Spring prod + MySQL 8.4.13 (검증 전용 별도 볼륨) | healthy, 관리 포트 health=UP |
| DB 재시작 후 데이터 보존 | 두 버전 모두 검증 행 유지 확인 |
| 기존 테스트 스키마의 JPA validate 재기동 | 두 버전 모두 validate 적용 및 healthy/UP 확인 |
| Connector/J 9.2.0 (두 버전) | 인증, PreparedStatement, 한글 JSON, TIMESTAMP(3), rollback 통과 |
| 기존 저장소 native SQL 7개 (두 버전) | 빈 업무 테이블에서 문법/실행 통과 |
| Pages CORS | 지정 Origin OPTIONS=200 및 Allow-Origin/credentials 확인, 미허용 Origin=403 |
| 관리 health의 API 포트 분리 (404 수정 전 기록) | API 포트에서는 health 미제공. 당시 공통 오류 처리로 500 응답, 8081에서만 UP. 이후 404 수정 결과는 oracle-endpoint-fix.md 참고 |
| 기존 local H2 로그인 API | 6개 테스트 통과 (실패 0) |
| 비즈니스/로컬 설정/기존 Compose·Actions·FE 변경 | git diff로 변경 없음 확인 |

기존 Hikari 연결 대기 1100ms로는 에뮬레이션에서 첫 JDBC handshake가 실패했다.
Oracle Compose만 30000ms로 늘린 뒤 두 DB에서 기동 성공했다. 첫 부팅은 에뮬레이션에서
약 6분 소요되어 기본 health 시작 유예 120초를 넘겼지만 이후 healthy로 회복했다.
추가 validate 검증에는 **QA 전용** start_period=600s override를 사용한다. 실제 A1에서
기동 시간을 확인해 운영 health 유예/배포 대기 시간을 조정해야 한다.
운영 유예는 .env.prod의 API_HEALTH_START_PERIOD로 설정할 수 있다 (기본 120s).

검증용 Compose 프로젝트는 calfit-oracle-qa80 / calfit-oracle-qa84로 운영 이름과 분리했다.
검증 종료 후 테스트 컨테이너/네트워크만 정리하고, 아래 새 테스트 볼륨과 로컬 빌드 이미지는
보존한다. 기존 사용자 볼륨을 삭제하거나 Oracle/AWS 서버를 변경한 작업은 없다.

- calfit_qa_mysql80_20261125
- calfit_qa_mysql84_20261125
- 로컬 이미지: calfit-backend:oracle-arm64-check (레지스트리 Push 없음)

보조 검사 소스/합성 환경변수는 Git에서 제외된 .gradle-user-codex/oracle-validation에 있다.
기존 H2 검사 재실행 명령은 아래와 같다. 환경에 따라 Gradle user home을 지정한다.

```bash
./gradlew :core:core-api:test --tests com.coffiness.calfit.api.users.login.POST_specs
```

storage:db-core:test에는 현재 구체적인 테스트 메서드가 없어 해당 task 성공을 기능 검증으로
계산하지 않았다. 위 H2 6개 결과는 실제 로그인 API 테스트 결과다.

## 신규 mysql:8.4.13 전용 검증 (2026-11-26)

별도 신규 external 볼륨 `calfit_qa_fresh_mysql8413_20261126_74de8ab6`을 만들어
고정 태그 `mysql:8.4.13`을 검증했다. 운영 볼륨 `calfit_oracle_mysql84`는 아직 생성하지 않았다.

| 검사 | 결과 |
| --- | --- |
| 신규 DB 버전 / 최초 테이블 수 | 8.4.13 / 0개 |
| 일회성 JPA update 기동 | MySQL·API healthy, 초기 테이블 38개 생성 |
| 기본 JPA validate로 API 재생성 | prod 및 validate 환경변수 확인, healthy / health=UP |
| API 재생성 후 데이터 보존 | 별도 QA 테이블의 검증 행 1개 유지 |
| 실행 이미지 | MySQL·백엔드 linux/arm64, 로컬 QEMU에서 실행 |

추가한 QA 테이블 1개를 포함한 최종 테이블 수는 39개다. 검증용 환경변수에는 합성 비밀만
사용했다. 테스트 컨테이너/네트워크는 종료하고 신규 QA 볼륨과 기존 볼륨은 보존한다.
실제 Oracle 서버, AWS S3 등 외부 서비스와 시연 업무 흐름은 아직 검증하지 않았다.

전송 스크립트는 현재 수정된 소스를 패키징하며 환경변수 템플릿만 포함한다. 실제 환경변수,
개인키, 백업·캐시 제외, 필요한 classpath SQL 포함, SHA-256 일치 여부를 로컬에서 확인했다.
Oracle로 전송하거나 배포하지 않았다.
