# Calfit Oracle ARM64 포트폴리오·시연 배포

이 문서는 설정 변경과 로컬 검증 결과를 기록한다. AWS 자원 삭제, Oracle 실제 배포,
GitHub/레지스트리 Push는 수행하지 않는다. 로컬 H2와 기존 개발용 Compose는 유지한다.

이미 운영 중인 서버의 GHCR 전환은 아래 **15-9 GHCR digest 배포 준비**를 따른다.
기존 DB에 최초 볼륨 생성/update 초기화 절차를 다시 실행하지 않는다.

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
docker image inspect calfit-backend:oracle-arm64 --format '{{.Os}}/{{.Architecture}}'
```

운영 Compose에는 build 항목이 없다. 위 inspect는 이미 준비된 로컬 이미지의 확인용이다.
운영 서버에서 소스를 빌드하지 않는다. 기존 로컬 빌드 이미지는 최초 전환 복구에 보존하고,
GHCR의 검증된 digest를 별도 이미지 버전 파일로 지정하는 절차는 아래 15-9를 따른다.

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
백엔드만 교체한다. 해당 문서의 `dc build core-api`는 이전 소스 빌드 방식의 기록이며,
15-9 이후에는 아래 GHCR pull 절차로 대체한다. 최초 DB 초기화용 update는 다시 실행하지 않는다.

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
콜백도 새 주소에서 확인한다. 파일 저장소는 아래 **Cloudflare R2 첨부파일 저장소 전환** 절차로
별도 설정한다. VM 이전만으로 저장소가 전환되지는 않는다. `S3_ENDPOINT`가 비어 있으면 기존
AWS S3 구성을 유지한다. EC2 IAM role은 OCI에 전달되지 않으므로 어느 저장소를 사용하든
해당 저장소의 자격증명이 필요하다. 실제 키는 운영 서버에서만 설정하고 Git/Docker build
context에 포함하지 않는다. 이 가이드는 기존 AWS 객체를 이동하거나 삭제하지 않는다.

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

## 15-9 GHCR digest 배포 준비

이 절은 정상 운영 중인 기존 DB를 유지하는 수동 전환 절차다. **아래 서버 명령은 이번
단계에서 실행하지 않았다.** 자동 배포 스크립트/systemd Timer는 다음 단계의 작업이다.
새 Compose 파일 없이 기존 `BACKEND_IMAGE` 보간을 재사용하고 `core-api.build`만 제거했다.
Health Check, 메모리, 네트워크, 포트, 로그, MySQL 설정은 유지한다.

### 이미지 버전과 운영 Secret 분리

- `.env.prod`: 기존 DB/JWT/S3 등 운영 설정. 복사·수정·출력하지 않는다.
- `.env.image`: 마지막으로 성공한 이미지 주소 한 줄만 보관한다.
- `.env.image.next-<시각>`: 다음 배포 후보의 GHCR digest 주소 한 줄.
- `.env.image.rollback-<시각>`: 직전 실행 이미지에 붙인 로컬 복구 태그 한 줄.

위 버전 파일은 기존 `.env.*` ignore 규칙으로 제외된다. `.env.prod.example`은 기존
로컬 fallback 값을 유지하고 두 번째 env-file 사용법만 안내한다. 버전 파일에 DB 설정이나
Secret을 넣지 않는다. 뒤에 지정한 env-file이 우선하지만 셸의 BACKEND_IMAGE는 파일보다
우선하므로 아래 명령은 이를 명시적으로 제거한다. JPA는 명령 범위에서 validate로 고정한다.
[Compose 보간 우선순위](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/)

### 1. 현재 컨테이너와 로컬 복구 이미지 보존

기존 운영 디렉터리의 Ubuntu Bash에서 실행한다. 프로젝트 이름을 바꾸면 다른 컨테이너를
만들 수 있으므로 기존 `calfit-oracle` 프로젝트를 사용한다. 별도 이름으로 운영했다면
기존 프로젝트 이름을 그대로 지정한다. 각 명령이 실패하면 이후 단계로 진행하지 않는다.

```bash
project=calfit-oracle
envfile="$PWD/.env.prod"
compose="$PWD/docker-compose.prod.yml"
stamp=$(date -u +%Y%m%dT%H%M%SZ)
base_dc() {
  env -u BACKEND_IMAGE JPA_DDL_AUTO=validate docker compose --project-name "$project" \
    --env-file "$envfile" -f "$compose" "$@"
}
base_dc config --quiet
api_id=$(base_dc ps -q core-api)
mysql_id=$(base_dc ps -q mysql)
test -n "$api_id" && test -n "$mysql_id"
test "$(docker inspect "$api_id" --format '{{.State.Running}}')" = true
test "$(docker inspect "$mysql_id" --format '{{.State.Health.Status}}')" = healthy
mysql_started=$(docker inspect "$mysql_id" --format '{{.State.StartedAt}}')
mysql_volume=$(docker inspect "$mysql_id" --format '{{range .Mounts}}{{if eq .Destination "/var/lib/mysql"}}{{.Name}}{{end}}{{end}}')
test "$mysql_volume" = calfit_oracle_mysql84
env_hash=$(sha256sum "$envfile" | cut -d ' ' -f 1)
previous_id=$(docker inspect "$api_id" --format '{{.Image}}')
test "$(docker image inspect "$previous_id" --format '{{.Os}}/{{.Architecture}}')" = linux/arm64
rollback_image="calfit-backend:rollback-$stamp"
docker image tag "$previous_id" "$rollback_image"
test "$(docker image inspect "$rollback_image" --format '{{.Id}}')" = "$previous_id"
rollback_env="$PWD/.env.image.rollback-$stamp"
(umask 077; set -o noclobber; printf 'BACKEND_IMAGE=%s\n' "$rollback_image" > "$rollback_env")
```

`.Config.Image`의 태그가 아닌 실행 컨테이너의 `.Image` ID를 보존하므로 기존 로컬 태그가
다른 이미지로 바뀌었어도 실제 실행 중이던 버전을 복구할 수 있다. 복구 확인 전에는 이
이미지·복구 파일을 삭제하거나 image/system prune을 실행하지 않는다.

### 2. 실제 발행 digest로 후보 파일 생성 및 사전 검증

15-8 main 성공 실행의 `published-image.json`에 기록된 **registry manifest digest**를
사용한다. config/image ID, JAR checksum, `latest`, SHA 태그를 digest 대신 사용하지 않는다.
아래 REPLACE 값을 실제 64자리 hex로 교체해야 형식 검사를 통과한다.

```bash
candidate_image='ghcr.io/coffiiness/calfit-backend@sha256:REPLACE_WITH_64_HEX_MANIFEST_DIGEST'
[[ "$candidate_image" =~ ^ghcr\.io/coffiiness/calfit-backend@sha256:[0-9a-f]{64}$ ]]
imagefile="$PWD/.env.image.next-$stamp"
(umask 077; set -o noclobber; printf 'BACKEND_IMAGE=%s\n' "$candidate_image" > "$imagefile")
dc_image() {
  env -u BACKEND_IMAGE JPA_DDL_AUTO=validate docker compose --project-name "$project" \
    --env-file "$envfile" --env-file "$imagefile" -f "$compose" "$@"
}
dc_image config --quiet
# 전체 config를 출력/저장하지 않고 파이프로 필요한 항목만 검사한다.
dc_image config --format json | python3 -c '
import json,sys
c=json.load(sys.stdin); a=c["services"]["core-api"]
assert a["image"] == sys.argv[1] and "build" not in a
assert a["platform"] == "linux/arm64"
assert a["environment"]["SPRING_JPA_HIBERNATE_DDLAUTO"] == "validate"
assert c["services"]["mysql"]["image"] == "mysql:8.4.13"
assert c["volumes"]["mysql_data"]["external"] is True
assert c["volumes"]["mysql_data"]["name"] == "calfit_oracle_mysql84"
print("digest / no build / ARM64 / validate / existing MySQL volume verified")
' "$candidate_image"
```

이미지 공개 여부 및 Oracle의 GHCR 읽기 권한을 먼저 확인한다. 공개 package라면 로그인
없이 pull할 수 있다. private package의 읽기 인증은 별도 준비 사항이며, Actions의
GITHUB_TOKEN을 Oracle에 복사하거나 `.env.prod`에 넣지 않는다. 이번 단계에서는 로그인이나
pull을 실행하지 않았다.

### 3. 백엔드만 Pull 및 교체

```bash
dc_image pull core-api
# Pull/아키텍처 검사가 실패하면 기존 컨테이너는 그대로 두고 중단한다.
test "$(docker image inspect "$candidate_image" --format '{{.Os}}/{{.Architecture}}')" = linux/arm64
candidate_id=$(docker image inspect "$candidate_image" --format '{{.Id}}')
dc_image up -d --no-deps --no-build --pull never --wait --wait-timeout 600 core-api
test "$(docker inspect "$(dc_image ps -q core-api)" --format '{{.Image}}')" = "$candidate_id"
dc_image exec -T core-api curl -fsS http://127.0.0.1:8081/actuator/health
curl --fail --silent --show-error --max-time 10 http://127.0.0.1:8080/health
test "$mysql_id" = "$(dc_image ps -q mysql)"
test "$mysql_started" = "$(docker inspect "$mysql_id" --format '{{.State.StartedAt}}')"
test "$env_hash" = "$(sha256sum "$envfile" | cut -d ' ' -f 1)"
# 위의 모든 확인이 성공한 뒤 이미지 버전 파일만 갱신한다.
install -m 600 "$imagefile" "$PWD/.env.image"
```

`pull core-api`는 MySQL을 대상으로 하지 않는다. `--no-deps`는 MySQL 시작/재생성을
막으며 `--no-build --pull never`는 이미 내려받은 이미지만 사용한다. 서비스 중단·재시작은
core-api에 한정되며 단일 컨테이너 교체 중 짧은 API 중단이 생길 수 있다. API_BIND_PORT를
기본 8080 외 값으로 운영한다면 호스트 health URL만 기존 포트에 맞춘다. Tunnel은 변경하지 않는다.
[Compose up 옵션](https://docs.docker.com/reference/cli/docker/compose/up/)

### 4. 교체 후 실패하면 수동 복구

위와 같은 셸에서 직전에 만든 rollback 파일을 지정한다. 셸을 다시 열었다면 기존 경로와
함수를 다시 설정하고 해당 `.env.image.rollback-<시각>`을 직접 선택한다. 이 경우 복구 태그와
ID는 아래처럼 보존 파일 및 로컬 이미지에서 다시 읽는다. ID를 다시 읽은 경우 최초에
보존한 ID와의 독립 비교는 할 수 없으므로, 복구 태그를 다른 이미지에 재사용하지 않는다.
MySQL ID/시작 시각과 env hash도 복구 전 기준으로 다시 기록한다. 이전 로컬 이미지는
GHCR pull 없이 사용할 수 있다.

```bash
# 새 셸에서만: 1단계의 project/envfile/compose 및 두 함수를 먼저 다시 설정한다.
# 실제 보존한 파일 이름으로 바꾼다. .env.prod를 source하지 않는다.
rollback_env="$PWD/.env.image.rollback-REPLACE_WITH_SAVED_TIMESTAMP"
test -f "$rollback_env"
rollback_image=$(python3 -c '
import pathlib,re,sys
text=pathlib.Path(sys.argv[1]).read_text().strip()
assert re.fullmatch(r"BACKEND_IMAGE=calfit-backend:rollback-[0-9]{8}T[0-9]{6}Z", text)
print(text.split("=",1)[1])
' "$rollback_env")
previous_id=$(docker image inspect "$rollback_image" --format '{{.Id}}')
mysql_id=$(base_dc ps -q mysql)
test -n "$mysql_id"
mysql_started=$(docker inspect "$mysql_id" --format '{{.State.StartedAt}}')
env_hash=$(sha256sum "$envfile" | cut -d ' ' -f 1)
```

```bash
imagefile="$rollback_env"
test -f "$imagefile"
test "$(docker image inspect "$rollback_image" --format '{{.Id}}')" = "$previous_id"
dc_image config --quiet
dc_image up -d --no-deps --no-build --pull never --wait --wait-timeout 600 core-api
test "$(docker inspect "$(dc_image ps -q core-api)" --format '{{.Image}}')" = "$previous_id"
dc_image exec -T core-api curl -fsS http://127.0.0.1:8081/actuator/health
test "$mysql_id" = "$(dc_image ps -q mysql)"
test "$mysql_started" = "$(docker inspect "$mysql_id" --format '{{.State.StartedAt}}')"
test "$env_hash" = "$(sha256sum "$envfile" | cut -d ' ' -f 1)"
install -m 600 "$imagefile" "$PWD/.env.image"
```

이 복구는 애플리케이션 이미지 복구이며 DB 데이터 복원은 아니다. validate 실패를 update로
우회하지 않는다. 운영/QA 볼륨, MySQL 컨테이너, 운영 env, Cloudflare 설정을 변경하지 않는다.
이 절에서는 `down`, `up` 전체 서비스 실행, MySQL pull, volume create/rm/prune을 사용하지 않는다.

후속 평상시 실행도 이미지 파일을 명시한다. `.env.prod`만으로 실행하면 기존 fallback
이미지가 선택될 수 있으므로 아래 형식을 사용한다.

```bash
imagefile="$PWD/.env.image"
dc_image config --quiet
# 실제 백엔드 교체는 위 사전 보존·pull·health 절차를 모두 수행한 경우에만 한다.
```

### 15-9 로컬 설정 검증

Docker Compose v2.40.3에서 실제 `.env.prod` 대신 합성 값과 격리한 빈 Docker CLI 설정을
사용했다. config만 실행했으며 engine/registry 요청, pull/up, 이미지 태깅, 컨테이너 및
볼륨 변경은 하지 않았다. 검증은 다음 9개 시나리오를 통과했다.

- 변경 전 기준 모델과 변경 후 로컬 모델
- BACKEND_IMAGE 미지정 fallback, 두 번째 env-file의 GHCR digest 선택
- 두 번째 env-file의 로컬 rollback 태그 선택
- 셸 BACKEND_IMAGE 우선 및 이를 제거한 후 digest 선택
- 기존 env가 update인 경우 명령 범위의 validate 우선 적용
- 필수 운영 값이 없는 config의 실패 확인

전체 JSON 모델을 비교해 build 제거 외에는 설정이 동일하고, digest 선택 시 image 이외
모든 설정이 같은 것을 확인했다. mysql:8.4.13, external `calfit_oracle_mysql84`, JPA validate,
Health Check, 메모리, 네트워크, 포트, 로그가 유지된다. 이미지 파일들의 gitignore도 확인했다.
15-8 Workflow/검증 스크립트/문서와 Dockerfile/Quality Gate/Baseline의 SHA-256도 일치한다.
실제 digest pull/ARM64 실행/운영 복구 성공은 이번 설정 검증에 포함하지 않는다.

## 15-10 Oracle 배포 요청 수신 및 수동 실행 준비

구현 파일은 `tools/deployment/deploy-oracle.sh`다. 운영 기본 경로는
`/home/ubuntu/calfit/BE`이며 기존 Compose 프로젝트 `calfit-oracle`을 사용한다.
아래 명령은 후속 운영 적용을 위한 예시이며 이번 작업에서는 서버에 접속하거나 실행하지 않았다.
systemd 서비스/Timer와 상세 자동 롤백은 이 단계에서 만들지 않았다.

### GitHub에서 전달하는 요청

Publish Job의 발행·digest/config ID 확인 및 publication Artifact 보관이 성공한 뒤
`GITHUB_TOKEN`으로 Deployments API에 요청한다. 발행 Job에만 `deployments: write`를
추가했고 기존 Gate/ARM64 빌드·검증 코드는 유지했다.

- environment: `oracle-production`, task: `deploy`
- ref: 전체 commit SHA, `auto_merge=false`
- payload: schemaVersion/repository/commitSha/image/digest/imageId/workflowRunId/workflowRunAttempt
- `required_contexts=[]`: 요청 생성 시 현재 Workflow가 아직 끝나지 않았기 때문에 사용한다.
  실제 배포는 Oracle의 전체 Run 및 세 필수 Job 성공 확인을 거쳐야 하며 Gate를 생략하지 않는다.

GitHub에 Deployment `success` 상태를 쓰는 단계는 없다. Oracle은 공개 API만 읽으며
PAT/GitHub App/GITHUB_TOKEN을 저장하거나 요구하지 않는다. 따라서 서버에서 성공한 뒤에도
GitHub Deployment의 success 표시를 자동 갱신하지 않는다. **운영 성공의 확인 지점은
로컬 `current.json`과 컨테이너 health**다. GitHub UI의 배포 요청을 운영 성공으로 해석하지 않는다.

### Oracle의 실행 전 조건

Ubuntu 24.04의 Bash, curl, jq, flock(util-linux), coreutils와 기존 Docker Engine/Compose가
필요하다. Python 3는 Mock 테스트에만 필요하다. 기존 Docker 설치를 다른 패키지로 교체하지 않는다.
실행 사용자 ubuntu는 기존 로컬 Docker socket 사용 권한이 있어야 한다.

```bash
# 필요 패키지 설치는 운영 적용 승인 후에만 실행한다.
sudo apt-get update
sudo apt-get install -y curl jq util-linux coreutils
cd /home/ubuntu/calfit/BE
chmod 700 tools/deployment/deploy-oracle.sh
bash -n tools/deployment/deploy-oracle.sh
bash tools/deployment/deploy-oracle.sh --dry-run
```

운영 Compose, read-only 로그 설정 파일과 기존 `.env.prod`는 서버에 그대로 둔다.
이 스크립트는 소스를 pull하거나 빌드하지 않고 운영 Secret을 전송·복사하지 않는다.
GHCR package가 Public이며 Ubuntu에서 익명 digest pull이 가능한지 별도로 확인해야 한다.
GitHub/레지스트리/Oracle 접속을 추가하기 위한 PAT는 요구하지 않는다.

dry-run은 공개 API/기존 로컬 이력만 검증한다. `.oracle-deploy` 디렉터리와 lock/임시 응답은
사용하지만 Docker 명령, `.env.prod` 읽기, 이미지 Pull/태깅, `.env.image` 수정, 성공 기록은 없다.
따라서 dry-run 성공은 원격 이미지 가용성이나 실제 Compose 기동 성공을 뜻하지 않는다.

### 수신 검증과 역행 방지

가장 최신 `oracle-production` 요청 하나만 검토한다. 잘못되거나 실패한 최신 요청이 있으면
이전 요청을 찾아 대신 배포하지 않는다. 요청 생성자가 GitHub Actions bot인지 확인하고,
repository/commit/digest/image ID/run ID/attempt 형식을 검사한다. 이후 공개 API로 다음을 확인한다.

1. 요청 SHA가 최신 main의 SHA와 일치
2. 기존 `.github/workflows/ci.yml` / `Backend CI/CD`의 동일 workflow ID
3. 같은 repository의 main Push, 동일 SHA/run ID/attempt, completed/success
4. 기존 품질 검증·ARM64 검증·발행 세 Job이 모두 completed/success
5. Deployment 요청이 실패/취소/완료 상태로 변경되지 않음
6. 로컬 성공 이력보다 Deployment ID 및 run/attempt가 전진함
7. 다른 commit이면 이전 성공 SHA에서 새 SHA로 GitHub compare 결과가 ahead임

main의 강제 되돌리기나 diverged 이력은 자동 배포하지 않고 수동 판단을 기다린다. 이미
성공한 digest는 재적용하지 않는다. 로컬 이력을 삭제하면 역행 방지 기준도 사라지므로
`.oracle-deploy`는 배포 사이에 유지한다. 최초 도입 시에는 로컬 성공 이력이 없으므로
최신 main/성공 Workflow/최신 요청을 기준으로 시작하며 현재 실행 이미지를 복구용으로 보존한다.

이 검증은 공개 GitHub 메타데이터와 repository의 Actions 권한을 신뢰하는 모델이다.
독립적인 서명/attestation 검증을 추가한 것은 아니다. main/Workflow 및 Actions의 쓰기 권한을
보호해야 하며 원격 응답을 source/eval하거나 원격 스크립트를 실행하지 않는다.

### 교체와 상태 파일

실제 배포는 후속 운영 적용 승인 후 아래 명령을 한 번 실행한다. 주기적 실행은 15-12에서
설정한다. `--root`는 격리 검증 또는 동일 설정의 준비 경로를 지정할 때만 사용한다.

```bash
cd /home/ubuntu/calfit/BE
bash tools/deployment/deploy-oracle.sh
```

`flock`으로 같은 운영 디렉터리의 실행을 직렬화한다. Docker는 명시적으로 로컬 Unix socket을
사용하고 상속한 DOCKER_HOST/CONTEXT 및 Compose 파일/프로젝트 override는 제거한다.
BACKEND_IMAGE를 검증한 digest로, JPA_DDL_AUTO를 validate로 명령 범위에서 지정한 뒤
실제 Compose 해석 결과도 검사하므로 상속된 다른 이미지/update 값이 적용되지 않는다.

Pull 후 RepoDigests/image ID/linux-arm64/source/revision/Java label을 검사한다. 느린 Pull 동안
새 main/요청/재실행/취소가 생기지 않았는지 API를 다시 확인하고 API 오류라면 교체하지 않는다.
원격 GitHub와 Docker 사이에 분산 트랜잭션은 없으므로 최종 확인 직후의 작은 경합 구간까지
원자적으로 막는 것은 아니다. 다음 실행은 새 최신 요청만 검토한다.

현재 backend 컨테이너의 실제 image ID에 복구 태그를 붙이고 기존 `.env.image` 원본(또는
없었다는 표시)을 보존한다. `.env.prod`는 수정하거나 백업 내용에 복사하지 않으며 hash만
전후 비교한다. `.env.image`는 같은 디렉터리의 임시 파일을 rename하여 원자적으로 갱신한다.
그 뒤 `core-api`만 `--no-deps --no-build --pull never --wait --wait-timeout 600`으로 교체한다.
기존 Compose Health Check 및 MySQL ID/시작 시각, 실행 image ID를 확인한 뒤에만 성공으로 기록한다.
단일 컨테이너 교체 중 짧은 API 중단이 생길 수 있다.

`.oracle-deploy/`는 gitignore되며 소유 사용자 전용 권한으로 관리한다.

| 경로 | 의미 |
| --- | --- |
| lock | 프로세스 간 flock |
| current.json | 마지막 정상 배포의 SHA/digest/image ID/run/attempt/Deployment ID |
| digests/<digest hex>.json | 성공한 digest 이력, 재적용 차단 |
| backups/<Deployment ID>/recovery.env | 직전 실제 실행 이미지의 로컬 복구 태그 |
| backups/<Deployment ID>/env.image.previous | 직전 이미지 버전 파일 원본 |
| backups/<Deployment ID>/env.image.absent | 이전 버전 파일이 없었음 |
| backups/<Deployment ID>/recovery.json | 이전 image ID, MySQL ID/시작 시각, env hash |
| pending.json | 진행 중/중단된 교체 복구 저널. 15-11에서 재개하며 rollback_failed는 자동 실행 차단 |

### 실패 처리 및 15-11 경계

API 오류/Rate Limit/네트워크 오류/잘못된 요청/실패 Workflow는 교체 전에 종료하므로 기존
서비스를 유지한다. Pull 실패나 image/label 불일치도 기존 컨테이너와 `.env.image`를 유지한다.
익명 GitHub API 한도는 공유되므로 짧은 주기 반복을 피한다. 예를 들어 15분 이상 주기로
설계하되 실제 Timer 및 backoff 정책은 15-12에서 결정한다. API 오류를 과거 요청으로 우회하지 않는다.

15-10에서는 교체 실패 후 pending으로 차단하는 단계까지 구현했다. **현재 동작은 아래
15-11 자동 롤백 절차로 확장했다.** 내부 실패는 자동 복구를 시도하고, 복구 실패만
rollback_failed 상태로 차단한다. `.env.image`만 보고 성공으로 판정하지 않는다.
복구 이미지/상태 파일 prune은 하지 않는다.

### 로컬 검증

```bash
bash -n tools/deployment/deploy-oracle.sh
python3 tools/deployment/test_deploy_oracle.py
```

테스트는 Python 표준 라이브러리 unittest와 실제 Bash/jq/flock을 사용한다. curl/Docker/uname은
테스트 전용 명령으로 대체하고 Linux 임시 디렉터리의 합성 `.env.prod`만 사용한다.
외부 API 또는 실제 Docker daemon과 통신하지 않는다. Windows에서는 WSL에서 실행한다.
Workflow는 actionlint로 YAML/context/Job 의존성을 검사한다. 프로젝트 품질 도구/Gradle Gate에
새 검사를 추가하지 않았고 전체 Gradle 테스트나 ARM64 이미지 빌드를 반복하지 않았다.

이번 로컬 결과: Bash 구문/actionlint 검사 통과, Workflow inline Bash 8개 구문 통과,
기존 Gate/ARM64 및 기존 Publish 본문 보존 비교 통과, Linux Mock 테스트 **14개 성공/실패 0**.
신규/최초 배포, 같은 digest/과거 digest skip, 잘못된 digest·repository·creator·SHA,
실패/진행 중/다른 Workflow·skip Job, API/Rate Limit/네트워크 오류, Pull/ID/아키텍처/label 불일치,
Pull 중 main/요청/attempt 변경, 역행/diverged 이력, 실제 flock 경합, 실패 health/교체,
손상된 로컬 이력 및 중단 pending 차단을 확인했다. 모든 API/Docker는 대역이었다.
실제 main Push의 API 요청 생성과 Oracle의 실제 익명 조회·Pull·기동은 후속 검증 대상이다.

참고: [Deployments API](https://docs.github.com/en/rest/deployments/deployments),
[공개 Workflow Run API](https://docs.github.com/en/rest/actions/workflow-runs),
[공개 Deployment Status API](https://docs.github.com/en/rest/deployments/statuses).

## 15-11 Health Check 및 자동 Rollback

기존 스크립트·Compose를 재사용한다. Workflow, Java/Gradle Gate/Baseline, Dockerfile,
운영 Compose와 Secret은 변경하지 않았다. 다음은 코드의 동작 및 향후 운영 명령이며,
이번 작업에서 실제 Oracle/GHCR/Cloudflare/DB에 연결하거나 배포하지 않았다.

### 내부 실패와 외부 장애 구분

신규 배포와 롤백 모두 기존 `up --wait --wait-timeout 600`을 사용하고, 이후 아래 내부
검사를 모두 통과해야 정상으로 판단한다. 내부 curl에는 10초 제한을 둔다.

| 검사 | 정상 조건 | 실패 시 처리 |
| --- | --- | --- |
| 백엔드 컨테이너 | 실행 중, 예상 image ID, Docker healthy | 신규 배포는 롤백, 복구 중이면 차단 |
| 내부 관리 포트 | 컨테이너의 127.0.0.1:8081/actuator/health JSON status=UP | 동일 |
| 호스트 API | http://127.0.0.1:8080/health HTTP 200 | 동일 |
| MySQL/운영 env | 기존 container ID·시작 시각·external volume 및 env SHA-256 유지 | 외부 변경이면 복구도 차단하여 수동 확인 |
| 외부 API | https://api.calfit.co.kr/health HTTP 200, 최대 15초 | 경고와 externalHealth=degraded, 내부 정상 버전 유지 |

외부 API의 HTTP 오류·네트워크 오류만으로 롤백하지 않는다. 외부 응답 본문이나 운영 Secret을
로그에 쓰지 않는다. 이 단계는 배포 직후 검사이며 상시 모니터링/Timer는 15-12 범위다.

### 교체 전 보존 및 버전 설정 일치

배포 전에 이전 `.env.image`를 강제 BACKEND_IMAGE 없이 Compose로 해석하고, 해당 로컬
이미지 ID가 실제 실행 중인 image ID와 같은지 확인한다. 버전 파일이 없으면 `.env.prod`의
기존 fallback으로 같은 검사를 한다. 다른 이미지로 바뀐 오래된 태그나 설정이면 **배포 전에
중단**한다. 버전 파일에는 BACKEND_IMAGE 한 항목과 선택적 주석/빈 줄만 허용한다.

기존 이미지의 로컬 rollback 태그와 recovery.json/recovery.env, 이전 이미지 버전 파일,
기존 current.json 원본(또는 없었다는 표시)을 모두 보존한다. rollback 태그가 다른 image ID로
바뀌거나 캐시가 사라지면 자동 Pull/Build로 대체하지 않고 차단한다.

### 자동 복구 순서

1. 교체 시작 전 실패는 기존 서비스/버전 파일을 유지한다.
2. 교체 시작 이후 내부 기동·Health 실패는 같은 flock 안에서 recovery.json을 검증한다.
3. MySQL ID/시작/volume과 `.env.prod` hash가 보존됐는지 먼저 확인한다. 변경됐다면 더 건드리지 않는다.
4. 이전 로컬 rollback 태그의 image ID, 원본 버전 설정이 선택하는 image ID를 확인한다.
5. JPA validate를 강제하고 core-api만 `--no-deps --no-build --pull never --wait --wait-timeout 600`으로 복구한다.
6. 이전 image ID 및 신규 배포와 동일한 내부 Health Check를 모두 확인한다.
7. `.env.image` 원본을 같은 디렉터리 임시 파일/rename으로 복원한다. 처음에 없었다면 파일을 제거해 없던 상태로 돌린다.
8. 복원된 자연스러운 Compose 이미지 선택이 실제 복구 이미지 ID와 같은지 다시 검사한다.
9. 이전 current.json을 정확히 유지/복원하고 실패 요청·digest를 기록한 뒤 pending을 해제한다.

복구 실행에 쓰는 로컬 태그와 복원한 버전 파일의 GHCR 주소/로컬 태그 문자열은 달라도
**image ID는 같아야 한다.** 따라서 최초 로컬 이미지와 이전 GHCR 이미지를 모두 복구할 수
있으며, 복원 후 평상시 Compose 실행이 다른 이미지를 선택하지 않도록 검사한다.
MySQL 재시작, SQL 실행, 스키마 변경, 데이터 복원은 하지 않는다. 애플리케이션 이미지
롤백은 배포 후 발생한 업무 데이터 변경을 되돌리는 작업이 아니다.

### 상태 파일과 재실행

| 경로/상태 | 의미 |
| --- | --- |
| current.json | 마지막 정상 버전. 실패한 신규 버전으로 갱신하지 않음 |
| backups/<ID>/current.previous.json 또는 current.absent | 실패/중단 복구를 위한 이전 성공 이력 원본/없음 표시 |
| failures/requests/<ID>.json | 실패 요청 및 rolled_back 결과 |
| failures/digests/<hex>.json | 실패 digest 격리. 새 요청이어도 같은 digest는 자동 재배포하지 않음 |
| pending.json: in_progress | 교체 진행 또는 중단. 다음 실행은 새 API 조회보다 복구를 먼저 함 |
| pending.json: rolling_back | 복구 도중 중단. 동일 복구를 다시 확인·수행 |
| pending.json: rollback_failed | 자동 복구 실패. 이후 일반 실행은 Docker/API 호출 없이 차단 |

pending의 phase=prepared는 Compose 교체 실행 전, replacing은 교체 실행 직전을 뜻한다.
prepared 상태에서 중단되면 정상인 기존 컨테이너를 재생성하지 않고 버전 설정만 복구한다.
복구 도중 재실행했을 때도 이전 이미지가 이미 내부 정상이라면 불필요한 재생성을 생략한다.

롤백 성공 시 실패했던 배포 실행은 종료 코드 1을 유지하므로 실패를 숨기지 않는다. 다음
실행은 같은 실패 digest를 skip하며, 다른 정상 최신 요청은 이전 정상 이력 기준으로 처리한다.
중단된 배포를 다음 실행에서 성공적으로 복구한 경우는 복구만 수행하고 종료 코드 0으로
끝낸다. 새 요청은 그 다음 실행에서 확인한다. API 장애가 있어도 저장된 로컬 복구는 가능하다.

성공 이력을 쓰는 중간에 중단됐더라도 pending이 남아 있으면 보수적으로 이전 상태로
복구한다. 이때 해당 시도의 불완전한 성공 digest marker도 제거한다. 메타데이터가 손상됐거나
기존 15-10 백업으로 이전 이력을 확정할 수 없으면 추정해서 덮어쓰지 않고 차단한다.
pending이 없는 기존 15-10의 실패 없는 성공 이력은 그대로 사용한다.

### 수동 확인과 명시적 복구

자동 복구 실패 시 pending을 지워서 새 배포를 강제로 시작하지 않는다. 먼저 로컬 상태와
보존 파일만 검사한다. 아래 명령은 실제 서버에 아직 실행하지 않았다.

```bash
cd /home/ubuntu/calfit/BE
# 상태 메타데이터에는 운영 env 원문이 없다. .env.prod 또는 전체 Compose config는 출력하지 않는다.
jq '{status,deploymentId,digest,backup}' .oracle-deploy/pending.json
bash tools/deployment/deploy-oracle.sh --dry-run --recover
```

pending에 기록된 backup의 recovery.json과 이전 버전 파일, 로컬 rollback 이미지 ID를
대조한다. 로컬 이미지 캐시 손실, 태그 변경, MySQL 상태 변경, 운영 env hash 차이 또는
내부 Health 원인을 운영자가 먼저 확인한다. 스크립트가 MySQL이나 운영 env를 자동 변경하여
문제를 덮지 않는다. 운영 Secret을 채팅/로그에 출력하지 않는다.

```bash
# 원인 확인 및 복구 준비가 끝난 뒤에만 실행한다. 새 버전 배포는 하지 않는다.
bash tools/deployment/deploy-oracle.sh --recover
```

`--recover`는 rollback_failed에 대한 명시적 재복구를 허용하지만 image/버전/DB/env/Health
검사를 완화하지 않는다. 성공해야 pending이 해제된다. pending이 없으면 아무 배포도 하지 않고
종료한다. 이 절차로도 검증할 수 없다면 상태·복구 파일을 보존하고 수동 장애 조치를 진행한다.
상태 폴더 전체 삭제, 실패 marker의 무조건 삭제, current.json의 임의 성공 기록은 하지 않는다.

### 검증 범위

기존 Mock 테스트 14개를 유지하고 변경된 자동 복구 동작에 맞춰 실패 처리 단언만 갱신했다.
Python 표준 라이브러리와 실제 Bash/jq/flock으로 신규/내부 실패/외부만 실패, 로컬·GHCR
복구, absent 버전 파일 복원, 실패 digest 차단 후 새 정상 요청, rollback 실패·중단·성공 이력
기록 도중 중단, 수동 재복구, MySQL/env 보존을 추가 검사한다. 강제 중단은 Mock 명령이 테스트
배포 프로세스에 SIGKILL을 보내 재현한다. 실제 Docker/API/운영 env에는 연결하지 않는다.

최종 Linux Mock 결과는 **28개 성공, 실패 0개**(기존 14개 유지, 14개 추가)이며
88.015초에 완료했다. Bash 구문 검사, 문서의 Bash 예시 구문 검사 및 git diff --check도
통과했다. Workflow/Gradle Gate/Baseline/Dockerfile/운영 Compose의 변경이 없는 것도 확인했다.
이는 격리 Mock 결과이며 실제 Oracle의 기동·네트워크·롤백 성공을 검증한 결과는 아니다.

## 15-12 systemd Service 및 Timer — /opt 운영 경로

root 관리 운영 경로는 `/opt/calfit/BE`다. `/home/ubuntu`와 기존 하위 디렉터리의 소유권·권한은
변경하지 않는다. 기존 `/home/ubuntu/calfit/BE`는 전환 성공 전까지 파일과 상태를 그대로 보존한다.
아래는 **향후 서버 내부에서 수행할 절차**이며 이번 작업에서는 서버 접속·권한 변경·복사·배포를
하지 않았다. 이전 절의 /home 경로는 초기 운영 기록이다. 설치된 스크립트의 기본 경로도 유지하므로
현재 Service와 모든 /opt 운영·복구 호출은 반드시 `--root /opt/calfit/BE`를 전달한다.

### Service와 Timer

- root/root oneshot, WorkingDirectory=/opt/calfit/BE.
- ExecStart=/usr/local/sbin/calfit-deploy-oracle --root /opt/calfit/BE.
- Docker/network 의존성, 40분 제한, Restart=no, journald는 기존과 같다.
- Timer 파일은 변경하지 않는다. OnCalendar 10분 주기와 Persistent=true를 유지한다.
- 기존 flock/pending/실패 digest 동작을 재사용한다. 소유권 인계는 /opt의 복사본에만 적용한다.

Service가 실행 중이면 같은 Service를 중복 시작하지 않으며 직접 호출은 flock으로 방어한다.
Persistent Timer는 활성화 직후 실행될 수도 있으므로 **최초 수동 전환·배포 검증 완료 전에는
Timer를 활성화하지 않는다.** 별도 cron/무한 루프나 ubuntu의 Docker 그룹 가입은 필요 없다.

### 1. 실행을 동결하고 원본 상태 확인

기존 Timer가 설치돼 있다면 비활성화하고 기존 Service/직접 실행 중인 배포가 끝날 때까지 기다린다.
원본과 /opt 복사본은 lock 경로가 다르므로 전환 중에는 어느 쪽에서도 다른 수동 배포를 실행하지
않는다. pending이 있으면 먼저 기존 환경의 복구 문제를 해결한 뒤 전환을 다시 준비한다. pending을
복사해 새 경로에서 무작정 재개하거나 이력을 초기화하지 않는다.

```bash
# 이미 설치된 Timer가 있는 경우에만:
sudo systemctl disable --now calfit-deploy.timer
sudo systemctl status calfit-deploy.service --no-pager
# 위 상태가 실행 중이라면 여기서 기다리고 아래로 진행하지 않는다.
old=/home/ubuntu/calfit/BE
app=/opt/calfit/BE
sudo test ! -e "$old/.oracle-deploy/pending.json" || exit 1
command -v bash curl jq flock sha256sum mktemp realpath grep systemd-analyze
sudo docker --host unix:///var/run/docker.sock version
sudo docker compose version
# 부족한 경우에만 curl/jq/util-linux/coreutils/grep을 준비한다. Docker는 교체하지 않는다.
api_id=$(sudo docker --host unix:///var/run/docker.sock ps -q --filter label=com.docker.compose.project=calfit-oracle --filter label=com.docker.compose.service=core-api)
mysql_id=$(sudo docker --host unix:///var/run/docker.sock ps -q --filter label=com.docker.compose.project=calfit-oracle --filter label=com.docker.compose.service=mysql)
test -n "$api_id" && test -n "$mysql_id" || exit 1
previous_id=$(sudo docker --host unix:///var/run/docker.sock inspect "$api_id" --format '{{.Image}}')
mysql_started=$(sudo docker --host unix:///var/run/docker.sock inspect "$mysql_id" --format '{{.State.StartedAt}}')
mysql_volume=$(sudo docker --host unix:///var/run/docker.sock inspect "$mysql_id" --format '{{range .Mounts}}{{if eq .Destination "/var/lib/mysql"}}{{.Name}}{{end}}{{end}}')
test "$mysql_volume" = calfit_oracle_mysql84 || exit 1
env_hash=$(sudo sha256sum "$old/.env.prod" | cut -d ' ' -f 1)
rollback_image="calfit-backend:before-opt-$(date -u +%Y%m%dT%H%M%SZ)"
sudo docker --host unix:///var/run/docker.sock image tag "$previous_id" "$rollback_image"
```

### 2. 필요한 운영 파일만 서버 내부에서 복사

`/opt`, `/usr/local/sbin`, `/etc/systemd/system`의 root 소유권·상위 쓰기 권한을 확인한다.
원본의 Compose/logback은 검토한 버전이어야 하며, 복사본의 공개 설정 hash를 검토본과 대조한다.
원본 `.env.prod`와 선택적 `.env.image`는 서버 내부에서만 복사한다. 내용을 출력하거나 PC/GitHub로
전송하지 않는다. Secret 파일은 cmp의 종료 코드와 hash로만 동일성을 확인한다.

목적지가 이미 있으면 덮어쓰지 말고 기존 사용 이력을 확인한다. 다음 예시는 빈 /opt 전용 경로를
처음 준비하는 경우다. 전체 저장소나 소스·캐시·개인키·DB 데이터를 복사할 필요는 없다.

```bash
sudo namei -l /opt
sudo namei -l /usr/local/sbin
sudo test ! -e /opt/calfit/BE || exit 1
sudo test ! -L /opt/calfit || exit 1
sudo install -d -o root -g root -m 0755 /opt/calfit "$app" "$app/deploy" "$app/deploy/oracle"
for file in docker-compose.prod.yml deploy/oracle/logback.xml .env.prod; do
  sudo test -f "$old/$file" && sudo test ! -L "$old/$file" || exit 1
done
sudo install -o root -g root -m 0644 "$old/docker-compose.prod.yml" "$app/docker-compose.prod.yml"
sudo install -o root -g root -m 0644 "$old/deploy/oracle/logback.xml" "$app/deploy/oracle/logback.xml"
sudo install -o root -g root -m 0600 "$old/.env.prod" "$app/.env.prod"
if sudo test -e "$old/.env.image"; then
  sudo test ! -L "$old/.env.image" || exit 1
  sudo install -o root -g root -m 0600 "$old/.env.image" "$app/.env.image"
fi
sudo cmp --silent "$old/.env.prod" "$app/.env.prod" || exit 1
test "$env_hash" = "$(sudo sha256sum "$old/.env.prod" | cut -d ' ' -f 1)" || exit 1
test "$env_hash" = "$(sudo sha256sum "$app/.env.prod" | cut -d ' ' -f 1)" || exit 1
sudo cmp --silent "$old/docker-compose.prod.yml" "$app/docker-compose.prod.yml" || exit 1
sudo cmp --silent "$old/deploy/oracle/logback.xml" "$app/deploy/oracle/logback.xml" || exit 1
sudo install -d -o root -g root -m 0700 "$app/.oracle-deploy"
if sudo test -d "$old/.oracle-deploy"; then
  sudo test ! -L "$old/.oracle-deploy" || exit 1
  test -z "$(sudo find "$old/.oracle-deploy" -type l -print -quit)" || exit 1
  sudo cp -a -- "$old/.oracle-deploy/." "$app/.oracle-deploy/"
fi
# 아래 권한 변경은 새 /opt 복사본에만 적용한다.
sudo chown -R --no-dereference root:root "$app/.oracle-deploy"
sudo find "$app/.oracle-deploy" -xdev -type d -exec chmod 0700 {} +
sudo find "$app/.oracle-deploy" -xdev -type f -exec chmod 0600 {} +
sudo namei -l "$app/docker-compose.prod.yml"
sudo stat -c '%U:%G %a %n' "$app" "$app/docker-compose.prod.yml" "$app/.env.prod" "$app/.oracle-deploy"
sudo -u ubuntu test ! -w "$app" || exit 1
sudo -u ubuntu test ! -w "$app/docker-compose.prod.yml" || exit 1
```

원본의 성공/실패 digest 이력과 복구 자료도 보존하며 lock의 실행 상태를 복사해 이어받는 것은
아니다. 과거 성공 기록의 backup 필드에 /home 경로가 있으면 감사 기록으로 그대로 둔다.
pending이 없는 상태에서만 복사하고, 새로운 배포의 backup은 /opt에서 생성한다. 기존 경로의
과거 복구 자료도 임의로 삭제하지 않는다. chmod/chown/find 권한 변경 대상은 /opt뿐이다.

### 3. 검토한 실행 파일과 unit 설치

검토된 비민감 파일을 서버의 별도 staging 경로에 준비한다. 원본 운영 디렉터리의 파일을
덮어쓰지 않는다. 아래 review는 검토된 배포 스크립트와 수정 Service, 기존 Timer가 있는 경로다.
검토 시 고정한 SHA-256을 채워 source와 설치 복사본을 대조한다. Timer 내용은 기존과 같아야 한다.

```bash
review=/tmp/calfit-reviewed
script_sha=REPLACE_WITH_REVIEWED_SCRIPT_SHA256
service_sha=REPLACE_WITH_REVIEWED_SERVICE_SHA256
timer_sha=REPLACE_WITH_UNCHANGED_TIMER_SHA256
for digest in "$script_sha" "$service_sha" "$timer_sha"; do
  [[ "$digest" =~ ^[0-9a-f]{64}$ ]] || exit 1
done
printf '%s  %s\n' "$script_sha" "$review/tools/deployment/deploy-oracle.sh" \
  "$service_sha" "$review/deploy/oracle/systemd/calfit-deploy.service" \
  "$timer_sha" "$review/deploy/oracle/systemd/calfit-deploy.timer" | sha256sum --check --strict - || exit 1
sudo test ! -L /usr/local/sbin/calfit-deploy-oracle || exit 1
sudo test ! -L /etc/systemd/system/calfit-deploy.service || exit 1
sudo test ! -L /etc/systemd/system/calfit-deploy.timer || exit 1
sudo install -o root -g root -m 0750 "$review/tools/deployment/deploy-oracle.sh" /usr/local/sbin/calfit-deploy-oracle
sudo install -o root -g root -m 0644 "$review/deploy/oracle/systemd/calfit-deploy.service" /etc/systemd/system/calfit-deploy.service
sudo install -o root -g root -m 0644 "$review/deploy/oracle/systemd/calfit-deploy.timer" /etc/systemd/system/calfit-deploy.timer
printf '%s  %s\n' "$script_sha" /usr/local/sbin/calfit-deploy-oracle \
  "$service_sha" /etc/systemd/system/calfit-deploy.service \
  "$timer_sha" /etc/systemd/system/calfit-deploy.timer | sudo sha256sum --check --strict - || exit 1
sudo systemd-analyze verify /etc/systemd/system/calfit-deploy.service
sudo systemctl daemon-reload
sudo env -i PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin HOME=/root \
  /usr/local/sbin/calfit-deploy-oracle --root /opt/calfit/BE --dry-run
```

### 4. 최초 전환: 같은 로컬 이미지로 백엔드 bind mount만 이동

성공 digest 이력을 복사하면 스크립트가 같은 digest를 skip할 수 있다. 따라서 Service의 경로를
바꾼 것만으로 전환이 끝났다고 판단하지 않는다. 기존과 동일한 이미지 ID를 사용해 core-api만
한 번 재생성하고 logback bind mount가 새 경로인지 확인한다. MySQL은 재생성하지 않는다.
다음 함수는 운영 env를 source하지 않으며 내용을 출력하지 않는다.

```bash
set -o pipefail
compose_at() {
  local base=$1; shift
  local files=(--env-file "$base/.env.prod")
  if sudo test -f "$base/.env.image"; then files+=(--env-file "$base/.env.image"); fi
  sudo env -u DOCKER_HOST -u DOCKER_CONTEXT -u DOCKER_TLS_VERIFY -u DOCKER_CERT_PATH \
    -u BACKEND_IMAGE -u COMPOSE_FILE -u COMPOSE_PROJECT_NAME -u COMPOSE_ENV_FILES -u COMPOSE_PROFILES \
    JPA_DDL_AUTO=validate docker --host unix:///var/run/docker.sock compose \
    --project-name calfit-oracle --project-directory "$base" "${files[@]}" -f "$base/docker-compose.prod.yml" "$@"
}
compose_at "$app" config --quiet || exit 1
compose_at "$app" config --format json | jq -e '
.services["core-api"] as $a |
($a | has("build") | not) and $a.environment.SPRING_JPA_HIBERNATE_DDLAUTO=="validate" and
any($a.volumes[]; .target=="/app/config/logback-oracle.xml" and .source=="/opt/calfit/BE/deploy/oracle/logback.xml" and .read_only==true) and
.services.mysql.image=="mysql:8.4.13" and .volumes.mysql_data.external==true and .volumes.mysql_data.name=="calfit_oracle_mysql84"
' >/dev/null || exit 1
selected_image=$(compose_at "$app" config --format json | jq -er '.services["core-api"].image')
test "$(sudo docker --host unix:///var/run/docker.sock image inspect "$selected_image" --format '{{.Id}}')" = "$previous_id" || exit 1
# Pull/Build 없이 캐시된 동일 이미지로 backend만 재생성한다.
compose_at "$app" up -d --no-deps --no-build --pull never --force-recreate --wait --wait-timeout 600 core-api || exit 1
new_api_id=$(compose_at "$app" ps -q core-api)
test "$(sudo docker --host unix:///var/run/docker.sock inspect "$new_api_id" --format '{{.Image}}')" = "$previous_id" || exit 1
sudo docker --host unix:///var/run/docker.sock inspect "$new_api_id" | jq -e '
.[0].State.Health.Status=="healthy" and
any(.[0].Mounts[]; .Destination=="/app/config/logback-oracle.xml" and .Source=="/opt/calfit/BE/deploy/oracle/logback.xml" and .RW==false)
' >/dev/null || exit 1
sudo docker --host unix:///var/run/docker.sock exec "$new_api_id" curl -fsS --max-time 10 http://127.0.0.1:8081/actuator/health | jq -e '.status=="UP"' >/dev/null || exit 1
curl -fsS --max-time 10 http://127.0.0.1:8080/health || exit 1
test "$mysql_id" = "$(compose_at "$app" ps -q mysql)" || exit 1
test "$mysql_started" = "$(sudo docker --host unix:///var/run/docker.sock inspect "$mysql_id" --format '{{.State.StartedAt}}')" || exit 1
test "$mysql_volume" = "$(sudo docker --host unix:///var/run/docker.sock inspect "$mysql_id" --format '{{range .Mounts}}{{if eq .Destination "/var/lib/mysql"}}{{.Name}}{{end}}{{end}}')" || exit 1
test "$env_hash" = "$(sudo sha256sum "$old/.env.prod" | cut -d ' ' -f 1)" || exit 1
test "$env_hash" = "$(sudo sha256sum "$app/.env.prod" | cut -d ' ' -f 1)" || exit 1
```

단일 backend 재생성 동안 짧은 중단이 생길 수 있다. 이 단계 실패 시 Timer는 비활성 상태로 두고,
보존한 기존 경로와 같은 캐시 이미지로 `compose_at "$old" up -d --no-deps --no-build --pull never
--force-recreate --wait --wait-timeout 600 core-api`를 수동 검토 후 사용해 이전 bind mount로 되돌릴 수
있다. 이 명령도 기존 파일의 내용·소유권·권한을 변경하지 않는다. 기존 이미지 태그와 두 경로의
운영 파일·이력은 전환 성공 전까지 삭제하지 않는다. down/volume 생성·삭제/MySQL Pull은 하지 않는다.

### 5. 실제 수동 배포 검증 후 Timer 활성화

```bash
sudo systemctl start calfit-deploy.service
sudo systemctl show calfit-deploy.service -p WorkingDirectory -p ExecStart -p Result -p ExecMainStatus
sudo journalctl -u calfit-deploy.service -n 100 --no-pager
sudo test ! -e /opt/calfit/BE/.oracle-deploy/pending.json || exit 1
sudo jq '{status,commitSha,digest,imageId,configDigest,localImageId,externalHealth}' /opt/calfit/BE/.oracle-deploy/current.json
```

Result=success/ExecMainStatus=0에 더해 실제 승인된 main digest·실행 image ID·내부 health와
새 bind mount를 확인한다. 요청 없음/이미 배포됨 skip 또는 dry-run 종료 코드 0만으로 최초 배포를
검증했다고 판단하지 않는다. 최초 /opt 전환 및 수동 배포 확인이 모두 끝난 경우에만 다음을 실행한다.

```bash
# 최초 실제 전환/수동 배포 검증 완료 전에는 실행하지 않는다.
sudo systemctl enable --now calfit-deploy.timer
sudo systemctl list-timers --all calfit-deploy.timer
sudo systemctl show calfit-deploy.timer -p NextElapseUSecRealtime -p LastTriggerUSec
```

### 6. 로그·상태·복구

```bash
sudo systemctl disable --now calfit-deploy.timer
# 이미 실행 중인 Service/수동 작업은 종료할 때까지 기다린다.
sudo systemctl status calfit-deploy.service --no-pager
sudo journalctl -u calfit-deploy.service --since '1 hour ago' -n 100 --no-pager
sudo jq '{status,commitSha,digest,imageId,configDigest,localImageId,externalHealth}' /opt/calfit/BE/.oracle-deploy/current.json
if sudo test -f /opt/calfit/BE/.oracle-deploy/pending.json; then
  sudo jq '{status,phase,deploymentId,digest,backup}' /opt/calfit/BE/.oracle-deploy/pending.json
fi
if sudo test -d /opt/calfit/BE/.oracle-deploy/failures; then
  sudo find /opt/calfit/BE/.oracle-deploy/failures -type f -name '*.json' -print
fi
sudo env -i PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin HOME=/root \
  /usr/local/sbin/calfit-deploy-oracle --root /opt/calfit/BE --dry-run --recover
# 15-11의 원인 확인 및 복구 준비를 마친 뒤에만:
sudo env -i PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin HOME=/root \
  /usr/local/sbin/calfit-deploy-oracle --root /opt/calfit/BE --recover
sudo test ! -e /opt/calfit/BE/.oracle-deploy/pending.json
sudo systemctl reset-failed calfit-deploy.service
```

state와 복구 자료는 root로 검사한다. pending/실패 marker를 지워서 우회하지 않는다. 복구 후
실제 이미지·내부 health와 정상 이력을 확인한 뒤에만 Timer를 재활성화한다. Cloudflare Tunnel은
기존 `127.0.0.1:8080` 연결 및 systemd 서비스를 그대로 사용한다.

### 경로 수정의 로컬 검증 범위

이번 수정은 Service와 이 문서뿐이다. 임시 루트에서 실행 복사본과 Docker/target 검증용 fixture를
이용한 `systemd-analyze verify`는 Service만 대상으로 종료 코드 0, 경고 없이 통과했다.
Service 변경은 WorkingDirectory와 ExecStart의 --root 두 항목뿐이며, Timer와 기존 코드 등
보호 대상 8개 파일의 SHA-256이 그대로인 것을 확인했다. 실제 /opt, /usr/local/sbin,
/etc/systemd에 설치하거나 서버 파일을 복사하지 않았다. Timer 일정 검증, 기존 Mock/Gradle
테스트 및 이미지 빌드는 재실행하지 않았다. 실제 mount 전환/MySQL 보존/운영 기동은 위
후속 수동 적용에서 확인한다.

## Docker 이미지 저장 방식별 Image ID 호환성

Oracle의 containerd image store에서 `docker image inspect .Id`는 manifest digest일 수 있다.
기존 저장 방식에서는 config digest인 경우가 있어 Deployment payload의 imageId와 로컬 .Id를
직접 비교하지 않는다. Docker/Storage Driver를 변경하지 않고 다음 순서로 식별자를 검증한다.

1. 요청의 정확한 `ghcr.io/coffiiness/calfit-backend@sha256:...`를 Pull한다. 태그로 대체하지 않는다.
2. 동일 digest 참조의 registry manifest를 inspect하고 단일 OCI/Docker schema 2 이미지인지 확인한다.
3. manifest의 config.digest가 Deployment payload.imageId와 같아야 한다. 조회 실패/다른 config는 중단한다.
4. 해당 참조의 로컬 .Id가 검증한 config digest 또는 요청 manifest digest 중 하나와 같아야 한다.
5. 기존 RepoDigests, linux/arm64, revision, source, Java runtime label 검사도 모두 통과해야 한다.
6. 실제 컨테이너 .Image 및 내부 health 검사는 확인한 로컬 .Id를 사용한다.

식별자는 pending.json/current.json과 그 성공/실패 기록에 아래 의미로 보존한다.

| 필드 | 의미 |
| --- | --- |
| digest / manifestDigest | GHCR의 불변 registry manifest digest |
| imageId | 기존 API/상태 필드와 동일한 Deployment payload.imageId, 즉 config digest |
| configDigest | 조회한 manifest의 config.digest. imageId와 같아야 함 |
| localImageId | Oracle에서 실제 확인한 docker image inspect .Id. 컨테이너 .Image 비교용 |

기존 imageId 값을 로컬 ID로 덮어쓰지 않는다. 예전 classic 저장 방식의 성공 기록은
localImageId가 없으므로 운영 검증 명령은 `.localImageId // .imageId`로 읽는다. 상태 파일을
임의로 수정하거나 새 digest를 검증 없이 성공으로 기록하지 않는다. 저장 방식을 별도로 변경해
오래된 기록과 현재 로컬 ID가 달라진 경우에는 fallback을 신뢰해서 성공으로 간주하지 말고
실행 이미지와 digest/config를 직접 대조한다.

롤백의 recovery.json.previousImageId는 원래부터 직전 실행 컨테이너 .Image의 **로컬 ID**다.
이 의미를 유지하므로 이전 로컬 이미지가 config ID이든 manifest ID이든 같은 방식으로 검증하고
Pull/Build 없이 복구한다. current.previous.json의 과거 상태와 버전 파일도 기존처럼 그대로 복원한다.

운영에서 기존 `current.json.imageId == container.Image` 비교를 사용했다면 아래처럼 변경한다.
이는 후속 수동 확인 예시이며 이번 작업에서 서버에 실행하지 않았다.

```bash
set -o pipefail
state=/opt/calfit/BE/.oracle-deploy/current.json
local_id=$(sudo jq -er '.localImageId // .imageId' "$state")
config_id=$(sudo jq -er '.configDigest // .imageId' "$state")
manifest_digest=$(sudo jq -er '.manifestDigest // .digest' "$state")
image="ghcr.io/coffiiness/calfit-backend@$manifest_digest"
sudo docker --host unix:///var/run/docker.sock manifest inspect "$image" | \
  jq -e --arg config "$config_id" '.config.digest==$config' >/dev/null || exit 1
test "$(sudo docker --host unix:///var/run/docker.sock image inspect "$image" --format '{{.Id}}')" = "$local_id" || exit 1
api_id=$(sudo docker --host unix:///var/run/docker.sock ps -q \
  --filter label=com.docker.compose.project=calfit-oracle --filter label=com.docker.compose.service=core-api)
test -n "$api_id" || exit 1
test "$(sudo docker --host unix:///var/run/docker.sock inspect "$api_id" --format '{{.Image}}')" = "$local_id" || exit 1
```

이 변경은 배포 스크립트·관련 Mock·문서에만 적용한다. Workflow의 payload 의미나 이미지 빌드는
변경하지 않는다. 실제 Oracle/운영 DB/Storage Driver에는 접근하거나 변경하지 않는다.
검증은 기존 Mock 28개와 이미지 ID 방식·manifest/config/로컬 ID 불일치 차단·관련 롤백 테스트로
한정한다. 제공받은 실제 Oracle manifest/config digest 쌍도 Mock fixture로 사용하며 실제 Pull은 하지 않는다.

로컬 결과: 기존 테스트 28개의 본문이 AST 기준 그대로 유지됐고, 호환성 테스트 7개를 더한
**35개가 모두 성공/실패 0개**로 통과했다(120.187초). Bash 구문과 git diff --check도 통과했다.
manifest 조회/config mismatch, 지원하지 않는 index, 무관한 로컬 ID 및 기존 RepoDigests/ARM64/
revision 불일치는 교체 전에 차단했다. containerd 방식의 성공 상태·pending, 로컬 ID 기반 롤백 및
중단 복구도 확인했다. 모든 GitHub/registry/Docker 명령은 Mock이며 실제 운영 적용 결과는 아니다.

참고: [Docker containerd image store](https://docs.docker.com/engine/storage/containerd/),
[Docker manifest inspect](https://docs.docker.com/reference/cli/docker/manifest/inspect/),
[OCI image manifest/config 설명](https://github.com/opencontainers/image-spec/blob/main/manifest.md).

## Cloudflare R2 첨부파일 저장소 전환

기존 AWS SDK for Java v2 **2.42.3**과 첨부파일 API를 유지한다. `S3_ENDPOINT`가 비어 있거나
공백이면 기존 AWS region/기본 자격증명 체인을 그대로 사용한다. 값이 있으면 **R2 모드**로
동작하며 `S3Client`와 `S3Presigner` 모두 같은 endpoint, `Region.of("auto")`, path-style
주소를 사용한다. 임의의 S3 호환 서비스에 대한 일반 설정으로 사용하지 않는다.

### 운영자가 직접 설정할 값

검토한 새 이미지를 적용할 때 운영 서버의 `/opt/calfit/BE/.env.prod`에서 아래 항목만 직접
설정한다. 이 문서의 예시는 실제 인증정보가 아니다. 기존 DB/JWT/메일 등 다른 설정과
`.env.image`, `calfit-oracle` Compose 프로젝트 및 `calfit_oracle_mysql84` 볼륨은 유지한다.
실제 환경 파일을 터미널 로그, CI, Git, Docker 이미지에 출력하거나 포함하지 않는다.

```dotenv
S3_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
S3_APPLICANT_BUCKET=calfit-applicants
AWS_ACCESS_KEY_ID=<R2_ACCESS_KEY_ID>
AWS_SECRET_ACCESS_KEY=<R2_SECRET_ACCESS_KEY>
AWS_SESSION_TOKEN=
AWS_REGION=ap-northeast-2
S3_KEY_PREFIX=uploads
PRESIGN_EXPIRE_MINUTES=10
```

endpoint는 Cloudflare가 표시하는 계정별 **S3 API endpoint**를 사용한다. 버킷명, 객체 경로,
`r2.dev` 또는 사용자 도메인을 붙이지 않는다. `AWS_REGION`은 AWS fallback용이며 R2 모드의
실제 서명 region은 항상 `auto`다. 발급한 버킷 전용 Object Read & Write **Access Key ID /
Secret Access Key**를 기존 AWS 환경변수 이름으로 전달한다. 대시보드의 Bearer API Token을
키 대신 넣지 않는다. R2 기본 자격증명에는 session token이 필요 없으므로 기존 AWS session
token을 남기지 않는다. Compose는 프로세스 환경변수가 env 파일보다 우선하므로 수동 실행 시
셸에 남은 `S3_ENDPOINT`/`S3_APPLICANT_BUCKET`/`AWS_*` 값도 확인하되 값 자체를 출력하지 않는다.

코드와 함께 변경된 `docker-compose.prod.yml`도 검토하여 반영해야 새 환경변수가 컨테이너에
전달된다. 기존 파일을 자동 덮어쓰는 절차는 제공하지 않는다. 실제 적용은 별도 운영 절차에서
백엔드만 교체하고 MySQL을 재시작하거나 스키마/볼륨을 변경하지 않는다. `.env.prod`는 기존
root 소유권과 제한된 읽기 권한을 유지한다. 잘못된 키는 Health Check만으로 감지되지 않는다.

### SDK checksum 및 브라우저 호환성

SDK 2.30.0 이후 자동 CRC32 동작과 R2의 checksum 지원 차이를 고려해 **R2 모드에서만**
S3Client의 request checksum 계산과 response checksum 검증을 `WHEN_REQUIRED`로 설정한다.
`chunkedEncodingEnabled(false)`도 적용한다. Presigner의 사용자 지정 S3Configuration은
`checksumValidationEnabled(false)`로 구성하여 브라우저가 추가 checksum 헤더를 보내야 하는
서명을 생성하지 않는다. AWS fallback 설정과 SDK 버전은 변경하지 않는다.

합성 키를 사용한 실제 SDK 서명 테스트로 PUT의 signed headers는 `host;content-type`, GET은
`host`만 포함하고 checksum 쿼리/헤더 및 session token이 추가되지 않는지 확인한다. GET의
다운로드 `Content-Disposition`과 기존 10분 만료(설정값으로 변경 가능)도 유지한다.

### 기존 FE와 R2 CORS

`CareersApplyView.vue`는 BE에 보낸 `file.type || 'application/octet-stream'`과 동일한 값을
R2 PUT의 `Content-Type`으로 보내며, PUT에 JWT/Authorization 또는 checksum 헤더를 붙이지
않는다. 따라서 현재 준비한 아래 CORS와 호환되며 **FE 기능 수정은 필요 없다**.

| R2 CORS 항목 | 값 |
| --- | --- |
| AllowedOrigins | `https://calfit.co.kr`, `https://www.calfit.co.kr` |
| AllowedMethods | `GET`, `PUT`, `HEAD` |
| AllowedHeaders | `Content-Type` |
| ExposeHeaders | `ETag` |

버킷 Public Access는 Disabled로 유지한다. 서명 URL의 `Content-Type`은 실제 PUT 값과 같아야
한다. 위 Origin에는 localhost/Pages preview가 포함되지 않으므로 해당 환경에서 실제 R2를
사용하려면 별도의 명시적 허용이 필요하다. 이번 변경은 버킷/CORS 설정을 수정하지 않는다.

### 유지되는 흐름과 운영 확인

1. `POST /api/v1/application-files/presign-upload`: 기존 지원자 소유권/tenant 확인 후 PENDING
   파일과 PUT URL 생성. 객체 키 형식 및 요청/응답 DTO를 유지한다.
2. 브라우저가 서명된 Content-Type으로 R2 PUT 수행.
3. `POST /api/v1/application-files/complete`: 같은 R2 endpoint로 HEAD하여 크기를 기록하고
   ACTIVE로 전환. 기존 지원자 접근 권한 검증을 유지한다.
4. `GET /api/v1/application-files/{fileId}/presign-download`: 기존 HR/tenant/ACTIVE 검사 후
   GET URL을 발급하고 브라우저가 원래 파일명으로 다운로드한다.

기존 FE의 파일 입력 `accept`와 `maxFileSizeMB`(미설정 시 10MB)를 그대로 유지한다. 현재 BE
서비스는 별도 MIME allowlist/최대 크기를 강제하지 않으며, 이 전환에서 새로운 제한을 추가하거나
기존 검사를 삭제하지 않는다. 서명 URL과 자격증명은 로그나 공유 문서에 남기지 않는다.

### 변경 범위 로컬 검증 결과

Temurin `17.0.21.1+2` / Gradle `8.14.3` / AWS SDK `2.42.3`에서 다음 검사를 통과했다.

| 검사 | 결과 |
| --- | --- |
| R2 PUT/GET 서명, AWS fallback, loopback HEAD, 자격증명 미조회 Bean 초기화 | 새 테스트 8개 통과 |
| 기존 첨부파일 업로드 권한/업로드 URL/완료/다운로드 API | 기존 테스트 10개 통과 |
| 합계 | 18개 성공, 실패/오류/비활성화 0개 |
| core-api Spotless / git diff --check | 통과 |
| Compose config (endpoint 빈 값 및 R2 값) | 전달 정상; endpoint 외 해석된 설정 동일 |

재실행 대상은 다음으로 한정한다. 이번 검증은 격리된 작업 디렉터리, local H2와 합성 AWS/JWT
환경변수 및 빈 AWS profile 파일을 사용했으며 실제 운영 환경 파일은 읽지 않았다.

```bash
./gradlew :core:core-api:test \
  --tests '*AwsS3ConfigTest' \
  --tests 'com.coffiness.calfit.api.application_files.*' \
  :core:core-api:spotlessCheck
```

S3Client에는 최신 checksum 옵션만 설정하고 Presigner에만 legacy `checksumValidationEnabled`
옵션을 사용한다. SDK 2.42.3에서 두 방식을 S3Client에 동시에 지정하면 Bean 초기화가 실패하므로
설정을 분리한 상태를 위 테스트로 검증했다. 전체 Gradle 테스트/Quality Gate와 ARM64 빌드는
반복하지 않았으며 CI/CD, Dockerfile, Baseline 및 FE에는 변경이 없다.

실제 키를 설정하고 배포한 뒤 위 네 단계를 운영 Origin의 브라우저에서 수행해 OPTIONS/PUT,
완료 API의 HEAD, HR 다운로드 및 만료 후 접근 거절을 확인해야 한다. 합성 키 로컬 검증은 실제
R2의 키 권한/CORS/객체 저장 성공을 증명하지 않는다. `/health`는 저장소 연결 검사가 아니며,
자격증명 없이도 Bean 생성과 기존 CI 기동은 가능하지만 실제 URL 발급에는 유효한 키가 필요하다.

기존 DB의 첨부파일 레코드는 객체 키를 그대로 사용한다. endpoint/버킷만 바꾸어도 기존 AWS
객체가 R2에 복사되지는 않는다. 같은 키의 객체가 R2에 없다면 기존 첨부파일 다운로드는 실패할
수 있으므로 별도 데이터 이전 여부를 결정해야 한다. 이번 작업은 기존 AWS 객체/DB를 변경하지
않으며 데이터 이전은 수행하지 않는다.

공식 참고:
[Cloudflare Java SDK 예제](https://developers.cloudflare.com/r2/examples/aws/aws-sdk-java/),
[R2 Presigned URLs](https://developers.cloudflare.com/r2/api/s3/presigned-urls/),
[R2 CORS](https://developers.cloudflare.com/r2/buckets/cors/),
[R2 S3 checksum 지원 표](https://developers.cloudflare.com/r2/api/s3/api/),
[AWS Java SDK checksum 기본 동작](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/s3-checksums.html),
[SDK 2.42.3 Presigner 구현](https://github.com/aws/aws-sdk-java-v2/blob/2.42.3/services/s3/src/main/java/software/amazon/awssdk/services/s3/internal/signing/DefaultS3Presigner.java).
