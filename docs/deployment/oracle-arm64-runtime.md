# Oracle ARM64 런타임 이미지 검증

2026-11-27 KST, integration/oracle-cicd worktree에서 같은 Temurin **17.0.21.1+2**로
linux/arm64 이미지를 빌드하고 격리 QA 실행 검증을 완료했다.
Java 패치 다운그레이드나 floating tag로 대체하지 않았다.
Gradle 품질 파일과 SpotBugs Baseline은 변경하지 않았다.

선행 Gate 재실행도 통과했다. 기존 결과인 JUnit 성공 585/기존 비활성화 12/실패 0,
Line 75.48%, Branch 47.84%, SpotBugs 신규/High/악화 0을 유지한다.
변경되지 않은 테스트 실행 결과는 Gradle up-to-date로 재사용했고, 검증/보고서 Task는 다시 확인했다.

## 공식 배포물 확인

공식 Docker Hub Registry API에서 다음 patch 태그에는 linux/arm64 manifest가 없었다.

| 배포물 | 확인 결과 |
| --- | --- |
| 17.0.21.1_2-jdk-jammy | amd64 등만 제공, arm64 없음 |
| 17.0.21.1_2-jre-jammy | amd64 등만 제공, arm64 없음 |
| 공식 Linux aarch64 JDK tar.gz | 제공됨, 192348076 bytes |
| 공식 Linux aarch64 JRE tar.gz | 제공됨, 46121315 bytes |

