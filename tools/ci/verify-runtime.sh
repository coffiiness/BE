#!/usr/bin/env bash
# GitHub-hosted ARM64 CI only. Never reads .env.prod or attaches a Docker volume.
set -euo pipefail
[[ ${GITHUB_ACTIONS:-} == true && ${RUNNER_OS:-} == Linux && ${RUNNER_ARCH:-} == ARM64 ]]
[[ ${GITHUB_RUN_ID:-} =~ ^[0-9]+$ && ${GITHUB_RUN_ATTEMPT:-} =~ ^[0-9]+$ ]]
[[ ${GITHUB_SHA:-} =~ ^[0-9a-f]{40}$ && ${EXPECTED_JAR_SHA256:-} =~ ^[0-9a-f]{64}$ ]]
[[ ${CI_IMAGE:-} == "calfit-ci:sha-$GITHUB_SHA" ]]
: "${RUNNER_TEMP:?}" "${GITHUB_WORKSPACE:?}"

prefix="calfit-ci-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
network="$prefix-net"
db="$prefix-mysql"
bootstrap="$prefix-bootstrap"
api="$prefix-validate"
out="$RUNNER_TEMP/runtime-results"
mkdir -p "$out"
umask 077
credentials=$(mktemp -d "$RUNNER_TEMP/calfit-ci-env.XXXXXX")
containers=()
network_created=false
cleanup() {
  local status=$?
  trap - EXIT
  set +e
  if (( status != 0 )); then
    for container in "${containers[@]}"; do docker logs --tail 100 "$container"; done
  fi
  # Only IDs created by this invocation; no pruning, named volumes or Compose.
  for container in "${containers[@]}"; do docker rm --force "$container" >/dev/null; done
  if [[ $network_created == true ]]; then docker network rm "$network" >/dev/null; fi
  rm -f "$credentials/mysql.env" "$credentials/api.env"
  rmdir "$credentials"
  exit "$status"
}
trap cleanup EXIT

root_password=$(openssl rand -hex 24)
db_password=$(openssl rand -hex 24)
jwt_secret=$(openssl rand -hex 64)
watch_secret=$(openssl rand -hex 32)
for value in "$root_password" "$db_password" "$jwt_secret" "$watch_secret"; do
  printf '::add-mask::%s\n' "$value"
done
cat > "$credentials/mysql.env" <<EOF
MYSQL_ROOT_PASSWORD=$root_password
MYSQL_DATABASE=calfit_ci
MYSQL_USER=calfit_ci
MYSQL_PASSWORD=$db_password
TZ=Asia/Seoul
EOF
cat > "$credentials/api.env" <<EOF
SPRING_PROFILES_ACTIVE=prod
STORAGE_DATABASE_CORE_DB_URL=mysql:3306/calfit_ci?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
STORAGE_DATABASE_CORE_DB_USERNAME=calfit_ci
STORAGE_DATABASE_CORE_DB_PASSWORD=$db_password
STORAGE_DATASOURCE_CORE_MAXIMUMPOOLSIZE=5
STORAGE_DATASOURCE_CORE_CONNECTIONTIMEOUT=30000
JWT_SECRET=$jwt_secret
CALENDAR_GOOGLE_WATCH_TOKEN_SECRET=$watch_secret
CORS_ALLOWEDORIGINS=https://calfit-ci.invalid
MANAGEMENT_SERVER_PORT=8081
MANAGEMENT_SERVER_ADDRESS=127.0.0.1
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health
MANAGEMENT_ENDPOINT_HEALTH_SHOWDETAILS=never
MANAGEMENT_HEALTH_MAIL_ENABLED=false
SPRING_H2_CONSOLE_ENABLED=false
LOGGING_CONFIG=/app/config/logback-oracle.xml
APP_EMAIL_ENABLED=false
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=ci-validation-only
AWS_SECRET_ACCESS_KEY=ci-validation-only
AWS_EC2_METADATA_DISABLED=true
S3_APPLICANT_BUCKET=ci-validation-only
GOOGLE_CLIENT_ID=ci-validation-only
GOOGLE_CLIENT_SECRET=ci-validation-only
TOSS_CLIENT_KEY=ci-validation-only
TOSS_SECRET_KEY=ci-validation-only
JAVA_OPTS=-Xms256m -Xmx768m -XX:ActiveProcessorCount=2
TZ=Asia/Seoul
EOF

image_id=$(docker image inspect "$CI_IMAGE" --format '{{.Id}}')
test "$(docker image inspect "$image_id" --format '{{.Os}}/{{.Architecture}}')" = linux/arm64
docker image inspect "$image_id" | jq -e --arg sha "$GITHUB_SHA" --arg jar "$EXPECTED_JAR_SHA256" \
  '.[0].Config.Labels | .["org.opencontainers.image.revision"] == $sha and .["com.calfit.jar.sha256"] == $jar and .["com.calfit.java.runtime"] == "17.0.21.1+2"'
