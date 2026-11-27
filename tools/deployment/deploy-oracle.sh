#!/usr/bin/env bash
# Poll public GitHub metadata; never executes remote code or writes GitHub status.
set -euo pipefail
umask 077
readonly repo=coffiiness/BE registry=ghcr.io/coffiiness/calfit-backend
readonly api=https://api.github.com/repos/coffiiness/BE project=calfit-oracle
root=/home/ubuntu/calfit/BE
dry_run=false
while (( $# )); do
  case "$1" in
    --dry-run) dry_run=true; shift ;;
    --root) [[ $# -ge 2 ]] || exit 2; root=$2; shift 2 ;;
    --help) echo 'Usage: bash deploy-oracle.sh [--dry-run] [--root /absolute/BE]'; exit 0 ;;
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
[[ ! -e $state/pending.json ]] || die 'Unresolved deployment attempt; inspect pending.json and recover manually (15-11)'
work=$(mktemp -d "$state/check.XXXXXX")
candidate_file=
attempt_started=false
cleanup() {
  local rc=$?
  trap - EXIT
  set +e
  if [[ $attempt_started == true && $rc != 0 && -f $state/pending.json ]]; then
    jq '.status="failed" | .note="Replacement did not complete; manual recovery required"' "$state/pending.json" > "$work/failed.json" &&
      mv -f -- "$work/failed.json" "$state/pending.json"
  fi
  [[ -z $candidate_file ]] || rm -f -- "$candidate_file"
  # All temporary responses are from this invocation, never service/volume data.
  rm -f -- "$work/"*.json "$work/"*.tmp
  rmdir -- "$work"
  exit "$rc"
}
trap cleanup EXIT
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
[[ $(uname -m) == aarch64 ]] || die 'Real deployment requires native ARM64 Linux'
command -v docker >/dev/null || die 'Docker is required'
envfile="$root/.env.prod"
compose="$root/docker-compose.prod.yml"
version_file="$root/.env.image"
[[ -f $envfile && ! -L $envfile && -f $compose ]] || die 'Missing regular production env or Compose file'
[[ ! -L $version_file && ( ! -e $version_file || -f $version_file ) ]] || die 'Image version file must be a regular file'
env_hash=$(sha256sum "$envfile" | cut -d ' ' -f 1)
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
jq -n --arg old "$old_id" --arg rollback "$rollback_image" --arg mysql "$mysql_container" --arg started "$mysql_started" \
  --arg envHash "$env_hash" '{previousImageId:$old,rollbackImage:$rollback,mysqlContainer:$mysql,mysqlStarted:$started,productionEnvSHA256:$envHash}' > "$backup/recovery.json"
jq -n --argjson deployment "$deployment" --argjson run "$run" --argjson attempt "$attempt" --arg sha "$sha" \
  --arg digest "$digest" --arg id "$image_id" --arg backup "$backup" \
  '{status:"in_progress",deploymentId:$deployment,workflowRunId:$run,workflowRunAttempt:$attempt,commitSha:$sha,digest:$digest,imageId:$id,backup:$backup}' > "$work/pending.json"
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
dc_active up -d --no-deps --no-build --pull never --wait --wait-timeout 600 core-api || die 'Backend replacement/health failed; recovery preserved, automatic rollback deferred to 15-11'
new_container=$(dc_active ps -q core-api)
[[ -n $new_container && $(dkr inspect "$new_container" --format '{{.Image}}') == "$image_id" ]] || die 'Running image does not match deployment'
[[ $(dkr inspect "$new_container" --format '{{.State.Health.Status}}') == healthy ]] || die 'Backend is not healthy'
[[ $(dc_active ps -q mysql) == "$mysql_container" && $(dkr inspect "$mysql_container" --format '{{.State.StartedAt}}') == "$mysql_started" ]] || die 'MySQL identity/start time changed'
[[ $(sha256sum "$envfile" | cut -d ' ' -f 1) == "$env_hash" ]] || die 'Production env changed externally'
jq --arg date "$(date -u +%Y-%m-%dT%H:%M:%SZ)" '.status="success" | .deployedAt=$date' "$state/pending.json" > "$work/success.json"
cp -- "$work/success.json" "$state/digests/${digest#sha256:}.json"
mv -f -- "$work/success.json" "$state/current.json"
rm -- "$state/pending.json"
attempt_started=false
log "SUCCESS (local only): deployment $deployment, $image"