반면 [공식 Temurin 릴리스](https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.21.1%2B2)와
[공식 Jammy Dockerfile](https://github.com/adoptium/containers/blob/main/17/jre/ubuntu/jammy/Dockerfile)에는
같은 패치의 aarch64 아카이브와 checksum이 제공된다. 이 바이너리를 사용했다.

| Linux 바이너리 | 공식 SHA-256 |
| --- | --- |
| JDK x64 | 0b0ac5eab6db6abaec68c9913bcddb31321e5da411c45073d520d9c125e6f7b4 |
| JDK aarch64 | 1d808f4b558e9de63d8ba3ead21ffc8b9de204894eff3c19d52c1480aa17cc58 |
| JRE x64 | 5a092a0bc21de4c3d855480edd7945be9cfe157b38cdb84dac280511893bb009 |
| JRE aarch64 | ed968eb2cde06ba38c3d957956cab7faced9181aaeebed4904f89e2e6efa8d11 |

이번 런타임 빌드는 aarch64 JRE를 실제 다운로드하고 파일 전체 SHA-256을 검사했다.
JDK의 제공 여부와 공식 checksum도 확인하여 소스 빌드 bootstrap에 고정했다.
이번에 실제 이미지 빌드·QA 실행한 경로는 검증 JAR를 재사용하는 `runtime-from-jar`다.

## Jammy 구성과 에뮬레이터 대응

최초 Ubuntu ARM64 위에서 apt를 실행하는 빌드는 Docker Desktop 에뮬레이터의
libc-bin postinstall/ldconfig SIGSEGV로 실패했다. 해당 실패를 무시하지 않았다.

수정한 구성은 공식 Jammy OS 파일을 아래 immutable manifest로 공급받는다.

- OS 공급: `eclipse-temurin@sha256:c6a5dd70fb24f4e07ef88cbda8efb613a5a649a1af071a41de8eaabd1ee6b5da`
- 해당 index의 ARM64 OS 이미지: `sha256:8383c5ec76a4268796d66fed2fbc216b2051e6a95620eb07cd0a54c315c463f5`
- 네이티브 준비/컴파일 단계 Ubuntu 22.04 index:
  `sha256:b000e69a50ff940390233025e7a337fb8469b509e2334fa4d018ac30f3e18cdb`

OS 공급 이미지의 기존 Java는 준비 파일시스템에서 디렉터리 전체를 제거한다.
공식 checksum을 통과한 새 JRE만 설치한 뒤 최종 scratch stage로 현재 파일시스템을 복사한다.
이 과정은 에뮬레이터에서 ARM64 패키지 postinstall을 실행하지 않으며, 기존 Java의 layer도
최종 이미지에 상속하지 않는다. scratch는 빈 기반 stage이며 최종 OS 파일은 Ubuntu Jammy다.

최종 target stage에서는 실제 ARM64 Java를 실행하여 vendor/runtime version/os.arch를 검사한다.
기존 Java가 있는 OS 공급 이미지를 다운로드했다는 이유로 이전 JVM을 실행하거나 그대로 출하하지 않는다.
최종 컨테이너에서도 /opt 아래 Java release 파일이 새 설치 한 개뿐임을 확인했다.
fontconfig, curl, CA, locale 등은 공식 Jammy OS 파일을 유지한다.

## Dockerfile 단계와 Quality Gate

| 단계 | 역할 |
| --- | --- |
| build | 빌더 CPU용 공식 JDK 설치 및 버전 확인, 기존 qualityCheck + bootJar |
| runtime-os | digest로 고정한 공식 Jammy OS 파일 공급 |
| prepare-runtime | 빌더 네이티브 환경에서 target JRE 다운로드/SHA 검사, 기존 Java 제거 |
| runtime-base | 최종 ARM64 파일시스템과 Java 실행 버전/아키텍처 검사 |
| runtime-from-jar | 이미 검증한 app.jar 재사용, 필수 APP_JAR_SHA256 검사 |
| runtime | 기본 target. build 단계에서 Gate를 통과한 bootJar만 복사 |

기본 소스 빌드에는 테스트 제외 옵션이 없고, 기존 Gradle bootJar 우회 방지 정책도 유지한다.
로컬 `build-arm64-runtime.ps1`은 매번 기존 Gate를 먼저 실행하고 성공한 JAR만 staging한다.
SkipGate, ignoreFailures 또는 continue-on-error 옵션은 추가하지 않았다.

runtime-from-jar는 신뢰된 artifact 경계다. JAR hash는 바이트 무결성 검사이며, 임의의 외부 JAR가
품질 검사를 통과했다는 서명을 대신하지 않는다. 향후 CI는 quality Job 성공과 artifact provenance를
강제해야 한다. 로컬 verified-artifact.json도 서명된 CI attestation으로 설명하지 않는다.

## 실제 이미지와 실행 결과

| 항목 | 결과 |
| --- | --- |
| 로컬 이미지 | calfit-backend:arm64-temurin-17.0.21.1-2 |
| 플랫폼 | linux/arm64 |
| 실제 JVM os.arch | aarch64 |
| Java vendor | Eclipse Adoptium |
| java.runtime.version | 17.0.21.1+2 |
| OS | Ubuntu 22.04.5 LTS |
| Docker CLI 표시 크기 | 590MB |
| 이미지 콘텐츠 크기, Docker API | 198581900 bytes |
| 미압축 layer 합계, docker history | 391692288 bytes |
| 실제 실행 환경 | Windows Docker Desktop linux/amd64 호스트의 ARM64 에뮬레이션 |
| 발행/Push | 실행하지 않음 |

Docker Desktop의 CLI 표시에는 압축 콘텐츠와 unpacked layer 저장량이 함께 반영되므로 크기를 구분했다.

Manifest digest:

```text
sha256:b08b89632f15c0ca4486674e52bcf4fb7ffb05ca2416e27a4bd33a905ae8d286
```

Config digest:

```text
sha256:14d02eab4997cd6e83195e282bd86cb30d9678425e26e3407cf0b13d50995d60
```

JAR SHA-256:

```text
360a0b1ef3ea118df6d49e19aba7ca3126e72e64a04a50a3228187dbeaa9efa9
```

동일한 JAR hash를 host bootJar, build staging, 이미지 label, 실행 컨테이너 /app/app.jar에서
확인했다. 잘못된 JAR hash를 전달한 별도 negative build는 실제 checksum 오류로 실패했다.
이 digest는 로컬 빌드 결과이며 GHCR에 게시된 repository digest를 의미하지 않는다.
Oracle 운영 서버에 접속하거나 네이티브 성능을 측정한 결과도 아니다.

## 격리 QA와 데이터 보존

기존 `calfit_integration_mysql84_20261127_bbd57d2a` 볼륨을 읽기 전용으로 복제하여
새 `calfit_arm64qa_20261127_ddfad41c` 볼륨에서 실행했다.
원본 QA 볼륨의 전체 파일 checksum 목록은 검증 전후 동일했다.

기존 QA에는 사용자 행은 없었지만 총 3개의 데이터 행이 있었다.
그 3개 INSERT 행이 최종 복제본에서도 모두 동일함을 확인했다.
HTTP 검증 재실행 과정에서 복제본에만 QA 회원 2명과 관련 데이터가 추가되어 최종 7행이 됐다.
모든 HTTP/SSE 회귀 검사 전후 7행의 전체 dump hash와 schema dump hash가 동일했다.

JPA는 validate를 유지했다. 기존 prod는 ShedLock의 CREATE TABLE IF NOT EXISTS 초기화도 실행하므로,
기동 직전부터 DB 전체를 read-only로 설정하면 해당 SQL이 거절됐다.
prod 설정을 바꾸지 않고 복제 DB에만 기동/테스트 회원 생성 동안 쓰기를 허용했다.
그 뒤 super_read_only를 다시 켠 상태로 읽기 회귀와 전체 데이터 보존 검사를 수행했다.

신규 회원의 AUTO_INCREMENT 다음 번호 증가는 데이터 삽입의 결과다.
기존/최종 schema 형태 비교에서는 그 다음 번호 옵션만 제거하고 비교했다.
컬럼, 타입, null/default, index 및 constraint 정의는 그대로 비교하며, raw dump도 보관했다.
회귀 검사 전후의 fixture schema dump는 다음 번호를 포함한 파일 hash 자체가 같았다.

| 검사 | 결과 |
| --- | --- |
| MySQL | 8.4.13, ARM64 |
| Spring profile / JPA | prod / validate |
| 컨테이너 health | healthy |
| GET /health | 200 |
| GET /h2-console/, /h2-console/login.do | 404, E404 |
| API 포트 /actuator/health | 404, E404 |
| 미인증·잘못된 JWT | 401 |
| 실제 QA signup/login 및 정상 JWT /users/me | 200 |
| USER의 관리자 API | 403 |
| 허용/차단 CORS preflight | 200 / 403 |
| 인증된 없는 API | 404, E404 |
| 내부 8081 Actuator | UP, loopback에서만 접근 |
| 컨테이너 IP의 8081 및 호스트 8081 | 거절 / 미게시 |
| SSE | connected와 heartbeat, 연결 유지 확인 |
| HTTP 검사 | 16개 통과 |
| 없는 endpoint ERROR/stack trace | 검사 통과 |
| 원본 QA / 기존 행 / fixture 데이터 | 모두 보존 확인 |

두 Docker network는 internal이며 외부 기본 경로가 없다. 실제 AWS/S3/Google/Toss/SMTP 호출은
수행하지 않았고 QA 전용 임시 설정만 사용했다. 검사 후 새 QA 컨테이너를 중지했고,
원본·복제 QA 볼륨과 기존 Docker 데이터는 삭제하지 않았다.
초기 검증의 길이 제한 초과 QA 비밀번호와 AUTO_INCREMENT 비교 오탐은 검증 스크립트에서
수정해 재실행했으며, 애플리케이션 비즈니스 코드나 테스트를 고친 것이 아니다.

## 향후 CI에서 JAR 재사용

1. Java 17.0.21.1+2/Gradle 8.14.3의 quality Job에서 전체 Gate와 bootJar를 실행한다.
2. JAR, SHA-256, source 식별 정보, Baseline hash 및 전체 품질 보고서를 하나의 검증 artifact로 전달한다.
3. quality 성공에 의존하는 image Job에서 같은 JAR를 runtime-from-jar target에 전달한다.
   target에 맞는 JRE와 JAR hash를 검사하고, JAR를 재컴파일하지 않는다.
4. image QA Job에서 prod/validate/Health 및 보안·DB 보존 검사를 수행한다.
5. quality/image/QA 모두 성공한 신뢰된 main/release 실행만 GHCR 발행을 허용한다.
   후속 CD도 검증된 image digest만 pull하도록 연결한다.

이번 단계에서 신규 Workflow, GHCR 발행, 서버 배포, Commit/Push/원격 Merge는 실행하지 않았다.

## 실행 명령

PowerShell 7에서 JAVA_HOME을 검증한 Temurin JDK로 지정한 뒤 실행한다.

```powershell
.\tools\deployment\build-arm64-runtime.ps1
```

현재 보존된 QA 복제본을 재검증할 때는 운영 env 대신 QA state를 사용한다.
이 검증 명령은 QA 이름/volume mount를 확인하며 테스트 회원을 복제본에만 추가한다.

```powershell
$s = Get-Content .gradle-user-codex/arm64-runtime-validation/qa-state.json -Raw | ConvertFrom-Json
docker compose --project-name $s.project --env-file $s.envFile -f docker-compose.prod.yml -f $s.override up -d --no-build --pull never --wait --wait-timeout 360
.\tools\deployment\verify-arm64-runtime.ps1
docker compose --project-name $s.project --env-file $s.envFile -f docker-compose.prod.yml -f $s.override stop
```

새 복제본을 준비한 실행 기록은 무시되는 arm64-runtime-validation/setup-qa.ps1에 보관했다.
운영 환경변수/Secret은 읽거나 출력하지 않았고, QA env·SQL dump·토큰을 소스 전송 패키지에 포함하지 않는다.

## 변경 파일과 검증 범위

- Dockerfile: CPU별 공식 바이너리 bootstrap, Jammy runtime rootfs, Gate 소스 빌드와 JAR 재사용 분리.
- docker/install-temurin17.sh: 고정 release URL/4개 SHA, checksum 검사 및 추출.
- docker/verify-temurin17.sh: 실제 vendor/version/architecture 확인.
- tools/deployment/build-arm64-runtime.ps1: 선행 Gate, 검증 artifact staging, ARM64 빌드/Java 검사 및 메타데이터.
- tools/deployment/verify-arm64-runtime.ps1: 격리 QA HTTP/SSE·관리 포트·schema/data 보존 검사.
- tools/deployment/package-oracle.ps1: 두 bootstrap 파일을 필수 전송 파일에 포함.
- docs/deployment/oracle-arm64-runtime.md: 본 결과와 CI 설계.
- docs/quality/gradle-quality.md 및 quality-gate-results.md: 이전 ARM64 발행 보류와 이번 해소 결과 연결.

실제로 빌드한 이미지는 runtime-from-jar ARM64 target이다.
새 JDK bootstrap을 포함한 기본 Docker source-build target 전체를 별도로 실행한 결과는 아니다.
기존 Gradle Gate/bootJar 정책은 유지하고 현 단계에서도 JAR staging 전에 Gate를 다시 통과시켰다.
품질 보호 파일 11개와 Baseline의 전후 SHA-256이 일치하며,
기존 Java 소스·테스트·DB 설정·main/dev·FE는 변경하지 않았다.

## 15-8 GitHub Actions 연결

`.github/workflows/ci.yml`을 기존 파일 경로에서 교체했다. GitHub의 Workflow 상태는
`disabled_manually`로 확인했으며 활성화하지 않았다. 이 절은 Workflow 구현 및 정적 검증
결과이며, 실제 hosted runner 실행이나 GHCR 발행 성공을 뜻하지 않는다.

| 이벤트 | AMD64 Quality Gate / bootJar | ARM64 빌드 / MySQL QA | GHCR 발행 |
| --- | --- | --- | --- |
| dev/main 대상 PR | 실행 | 실행 안 함 | 실행 안 함 |
| dev Push | 실행 | Gate 성공 후 실행 | 실행 안 함 |
| main Push | 실행 | Gate 성공 후 실행 | 모든 선행 Job 성공 후 실행 |
| integration/oracle-cicd Push | 트리거 없음 | 트리거 없음 | 트리거 없음 |

`quality`는 ubuntu-24.04에서 기존 SHA 고정 JDK 설치·검증 스크립트를 사용한다.
Temurin 17.0.21.1+2 / Gradle 8.14.3 및 기존 `check qualityCheck :core:core-api:bootJar`
의존성을 유지한다. 설치한 JDK만 toolchain 탐색 대상으로 지정한다. 테스트 제외 옵션이나
Gate 변경은 없다. 성공한 bootJar만 `app.jar`, SHA-256, source SHA 메타데이터로 전달한다.
테스트 XML/HTML, 전체 모듈 JaCoCo, 미필터링 SpotBugs와 Gate 보고서는 실패 시에도
14일 보관하고, 검증 JAR는 3일 보관한다.

`arm64`는 ubuntu-24.04-arm에서 같은 run의 JAR를 내려받아 checksum 및 source SHA를
검사한다. 임시 최소 context에는 Dockerfile, JAR 및 기존 Java 설치·검증 스크립트만
포함한다. `runtime-from-jar` target으로 한 번만 빌드하며 Docker 내부에서 Gradle을
실행하지 않는다. native ARM64 runner를 사용하며 QEMU를 설치하지 않는다.

`tools/ci/verify-runtime.sh`는 GitHub Actions Linux/ARM64 환경에서만 실행 가능하다.
독립 internal Docker network 및 MySQL 8.4.13의 tmpfs 저장소를 만들고, 운영 env나
기존 QA/운영 볼륨을 읽거나 연결하지 않는다. 무작위 CI 전용 DB/JWT 값을 만들고 로그에
마스킹하며 env 파일은 Artifact에 포함하지 않는다. 외부 서비스에는 CI dummy 값만
전달하고 컨테이너의 외부 통신 및 모든 호스트 포트 게시를 차단한다.

신규 임시 DB에 한해 첫 컨테이너를 `prod/update`로 기동해 스키마를 초기화한 뒤 정지한다.
다른 컨테이너를 같은 이미지의 `prod/validate`로 기동하여 다음을 검사한다.

- Java vendor/runtime/architecture 및 이미지의 JAR hash·commit label
- 실제 MySQL 버전, DB 준비 상태, 초기화된 테이블 존재
- 내부 8081 Actuator `UP`, 8080 `/health` 200
- 8080 `/actuator/health`, `/h2-console/`, 공개 경로의 없는 URL 404
- validate 전후 schema checksum 일치, 호스트 포트 게시 없음, DB tmpfs 사용

컨테이너·네트워크·env 파일은 성공/실패 시 이번 실행이 만든 것만 정리한다.
Docker prune, 기존 볼륨 삭제, 운영 Compose 실행은 하지 않는다. QA 결과와 Java 버전은
14일 보관한다. 실패 시 컨테이너 로그 일부를 Actions 로그에 출력한다.

`publish`만 `packages: write`를 가지며 일반 Job은 `contents: read`다. main Push 및
`coffiiness/BE`에서 두 선행 Job이 모두 성공한 경우만 실행한다. 발행 Job에는 checkout,
Gradle 실행, Docker 빌드가 없다. 검증한 이미지를 `docker save`로 전달하고(1일 보관),
archive checksum·source SHA·JAR hash·image ID·ARM64 label을 재검사한 뒤 load/tag/push한다.
`GITHUB_TOKEN`으로만 인증하며 별도 PAT/운영 Secret은 사용하지 않는다.

발행 경로는 `ghcr.io/coffiiness/calfit-backend:sha-<전체 커밋 SHA>`다. push가 반환한
registry manifest digest로 원격 manifest를 읽고 config digest가 검증 이미지 ID와
같은지 확인한다. 실제 발행 digest는 Step Summary 및 `published-image.json` Artifact에
30일 보관한다. image config ID와 registry manifest digest는 서로 다른 값이다.
브랜치별 Workflow 실행을 직렬화하고 진행 중 발행을 자동 취소하지 않는다.

로컬에서는 actionlint 1.7.12로 문법·expression context·Job 의존성을 검사했고,
Workflow의 실제 조건식에 대해 PR/dev/main 및 실패·취소·skip·fork 등 66개 조합을
검증했다. Bash 구문 및 CI 외 실행 거부도 확인했다. actionlint는 무시되는 검증 폴더에서만
사용했으며 프로젝트 품질 도구나 CI 필수 검사로 추가하지 않았다.
추가로 Workflow의 inline Bash 7개를 구문 검사했고, 변조한 이미지 archive를 주입하면
실제 발행 준비 명령이 Docker load/push 전에 실패하는 것도 확인했다.

후속 Push 및 별도 활성화 승인 후 실제 runner에서 확인할 항목:

1. SHA 고정 JDK 다운로드, 모든 toolchain 및 기존 Gate/bootJar 성공 여부
2. ARM runner 가용성, native runtime target 빌드, MySQL tmpfs 초기화 및 validate 성공
3. dev/PR에서 발행 Job이 skip되고 main 실패 시 발행이 차단되는지 확인
4. main 성공 시 archive load 전후 image ID, GHCR 실제 digest/config ID 일치 확인
5. 최초 package의 Repository 연결·GITHUB_TOKEN 권한·가시성 및 Artifact 보관 결과 확인

이번 단계에서 전체 Gradle 테스트, Linux/Windows 전체 검증, 로컬 ARM 이미지 재빌드를
반복하지 않았다. Commit/Push/Merge/실제 발행/Oracle 자동 배포·systemd 변경은 하지 않았다.

참고: [GitHub hosted runners](https://docs.github.com/en/actions/reference/runners/github-hosted-runners),
[GHCR의 GITHUB_TOKEN 인증](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry).