docker run --rm --platform linux/arm64 --network none --entrypoint java "$image_id" \
  -XshowSettings:properties -version > "$out/java-runtime.txt" 2>&1
grep -Fx '    java.runtime.version = 17.0.21.1+2' "$out/java-runtime.txt"
grep -Fx '    java.vendor = Eclipse Adoptium' "$out/java-runtime.txt"
grep -Fx '    os.arch = aarch64' "$out/java-runtime.txt"

docker pull --platform linux/arm64 mysql:8.4.13
docker network create --internal "$network" >/dev/null
network_created=true
containers+=("$(docker create --name "$db" --platform linux/arm64 --network "$network" \
  --network-alias mysql --env-file "$credentials/mysql.env" --memory 2g \
  --tmpfs /var/lib/mysql:rw,nosuid,size=2g,mode=1777 mysql:8.4.13 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci)")
docker start "$db" >/dev/null
sql() {
  docker exec "$db" sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --protocol=TCP -h127.0.0.1 -u"$MYSQL_USER" --database="$MYSQL_DATABASE" --connect-timeout=5 -N -B -e "$1"' sh "$1"
}
ready=false
for (( attempt=0; attempt<60; attempt++ )); do
  if sql 'SELECT 1' >/dev/null 2>&1; then ready=true; break; fi
  sleep 3
done
[[ $ready == true ]]
test "$(sql 'SELECT VERSION()')" = 8.4.13

start_api() {
  local name=$1 ddl=$2
  containers+=("$(docker create --name "$name" --platform linux/arm64 --network "$network" \
    --env-file "$credentials/api.env" --env "SPRING_JPA_HIBERNATE_DDLAUTO=$ddl" \
    --mount "type=bind,source=$GITHUB_WORKSPACE/deploy/oracle/logback.xml,target=/app/config/logback-oracle.xml,readonly" \
    --memory 1536m --init "$image_id")")
  docker start "$name" >/dev/null
}
wait_healthy() {
  local name=$1
  for (( attempt=0; attempt<60; attempt++ )); do
    [[ $(docker inspect "$name" --format '{{.State.Running}}') == true ]] || return 1
    if docker exec "$name" curl --fail --silent --show-error --max-time 5 \
        http://127.0.0.1:8081/actuator/health > "$out/health.json" 2>/dev/null && \
        jq -e '.status == "UP"' "$out/health.json" >/dev/null; then return 0; fi
    sleep 3
  done
  return 1
}
schema_hash() {
  docker exec "$db" sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump --protocol=TCP -h127.0.0.1 -u"$MYSQL_USER" --no-data --skip-comments --skip-dump-date --skip-add-locks --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"' | sha256sum | cut -d ' ' -f 1
}

# Only the new, tmpfs-backed DB can receive schema creation. The tested instance
# is a separate container started with validate against that initialized DB.
start_api "$bootstrap" update
wait_healthy "$bootstrap"
docker logs "$bootstrap" > "$RUNNER_TEMP/schema-bootstrap.log" 2>&1
if grep -E 'CommandAcceptanceException|SchemaManagementException|GenerationTarget encountered exception' "$RUNNER_TEMP/schema-bootstrap.log"; then
  echo 'Temporary schema initialization failed' >&2
  exit 1
fi
docker stop --time 30 "$bootstrap" >/dev/null
test "$(sql 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()')" -gt 0
before=$(schema_hash)
start_api "$api" validate
wait_healthy "$api"
docker inspect "$api" | jq -e '.[0].Config.Env | index("SPRING_PROFILES_ACTIVE=prod") != null and index("SPRING_JPA_HIBERNATE_DDLAUTO=validate") != null'
docker inspect "$api" | jq -e '.[0].HostConfig.PortBindings | . == null or length == 0'
docker inspect "$db" | jq -e '.[0].Mounts | all(.Type != "volume") and any(.Type == "tmpfs" and .Destination == "/var/lib/mysql")'
test "$(docker exec "$api" curl --silent --output /dev/null --write-out '%{http_code}' --max-time 10 http://127.0.0.1:8080/health)" = 200
for path in /h2-console/ /actuator/health /docs/ci-does-not-exist; do
  test "$(docker exec "$api" curl --silent --output /dev/null --write-out '%{http_code}' --max-time 10 "http://127.0.0.1:8080$path")" = 404
done
after=$(schema_hash)
test "$before" = "$after"
jq -n --arg sha "$GITHUB_SHA" --arg image "$image_id" --arg jar "$EXPECTED_JAR_SHA256" --arg schema "$after" \
  '{passed: true, sourceSha: $sha, imageId: $image, jarSHA256: $jar, platform: "linux/arm64", javaRuntime: "17.0.21.1+2", mysql: "8.4.13", profile: "prod", ddl: "validate", health: "UP", schemaPreserved: true, schemaSHA256: $schema, publishedPorts: false, databaseStorage: "disposable tmpfs"}' \
  > "$out/result.json"
printf 'ARM64 Java, MySQL 8.4.13, prod/validate, health and schema preservation passed.\n'
