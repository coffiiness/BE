# 없는 경로의 404 처리 및 Oracle 반영

## 변경 범위

`NoHandlerFoundException`과 `NoResourceFoundException`만 기존 NOT_FOUND 응답
(`HTTP 404`, `error.code=E404`)으로 처리한다. Advice 로그는 DEBUG 한 줄이며 예외 객체를
전달하지 않아 스택 추적을 남기지 않는다. Spring 자체의 PageNotFound WARN 한 줄은 남을 수 있다.
예상하지 못한 Exception과 일반 IllegalStateException은 기존 500/E500 및 ERROR 스택 로그를 유지한다.

SSE Accept 요청은 기존 공통 응답 함수에 따라 오류 상태만 반환하고 JSON 본문은 넣지 않는다.
SecurityConfig/JWT/권한/CORS/업무 로직은 변경하지 않는다. 따라서 인증되지 않은 보호 경로는
존재 여부와 관계없이 먼저 401이다. 404 확인은 공개 경로 또는 유효한 JWT를 사용한다.

prod에서 `spring.h2.console.enabled=false`를 명시한다. local의 H2 Console=true와 H2 DB는 유지한다.
Oracle Compose 관리 포트는 8081, 관리 주소는 컨테이너 loopback 127.0.0.1이며 호스트 포트로
발행하지 않는다. 같은 Docker 네트워크에서도 컨테이너 IP의 8081로 접속할 수 없게 한다.
Tunnel origin은 계속 http://127.0.0.1:8080이다.

