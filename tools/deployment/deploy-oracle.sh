#!/usr/bin/env bash
# Poll public GitHub metadata; never executes remote code or writes GitHub status.
set -euo pipefail
umask 077
readonly repo=coffiiness/BE registry=ghcr.io/coffiiness/calfit-backend
readonly api=https://api.github.com/repos/coffiiness/BE project=calfit-oracle
root=/home/ubuntu/calfit/BE
dry_run=false
recover_only=false
while (( $# )); do
  case "$1" in
    --dry-run) dry_run=true; shift ;;
    --recover) recover_only=true; shift ;;
    --root) [[ $# -ge 2 ]] || exit 2; root=$2; shift 2 ;;
    --help) echo 'Usage: bash deploy-oracle.sh [--dry-run] [--recover] [--root /absolute/BE]'; exit 0 ;;
    *) echo 'Unknown argument' >&2; exit 2 ;;
  esac
done
log() { printf '%s\n' "$*"; }
die() { log "STOP: $*" >&2; exit 1; }
for tool in curl jq flock sha256sum mktemp realpath; do command -v "$tool" >/dev/null || die "Missing package: $tool"; done
[[ $root == /* && -d $root ]] || die 'Root must be an existing absolute directory'
root=$(realpath -e -- "$root")
state="$root/.oracle-deploy"
[[ ! -L $state ]] || die 'State directory must not be a symlink'
mkdir -p "$state"
chmod 700 "$state"
[[ ! -L $state/lock ]] || die 'Lock must not be a symlink'
exec 9>"$state/lock"
if ! flock --nonblock 9; then log 'SKIP: another deployment check holds the lock'; exit 0; fi
work=$(mktemp -d "$state/check.XXXXXX")
candidate_file=
attempt_started=false
envfile="$root/.env.prod"
compose="$root/docker-compose.prod.yml"
version_file="$root/.env.image"
cleanup() {
  local rc=$?
  trap - EXIT
  set +e
  if [[ $attempt_started == true && $rc != 0 && -f $state/pending.json ]]; then
    if recover_pending; then
      log 'ROLLBACK: previous image, version selection and internal health restored'
    else
      mark_rollback_failed
      log 'STOP: rollback failed; pending and recovery data preserved; automatic deployment blocked' >&2
    fi
  fi
  [[ -z $candidate_file ]] || rm -f -- "$candidate_file"
  # All temporary responses are from this invocation, never service/volume data.
  rm -f -- "$work/"*.json "$work/"*.tmp
  rmdir -- "$work"
  exit "$rc"
}
trap cleanup EXIT
dkr() {
  env -u DOCKER_HOST -u DOCKER_CONTEXT -u DOCKER_TLS_VERIFY -u DOCKER_CERT_PATH \
    docker --host unix:///var/run/docker.sock "$@"
}
dc() {
  env -u DOCKER_HOST -u DOCKER_CONTEXT -u DOCKER_TLS_VERIFY -u DOCKER_CERT_PATH \
    -u COMPOSE_FILE -u COMPOSE_PROJECT_NAME -u COMPOSE_PROFILES -u COMPOSE_ENV_FILES \
    BACKEND_IMAGE="$image" JPA_DDL_AUTO=validate \
    docker --host unix:///var/run/docker.sock compose --project-name "$project" \
    --env-file "$envfile" --env-file "$candidate_file" -f "$compose" "$@"
}
dc_version() {
  # Resolve the preserved version WITHOUT a forced BACKEND_IMAGE, including the
  # original .env.prod fallback when no version file existed.
  local selection=$1; shift
  local files=(--env-file "$envfile")
  [[ -z $selection ]] || files+=(--env-file "$selection")
  env -u DOCKER_HOST -u DOCKER_CONTEXT -u DOCKER_TLS_VERIFY -u DOCKER_CERT_PATH \
    -u COMPOSE_FILE -u COMPOSE_PROJECT_NAME -u COMPOSE_PROFILES -u COMPOSE_ENV_FILES \
    -u BACKEND_IMAGE JPA_DDL_AUTO=validate \
    docker --host unix:///var/run/docker.sock compose --project-name "$project" \
    "${files[@]}" -f "$compose" "$@"
}
setup_real() {
  [[ $(uname -m) == aarch64 ]] || return 1
  command -v docker >/dev/null || return 1
  [[ -f $envfile && ! -L $envfile && -f $compose ]] || return 1
  [[ ! -L $version_file && ( ! -e $version_file || -f $version_file ) ]] || return 1
}
invariants() {
  [[ $(dc ps -q mysql) == "$mysql_container" ]] || return 1
  [[ $(dkr inspect "$mysql_container" --format '{{.State.StartedAt}}') == "$mysql_started" ]] || return 1
  [[ $(dkr inspect "$mysql_container" --format '{{range .Mounts}}{{if eq .Destination "/var/lib/mysql"}}{{.Name}}{{end}}{{end}}') == calfit_oracle_mysql84 ]] || return 1
  [[ $(sha256sum "$envfile" | cut -d ' ' -f 1) == "$env_hash" ]] || return 1
}
internal_health() {
  local expected=$1 container code
  container=$(dc ps -q core-api) || return 1
  [[ -n $container ]] || return 1
  [[ $(dkr inspect "$container" --format '{{.State.Running}}') == true ]] || return 1
  [[ $(dkr inspect "$container" --format '{{.Image}}') == "$expected" ]] || return 1
  [[ $(dkr inspect "$container" --format '{{.State.Health.Status}}') == healthy ]] || return 1
  dkr exec "$container" curl --fail --silent --show-error --noproxy '*' --max-time 10 \
    http://127.0.0.1:8081/actuator/health 2>/dev/null | jq -e '.status == "UP"' >/dev/null || return 1
  code=$(curl --silent --show-error --noproxy '*' --connect-timeout 3 --max-time 10 \
    --output /dev/null --write-out '%{http_code}' http://127.0.0.1:8080/health 2>/dev/null) || return 1
  [[ $code == 200 ]] || return 1
}
external_health() {
  local code
  code=$(curl --silent --show-error --proto '=https' --tlsv1.2 --connect-timeout 5 --max-time 15 \
    --output /dev/null --write-out '%{http_code}' https://api.calfit.co.kr/health 2>/dev/null) || return 1
  [[ $code == 200 ]]
}
version_matches() {
  local selection=$1 expected=$2 configured
  if [[ -n $selection ]]; then
    [[ -f $selection && ! -L $selection ]] || return 1
    [[ $(grep -c '^BACKEND_IMAGE=' "$selection") == 1 ]] || return 1
    if grep -qEv '^(BACKEND_IMAGE=.*|[[:space:]]*(#.*)?)$' "$selection"; then return 1; fi
  fi
  configured=$(dc_version "$selection" config --format json | jq -er '.services["core-api"].image') || return 1
  [[ $(dkr image inspect "$configured" --format '{{.Id}}') == "$expected" ]] || return 1
}
mark_rollback_failed() {
  jq '.status="rollback_failed" | .note="Recovery not verified; manual repair required"' "$state/pending.json" > "$work/blocked.json" &&
    mv -f -- "$work/blocked.json" "$state/pending.json"
}
recover_pending() {
  # Called both after a failed replacement and on startup after interruption.
  # Explicit return checks are required: this function is used as a conditional.
  setup_real || return 1
  local pending="$state/pending.json" id failed_digest backup recovery old_id rollback selection phase
  jq -e '(.deploymentId | type == "number" and . > 0 and floor == .) and
    (.digest | type == "string" and test("^sha256:[0-9a-f]{64}$"))' "$pending" >/dev/null || return 1
  id=$(jq -r '.deploymentId' "$pending") || return 1
  failed_digest=$(jq -r '.digest' "$pending") || return 1
  phase=$(jq -r '.phase // "replacing"' "$pending") || return 1
  backup="$state/backups/$id"
  [[ $(jq -r '.backup' "$pending") == "$backup" && -d $backup && ! -L $backup ]] || return 1
  recovery="$backup/recovery.json"
  jq -e '(.previousImageId | test("^sha256:[0-9a-f]{64}$")) and
    (.rollbackImage | test("^calfit-backend:rollback-deployment-[0-9]+$")) and
    (.mysqlContainer | test("^[a-zA-Z0-9_.-]+$")) and (.mysqlStarted | type == "string") and
    (.productionEnvSHA256 | test("^[0-9a-fA-F]{64}$"))' "$recovery" >/dev/null || return 1
  old_id=$(jq -r '.previousImageId' "$recovery") || return 1
  rollback=$(jq -r '.rollbackImage' "$recovery") || return 1
  [[ $rollback == "calfit-backend:rollback-deployment-$id" ]] || return 1
  local image="$rollback" candidate_file="$backup/recovery.env"
  [[ -f $candidate_file && $(cat "$candidate_file") == "BACKEND_IMAGE=$rollback" ]] || return 1
  local mysql_container mysql_started env_hash
  mysql_container=$(jq -r '.mysqlContainer' "$recovery") || return 1
  mysql_started=$(jq -r '.mysqlStarted' "$recovery") || return 1
  env_hash=$(jq -r '.productionEnvSHA256' "$recovery") || return 1
  invariants || return 1
  [[ $(dkr image inspect "$rollback" --format '{{.Id}}') == "$old_id" ]] || return 1
  if [[ -f $backup/env.image.previous && ! -e $backup/env.image.absent ]]; then
    selection="$backup/env.image.previous"
  elif [[ -f $backup/env.image.absent && ! -e $backup/env.image.previous ]]; then
    selection=
  else return 1; fi
  # Never restore a stale tag/config that would select a different image later.
  version_matches "$selection" "$old_id" || return 1
  if [[ -f $backup/current.previous.json ]]; then
    jq -e '.status == "success"' "$backup/current.previous.json" >/dev/null || return 1
  elif [[ ! -f $backup/current.absent && -f $state/current.json ]]; then
    # Legacy 15-10 recovery: don't guess after a partially committed success.
    [[ $(jq -r '.digest' "$state/current.json") != "$failed_digest" ]] || return 1
  fi
  jq '.status="rolling_back"' "$pending" > "$work/rolling.json" && mv -f -- "$work/rolling.json" "$pending" || return 1
  # A prepared attempt has not launched Compose up yet. Restore selection only,
  # and never recreate a still-healthy previous container on recovery/retry.
  if [[ $phase == prepared ]]; then
    internal_health "$old_id" || return 1
  elif ! internal_health "$old_id"; then
    dc up -d --no-deps --no-build --pull never --wait --wait-timeout 600 core-api || return 1
  fi
  internal_health "$old_id" || return 1
  invariants || return 1
  if [[ -n $selection ]]; then
    local restored
    restored=$(mktemp "$root/.env.image.restore.XXXXXX") || return 1
    cp -p -- "$selection" "$restored" && mv -f -- "$restored" "$version_file" || return 1
  else rm -f -- "$version_file" || return 1; fi
  if [[ -n $selection ]]; then version_matches "$version_file" "$old_id" || return 1
  else [[ ! -e $version_file ]] && version_matches '' "$old_id" || return 1; fi
  # Restore the exact previous success journal even if killed during finalization.
  if [[ -f $backup/current.previous.json ]]; then
    cp -- "$backup/current.previous.json" "$work/previous.json" && mv -f -- "$work/previous.json" "$state/current.json" || return 1
  elif [[ -f $backup/current.absent ]]; then rm -f -- "$state/current.json" || return 1; fi
  if [[ -f $state/digests/${failed_digest#sha256:}.json ]]; then
    jq -e --argjson id "$id" '.deploymentId == $id' "$state/digests/${failed_digest#sha256:}.json" >/dev/null || return 1
    rm -- "$state/digests/${failed_digest#sha256:}.json" || return 1
  fi
  mkdir -p "$state/failures/requests" "$state/failures/digests" || return 1
  jq --arg date "$(date -u +%Y-%m-%dT%H:%M:%SZ)" '.status="rolled_back" | .recoveredAt=$date' "$pending" > "$work/recovered.json" || return 1
  cp -- "$work/recovered.json" "$work/digest-failure.json" &&
    mv -f -- "$work/digest-failure.json" "$state/failures/digests/${failed_digest#sha256:}.json" || return 1
  mv -f -- "$work/recovered.json" "$state/failures/requests/$id.json" || return 1
  rm -- "$pending" || return 1
}
if [[ -e $state/pending.json ]]; then
  if [[ $dry_run == true ]]; then log 'DRY-RUN: pending recovery required; no env/Docker access'; exit 0; fi
  [[ $(jq -r '.status' "$state/pending.json") != rollback_failed || $recover_only == true ]] || die 'Rollback previously failed; inspect/repair then explicitly use --recover'
  if recover_pending; then log 'RECOVERED: interrupted deployment; next invocation may check a new request'; exit 0; fi
  mark_rollback_failed || true
  die 'Pending recovery could not be verified; automatic deployment blocked'
fi
if [[ $recover_only == true ]]; then log 'SKIP: no pending recovery; no new deployment requested'; exit 0; fi
get() {
  local endpoint=$1 destination=$2
  curl --fail --silent --show-error --proto '=https' --tlsv1.2 \
    --connect-timeout 10 --max-time 30 --max-filesize 4194304 \
    --header 'Accept: application/vnd.github+json' --header 'X-GitHub-Api-Version: 2022-11-28' \
    --header 'User-Agent: calfit-oracle-deployer' \
    "$api/$endpoint" > "$destination" || die 'GitHub GET failed; no replacement attempted'
  jq -e 'type == "object" or type == "array"' "$destination" >/dev/null || die 'Invalid GitHub JSON'
}
latest_endpoint='deployments?environment=oracle-production&task=deploy&per_page=1'
get "$latest_endpoint" "$work/latest.json"
jq -e 'type == "array" and length <= 1' "$work/latest.json" >/dev/null || die 'Unexpected deployment list'
if [[ $(jq length "$work/latest.json") == 0 ]]; then log 'SKIP: no deployment request'; exit 0; fi
jq '.[0]' "$work/latest.json" > "$work/request.json"
jq -e --arg repo "$repo" --arg registry "$registry" --arg api "$api" '
  .environment == "oracle-production" and .task == "deploy" and .production_environment == true and
  .repository_url == $api and .creator.login == "github-actions[bot]" and .creator.type == "Bot" and
  (.id | type == "number" and . > 0 and floor == .) and
  (.payload | type == "object") and .payload.schemaVersion == 1 and
  .payload.repository == $repo and .payload.image == $registry and
  (.payload.commitSha | type == "string" and test("^[0-9a-f]{40}$")) and
  .sha == .payload.commitSha and .ref == .sha and
  (.payload.digest | type == "string" and test("^sha256:[0-9a-f]{64}$")) and
  (.payload.imageId | type == "string" and test("^sha256:[0-9a-f]{64}$")) and
  (.payload.workflowRunId | type == "number" and . > 0 and floor == .) and
  (.payload.workflowRunAttempt | type == "number" and . > 0 and floor == .)
' "$work/request.json" >/dev/null || die 'Latest request is invalid; older requests are not used'
deployment=$(jq -r '.id' "$work/request.json")
sha=$(jq -r '.sha' "$work/request.json")
digest=$(jq -r '.payload.digest' "$work/request.json")
image_id=$(jq -r '.payload.imageId' "$work/request.json")
run=$(jq -r '.payload.workflowRunId' "$work/request.json")
attempt=$(jq -r '.payload.workflowRunAttempt' "$work/request.json")
image="$registry@$digest"
if [[ -e $state/failures/digests/${digest#sha256:}.json || -e $state/failures/requests/$deployment.json ]]; then
  log 'SKIP: failed digest/request is quarantined'; exit 0
fi
get 'git/ref/heads/main' "$work/main.json"
[[ $(jq -r '.object.sha' "$work/main.json") == "$sha" ]] || die 'Request is not the latest main commit'
get 'actions/workflows/ci.yml' "$work/workflow.json"
jq -e '.id > 0 and .name == "Backend CI/CD" and .path == ".github/workflows/ci.yml"' "$work/workflow.json" >/dev/null || die 'Unexpected workflow identity'
workflow_id=$(jq -r '.id' "$work/workflow.json")
verify_run() {
  get "actions/runs/$run" "$work/run.json"
  jq -e --arg repo "$repo" --arg sha "$sha" --argjson run "$run" --argjson attempt "$attempt" --argjson wf "$workflow_id" '
    .id == $run and .workflow_id == $wf and .run_attempt == $attempt and
    .name == "Backend CI/CD" and .path == ".github/workflows/ci.yml" and
    .event == "push" and .head_branch == "main" and .head_sha == $sha and
    .repository.full_name == $repo and .head_repository.full_name == $repo and
    .status == "completed" and .conclusion == "success"
  ' "$work/run.json" >/dev/null || die 'Workflow is not a completed, successful main Push of the requested attempt'
}
verify_run
get "actions/runs/$run/attempts/$attempt/jobs?per_page=100" "$work/jobs.json"
jq -e --arg sha "$sha" --argjson run "$run" '
  .total_count == 3 and (.jobs | length) == 3 and
  ([.jobs[].name] | sort) == (["Quality Gate and verified bootJar (AMD64)", "Build and verify the ARM64 runtime", "Publish the tested image to GHCR"] | sort) and
  all(.jobs[]; .run_id == $run and .head_sha == $sha and .status == "completed" and .conclusion == "success")
' "$work/jobs.json" >/dev/null || die 'Required jobs did not all succeed'
get "deployments/$deployment/statuses?per_page=1" "$work/statuses.json"
jq -e 'type == "array" and (length == 0 or (.[0].state == "pending" or .[0].state == "queued"))' "$work/statuses.json" >/dev/null || die 'Deployment request was cancelled, completed or marked failed'

# Local durable success history is the authority for this Oracle installation.
if [[ -f $state/current.json ]]; then
  jq -e '.status == "success" and
    (.commitSha | type == "string" and test("^[0-9a-f]{40}$")) and
    (.digest | type == "string" and test("^sha256:[0-9a-f]{64}$")) and
    (.workflowRunId | type == "number" and . > 0 and floor == .) and
    (.workflowRunAttempt | type == "number" and . > 0 and floor == .) and
    (.deploymentId | type == "number" and . > 0 and floor == .)
  ' "$state/current.json" >/dev/null || die 'Invalid local deployment state'
  if [[ $(jq -r '.digest' "$state/current.json") == "$digest" ]]; then log 'SKIP: digest already deployed'; exit 0; fi
  jq -e --argjson id "$deployment" --argjson run "$run" --argjson attempt "$attempt" '
    $id > .deploymentId and ($run > .workflowRunId or ($run == .workflowRunId and $attempt > .workflowRunAttempt))
  ' "$state/current.json" >/dev/null || die 'Older deployment/run/attempt cannot replace a newer local release'
  previous_sha=$(jq -r '.commitSha' "$state/current.json")
  if [[ $previous_sha != "$sha" ]]; then
    get "compare/$previous_sha...$sha" "$work/compare.json"
    jq -e '.status == "ahead"' "$work/compare.json" >/dev/null || die 'Main would move backwards or diverge from the deployed release'
  fi
fi
if [[ -e $state/digests/${digest#sha256:}.json ]]; then log 'SKIP: digest exists in successful deployment history'; exit 0; fi
if [[ $dry_run == true ]]; then
  log "DRY-RUN: valid deployment $deployment, main $sha, run $run attempt $attempt"
  log "DRY-RUN: would pull $image, inspect image/labels, preserve current image and replace core-api only"
  log 'DRY-RUN: no Docker commands, production env access, image-file update or success record'
  exit 0
fi
setup_real || die 'Real deployment requires ARM64, Docker and regular production files'
env_hash=$(sha256sum "$envfile" | cut -d ' ' -f 1)
candidate_file=$(mktemp "$root/.env.image.tmp.XXXXXX")
printf 'BACKEND_IMAGE=%s\n' "$image" > "$candidate_file"
dc config --format json | jq -e --arg image "$image" '
  .services["core-api"] as $a |
  $a.image == $image and ($a | has("build") | not) and $a.platform == "linux/arm64" and
  $a.environment.SPRING_PROFILES_ACTIVE == "prod" and $a.environment.SPRING_JPA_HIBERNATE_DDLAUTO == "validate" and
  .services.mysql.image == "mysql:8.4.13" and .volumes.mysql_data.external == true and
  .volumes.mysql_data.name == "calfit_oracle_mysql84"
' >/dev/null || die 'Resolved Compose settings are not the approved production configuration'
api_container=$(dc ps -q core-api)
mysql_container=$(dc ps -q mysql)
[[ -n $api_container && -n $mysql_container ]] || die 'Existing API and MySQL must already be running'
[[ $(dkr inspect "$api_container" --format '{{.State.Running}}') == true ]] || die 'Existing backend is not running'
[[ $(dkr inspect "$mysql_container" --format '{{.State.Health.Status}}') == healthy ]] || die 'Existing MySQL is not healthy'
mysql_started=$(dkr inspect "$mysql_container" --format '{{.State.StartedAt}}')
[[ $(dkr inspect "$mysql_container" --format '{{range .Mounts}}{{if eq .Destination "/var/lib/mysql"}}{{.Name}}{{end}}{{end}}') == calfit_oracle_mysql84 ]] || die 'Unexpected database volume'
old_id=$(dkr inspect "$api_container" --format '{{.Image}}')
[[ $old_id =~ ^sha256:[0-9a-f]{64}$ ]] || die 'Invalid running image ID'
if [[ -f $version_file ]]; then previous_selection="$version_file"; else previous_selection=; fi
version_matches "$previous_selection" "$old_id" || die 'Previous version selection does not match the running image; repair before deployment'
dkr pull --platform linux/arm64 "$image"
dkr image inspect "$image" | jq -e --arg id "$image_id" --arg sha "$sha" --arg image "$image" '
  length == 1 and .[0].Id == $id and .[0].Os == "linux" and .[0].Architecture == "arm64" and
  .[0].Config.Labels["org.opencontainers.image.source"] == "https://github.com/coffiiness/BE" and
  .[0].Config.Labels["org.opencontainers.image.revision"] == $sha and
  (.[0].RepoDigests | index($image)) != null and
  .[0].Config.Labels["com.calfit.java.runtime"] == "17.0.21.1+2"
' >/dev/null || die 'Pulled image identity, architecture or revision mismatch'

# Recheck after the potentially slow pull. No replacement on new API failures.
get 'git/ref/heads/main' "$work/main.json"
[[ $(jq -r '.object.sha' "$work/main.json") == "$sha" ]] || die 'Main advanced while pulling'
get "$latest_endpoint" "$work/latest.json"
jq -e --argjson request "$(cat "$work/request.json")" 'length == 1 and .[0] == $request' "$work/latest.json" >/dev/null || die 'Latest request changed while pulling'
verify_run
get "deployments/$deployment/statuses?per_page=1" "$work/statuses.json"
jq -e 'type == "array" and (length == 0 or (.[0].state == "pending" or .[0].state == "queued"))' "$work/statuses.json" >/dev/null || die 'Request status changed while pulling'
[[ $(dkr inspect "$api_container" --format '{{.Image}}') == "$old_id" ]] || die 'Running backend changed outside the deployment lock'
[[ $(sha256sum "$envfile" | cut -d ' ' -f 1) == "$env_hash" ]] || die 'Production env changed while pulling'

backup="$state/backups/$deployment"
[[ ! -e $backup ]] || die 'Recovery directory already exists; manual inspection required'
mkdir -p "$backup" "$state/digests"
rollback_image="calfit-backend:rollback-deployment-$deployment"
dkr image tag "$old_id" "$rollback_image"
[[ $(dkr image inspect "$rollback_image" --format '{{.Id}}') == "$old_id" ]] || die 'Could not preserve the running image'
printf 'BACKEND_IMAGE=%s\n' "$rollback_image" > "$backup/recovery.env"
if [[ -f $version_file ]]; then cp -p -- "$version_file" "$backup/env.image.previous"; else touch "$backup/env.image.absent"; fi
if [[ -f $state/current.json ]]; then cp -p -- "$state/current.json" "$backup/current.previous.json"; else touch "$backup/current.absent"; fi
jq -n --arg old "$old_id" --arg rollback "$rollback_image" --arg mysql "$mysql_container" --arg started "$mysql_started" \
  --arg envHash "$env_hash" '{previousImageId:$old,rollbackImage:$rollback,mysqlContainer:$mysql,mysqlStarted:$started,productionEnvSHA256:$envHash}' > "$backup/recovery.json"
jq -n --argjson deployment "$deployment" --argjson run "$run" --argjson attempt "$attempt" --arg sha "$sha" \
  --arg digest "$digest" --arg id "$image_id" --arg backup "$backup" \
  '{status:"in_progress",phase:"prepared",deploymentId:$deployment,workflowRunId:$run,workflowRunAttempt:$attempt,commitSha:$sha,digest:$digest,imageId:$id,backup:$backup}' > "$work/pending.json"
mv -- "$work/pending.json" "$state/pending.json"
attempt_started=true
# Same directory/filesystem: image selection changes atomically. Secret env untouched.
mv -f -- "$candidate_file" "$version_file"
candidate_file="$version_file"
# cleanup must never remove the active version file, even on a failed replacement.
active_file="$candidate_file"
candidate_file=
dc_active() {
  candidate_file="$active_file" dc "$@"
}
jq '.phase="replacing"' "$state/pending.json" > "$work/replacing.json"
mv -f -- "$work/replacing.json" "$state/pending.json"
dc_active up -d --no-deps --no-build --pull never --wait --wait-timeout 600 core-api || die 'Backend replacement/health failed; rolling back'
candidate_file="$active_file" internal_health "$image_id" || die 'Internal health failed; rolling back'
candidate_file="$active_file" invariants || die 'MySQL or production env invariant changed; recovery will fail closed'
external=up
if ! external_health; then external=degraded; log 'WARN: external API unavailable; internal health passed, no rollback'; fi
jq --arg date "$(date -u +%Y-%m-%dT%H:%M:%SZ)" --arg external "$external" \
  '.status="success" | .deployedAt=$date | .externalHealth=$external' "$state/pending.json" > "$work/success.json"
cp -- "$work/success.json" "$state/digests/${digest#sha256:}.json"
mv -f -- "$work/success.json" "$state/current.json"
rm -- "$state/pending.json"
attempt_started=false
log "SUCCESS (local only): deployment $deployment, $image"