근거: [Spring NoHandlerFoundException](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/servlet/NoHandlerFoundException.html),
[NoResourceFoundException](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/servlet/resource/NoResourceFoundException.html),
[Spring Boot 관리 포트·주소](https://docs.spring.io/spring-boot/3.5/reference/actuator/monitoring.html).

## 로컬 검증

실제 Oracle 서버에 접속하거나 배포하지 않았다. Windows Java 17에서 선택한 테스트 41개가
실패/오류/skip 없이 통과했다. 전체 업무 테스트 스위트를 실행한 결과는 아니다.

| 범위 | 결과 |
| --- | --- |
| 새 MVC 예외 처리 테스트 | 9개: 두 종류의 실제 MVC 404, JSON E404, DEBUG/스택 없음, 실제 500/스택 유지, SSE 오류 본문 유지 |
| 실제 애플리케이션 보안 회귀 | 9개: JWT 미인증/오류 401, 사용자/관리자 403/200, CORS 200/403, SSE 연결·heartbeat, local H2 Console |
| 기존 로그인 / 사용자 조회 | 6개 / 4개 통과 |
| 기존 관리자 대시보드 / 알림 목록 | 8개 / 5개 통과 |

테스트용 MVC 구성/컨트롤러는 TestConfiguration/TestComponent로 표시해 실제 애플리케이션의
component scan에 들어가지 않게 했다. 실패를 던지는 경로는 테스트에만 존재한다.

다시 실행할 때 BE에서 아래처럼 실행한다. AWS 값은 외부 접근 없이 컨텍스트를 구성하는 합성 값이다.

```powershell
$env:APP_EMAIL_ENABLED='false'
$env:SPRING_PROFILES_ACTIVE='local'
$env:AWS_ACCESS_KEY_ID='validation-only'
$env:AWS_SECRET_ACCESS_KEY='validation-only'
$env:AWS_EC2_METADATA_DISABLED='true'
.\gradlew.bat --no-daemon -g .gradle-user-codex :core:core-api:test `
  --tests com.coffiness.calfit.core.api.controller.ApiControllerAdviceTest `
  --tests com.coffiness.calfit.api.regression.EndpointSecurityRegressionTest `
  --tests com.coffiness.calfit.api.users.login.POST_specs `
  --tests com.coffiness.calfit.api.users.me.GET_specs `
  --tests com.coffiness.calfit.api.admin.dashboard.summary.GET_specs `
  --tests com.coffiness.calfit.api.notification.list.GET_specs
```

별도 로컬 이미지 `calfit-backend:oracle-endpoint404-check`를 linux/arm64로 빌드해 Windows
Docker Desktop의 QEMU에서 prod + MySQL 8.4.13 + validate로 기동했다. Oracle 네이티브
하드웨어/공개 Tunnel에 대한 측정은 아니다. Java 17.0.21 런타임을 사용했다.

| 실제 로컬 prod HTTP/격리 검사 | 결과 |
| --- | --- |
| GET /health | 200 |
| GET /h2-console/ | 404 / E404, prod H2 Console 비활성화 |
| GET /actuator/health (API 8080) | 404 / E404 |
| GET /docs/not-a-real-endpoint | 공개 경로에서 404 / E404 |
| GET /api/v1/not-a-real-endpoint | 미인증 401, 정상 JWT 404 / E404 |
| 보호 API 미인증 / 잘못된 JWT | 401 / 401 |
| 허용 / 미허용 CORS preflight | 200(정확한 Allow-Origin) / 403 |
| 정상 회원가입·로그인 후 /users/me | SUCCESS 및 GET 200 |
| 일반 사용자 → 관리자 API | 403 (관리자 200은 위 회귀 테스트에서 검증) |
| 실제 SSE 스트림 | connected + heartbeat 수신, 클라이언트 제한 시간까지 연결 유지 |
| 컨테이너 loopback 8081 health | UP |
| 컨테이너 IP로 8081 접속 | connection refused, 호스트 8081 매핑도 없음 |
| 없는 경로 요청 이후 로그 | NoHandlerFound/NoResourceFound 예외 스택·관련 ERROR 없음 |

검증에는 기존 로컬 QA 전용 볼륨 `calfit_qa_fresh_mysql8413_20261126_74de8ab6`과 합성
환경변수를 사용했다. 이 볼륨을 초기화하지 않고 validate로 재사용했다. 검증 종료 후 QA
컨테이너/네트워크만 종료하고 이 볼륨과 이전 QA 볼륨을 보존했다. 실제 운영 볼륨/환경변수,
AWS 자원, GitHub는 변경하지 않았다. S3/메일/Google/결제 외부 연동은 이번 검증 범위가 아니다.

로컬 검증 보조 파일/결과는 Git에서 제외된 `.gradle-user-codex/oracle-validation`에 있으며,
수정 운영 파일 3개만 담은 tar.gz와 SHA-256도 생성해 목록·무결성을 확인했다. 원격 전송은 하지 않았다.

## Windows에서 수정된 운영 파일만 패키징

이하 명령은 **추후 사용자가 반영할 때 실행하는 예시**다. 이번 작업에서 SSH/SCP/원격 실행은
하지 않는다. 초기 전체 소스 전송 절차와 다르게 기존 `.env.prod`를 새로 복사하지 않는다.

서버에 같은 버전의 소스가 이미 있으며 이 작업의 변경만 추가한다는 전제다. 먼저 아래
세 파일의 diff를 검토한다. 그 외 서버 변경이 있으면 덮어쓰기 전에 비교·병합해야 한다.

```powershell
Set-Location 'C:\Users\guddn\dev\swcamp-Sideproject\05_final_project\BE'
$files = @(
  'core/core-api/src/main/java/com/coffiness/calfit/core/api/controller/ApiControllerAdvice.java',
  'core/core-api/src/main/resources/application.yml',
  'docker-compose.prod.yml'
)
git diff -- $files
$patchName = 'calfit-endpoint-fix-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.tar.gz'
$patchDir = Join-Path (Get-Location) '.gradle-user-codex/oracle-transfer'
New-Item -ItemType Directory -Path $patchDir -Force | Out-Null
$patch = Join-Path $patchDir $patchName
tar -czf $patch -- $files
if ($LASTEXITCODE -ne 0) { throw 'Archive failed' }
tar -tzf $patch
$hash = (Get-FileHash -LiteralPath $patch -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText("$patch.sha256", "$hash  $patchName`n", [Text.Encoding]::ASCII)

$OracleHost = 'YOUR_ORACLE_PUBLIC_IP'
$OracleKey = "$env:USERPROFILE\.ssh\calfit-oracle.key"
# 등록된 호스트키 지문을 신뢰하는 콘솔/기존 세션의 지문과 비교한 상태에서 실행한다.
ssh -i $OracleKey -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes "ubuntu@$OracleHost" `
  'install -d -m 700 /home/ubuntu/calfit-upload'
scp -i $OracleKey -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes `
  $patch "$patch.sha256" "ubuntu@${OracleHost}:/home/ubuntu/calfit-upload/"
```

압축본에는 위 운영 파일 세 개만 들어간다. 실제 환경변수, 개인키, DB 파일, 테스트와 문서는
포함하지 않는다. SSH는 현재 NSG에서 허용한 관리자 IP로만 접속한다.

## Oracle에서 사전 확인·백업·파일 적용

실제 운영 **BE 경로**와 전달한 파일명으로 바꿔 실행한다. 현재 실행 중인 Compose의 `-p` 또는
COMPOSE_PROJECT_NAME을 따로 사용했다면 `dc`에도 동일하게 추가한다. 실행 중인 API를 못 찾으면
진행하지 않는다. 새 프로젝트 이름으로 별도 서비스를 띄우지 않는다.

```bash
set -euo pipefail
cd /home/ubuntu/REPLACE_WITH_EXISTING_BACKEND_PATH
envfile="$PWD/.env.prod"
compose="$PWD/docker-compose.prod.yml"
dc() { docker compose --env-file "$envfile" -f "$compose" "$@"; }

api_id="$(dc ps -q core-api)"
mysql_id="$(dc ps -q mysql)"
test -n "$api_id" && test -n "$mysql_id"
env_hash="$(sha256sum "$envfile" | cut -d ' ' -f 1)"
mysql_volume="$(docker inspect --format '{{range .Mounts}}{{if eq .Destination "/var/lib/mysql"}}{{.Name}}{{end}}{{end}}' "$mysql_id")"
test -n "$mysql_volume"
stamp="$(date +%Y%m%d-%H%M%S)"
backup="/home/ubuntu/calfit-patch-backups/$stamp"
install -d -m 700 "$backup"
files=(
  core/core-api/src/main/java/com/coffiness/calfit/core/api/controller/ApiControllerAdvice.java
  core/core-api/src/main/resources/application.yml
  docker-compose.prod.yml
)
tar -czf "$backup/previous-files.tar.gz" -- "${files[@]}"
rollback_image="calfit-backend:before-endpoint404-$stamp"
docker image tag "$(docker inspect --format '{{.Image}}' "$api_id")" "$rollback_image"

archive='/home/ubuntu/calfit-upload/REPLACE_WITH_PATCH_FILENAME.tar.gz'
(cd /home/ubuntu/calfit-upload && sha256sum -c "$(basename "$archive").sha256")
tar -tzf "$archive"
# 위에서 만든 세 파일만 있는지 확인한다. .env.prod/DB/개인키 경로가 있으면 중단한다.
stage="$backup/incoming"
install -d -m 700 "$stage"
tar --no-same-owner --no-same-permissions -xzf "$archive" -C "$stage"
for file in "${files[@]}"; do
  test -f "$stage/$file" && test ! -L "$stage/$file"
  test -f "$file" && test ! -L "$file"
done
for file in "${files[@]}"; do
  install -m 644 "$stage/$file" "$file"
done

test "$env_hash" = "$(sha256sum "$envfile" | cut -d ' ' -f 1)"
dc config --quiet
# 비밀이 들어 있는 전체 config를 출력하지 않고 validate/기존 volume만 검사한다.
dc config --format json | python3 -c '
import json,sys
c=json.load(sys.stdin)
assert c["services"]["core-api"]["environment"]["SPRING_JPA_HIBERNATE_DDLAUTO"] == "validate"
assert c["services"]["mysql"]["image"] == "mysql:8.4.13"
assert c["volumes"]["mysql_data"]["name"] == sys.argv[1]
print("validate / mysql:8.4.13 / existing volume verified")
' "$mysql_volume"
```

`.env.prod` 복사/수정, 초기화용 update, volume create, down, down -v, prune은 하지 않는다.
보관한 previous-files에는 소스 세 개만 있으며 `.env.prod`의 값은 출력하지 않는다.

## 기존 MySQL을 유지하며 백엔드만 재빌드·재생성

```bash
dc build core-api
dc up -d --no-deps --no-build --wait --wait-timeout 600 core-api
dc ps
test "$mysql_id" = "$(dc ps -q mysql)"
test "$env_hash" = "$(sha256sum "$envfile" | cut -d ' ' -f 1)"
dc exec -T core-api curl -fsS http://127.0.0.1:8081/actuator/health
# UP
curl -i http://127.0.0.1:8080/health
curl -i http://127.0.0.1:8080/h2-console/
curl -i http://127.0.0.1:8080/actuator/health
curl -i http://127.0.0.1:8080/docs/not-a-real-endpoint
# 각각 200 / 404(E404) / 404(E404) / 404(E404)
curl -i http://127.0.0.1:8080/api/v1/not-a-real-endpoint
# 보호 경로의 미인증 요청은 기존대로 401

dc port core-api 8081
# 발행한 포트 없음. host의 ss -ltn에서도 이 서비스의 8081 리스너가 없어야 한다.
```

API_BIND_PORT를 8080 외 값으로 설정했다면 위 호스트 curl 포트만 실제 값으로 바꾼다.
관리 health는 `docker compose exec`로 컨테이너 내부에서 확인한다. Tunnel 공개 API 주소에서도
동일한 응답을 검사한다. 404는 ERROR 스택 없이 처리되며 실제 500 오류 로그를 없애는 설정은 하지 않는다.

실패 시 DB/환경변수에 손대지 않고 소스 세 개와 이전 이미지만 복구한다. 위와 같은 셸의 변수를 사용한다.

```bash
tar --no-same-owner --no-same-permissions -xzf "$backup/previous-files.tar.gz" -C "$PWD"
BACKEND_IMAGE="$rollback_image" docker compose --env-file "$envfile" -f "$compose" \
  up -d --no-deps --no-build --wait --wait-timeout 600 core-api
```

Java 17, MySQL 8.4.13, ARM64, JPA validate, 기존 AWS S3 설정은 유지한다. GitHub Push와
AWS 자원 변경은 이 절차에 포함하지 않는다.
