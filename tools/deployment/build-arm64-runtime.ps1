[CmdletBinding()]
param(
    [string]$ImageTag = 'calfit-backend:arm64-temurin-17.0.21.1-2',
    [string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $root '.gradle-user-codex/arm64-runtime-validation/build' }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if (-not $OutputDirectory.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'OutputDirectory must stay inside this worktree.'
}
New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
$context = Join-Path $OutputDirectory 'context'
New-Item -ItemType Directory -Force (Join-Path $context 'docker') | Out-Null
$names = @('SPRING_PROFILES_ACTIVE','APP_EMAIL_ENABLED','AWS_ACCESS_KEY_ID','AWS_SECRET_ACCESS_KEY','AWS_EC2_METADATA_DISABLED')
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
Push-Location $root
try {
    $env:SPRING_PROFILES_ACTIVE='local'
    $env:APP_EMAIL_ENABLED='false'
    $env:AWS_ACCESS_KEY_ID='validation-only'
    $env:AWS_SECRET_ACCESS_KEY='validation-only'
    $env:AWS_EC2_METADATA_DISABLED='true'
    $wrapper = if ($IsWindows) { Join-Path $root 'gradlew.bat' } else { Join-Path $root 'gradlew' }
    & $wrapper --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx384m -XX:ActiveProcessorCount=2' -PspotbugsMaxHeapSize=512m check qualityCheck :core:core-api:bootJar *> (Join-Path $OutputDirectory 'quality-gate.log')
    if ($LASTEXITCODE) { throw "Quality Gate failed; see $OutputDirectory/quality-gate.log. No image build was started." }
    $gate = Get-Content build/reports/quality/spotbugs-gate.json -Raw | ConvertFrom-Json
    $coverage = Get-Content build/reports/quality/coverage-summary.json -Raw | ConvertFrom-Json
    if (-not $gate.passed -or $gate.highFindings.Count -or $gate.newFindings.Count -or $gate.escalatedFindings.Count -or
            $coverage.total.LINE.percent -lt 75 -or $coverage.total.BRANCH.percent -lt 47) {
        throw 'The required report criteria did not pass.'
    }
    $jars = @(Get-ChildItem core/core-api/build/libs -Filter 'core-api-*.jar' | Where-Object Name -NotLike '*-plain.jar')
    if ($jars.Count -ne 1) { throw 'Expected exactly one operating bootJar.' }
    $jarHash = (Get-FileHash -LiteralPath $jars[0].FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    Copy-Item -LiteralPath $jars[0].FullName -Destination (Join-Path $context 'app.jar')
    Copy-Item -LiteralPath Dockerfile -Destination (Join-Path $context 'Dockerfile')
    Copy-Item -LiteralPath docker/install-temurin17.sh -Destination (Join-Path $context 'docker/install-temurin17.sh')
    Copy-Item -LiteralPath docker/verify-temurin17.sh -Destination (Join-Path $context 'docker/verify-temurin17.sh')
    if ((Get-FileHash (Join-Path $context 'app.jar') -Algorithm SHA256).Hash.ToLowerInvariant() -ne $jarHash) {
        throw 'JAR changed during artifact staging.'
    }
    $artifact = [ordered]@{
        schemaVersion=1; head=(& git rev-parse HEAD); worktreeDirty=[bool](& git status --porcelain)
        javaRuntime='17.0.21.1+2'; jarSHA256=$jarHash
        baselineSHA256=(Get-FileHash gradle/quality/spotbugs-baseline.json -Algorithm SHA256).Hash.ToLowerInvariant()
        gateReportSHA256=(Get-FileHash build/reports/quality/spotbugs-gate.json -Algorithm SHA256).Hash.ToLowerInvariant()
        linePercent=$coverage.total.LINE.percent; branchPercent=$coverage.total.BRANCH.percent
        spotbugsKnown=$gate.known; spotbugsNew=$gate.newFindings.Count; spotbugsHigh=$gate.highFindings.Count
        trust='Local gate record, not a signed CI attestation. Future CI must enforce artifact provenance and job dependencies.'
    }
    $artifact | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $OutputDirectory 'verified-artifact.json')
    & docker buildx build --platform linux/arm64 --target runtime-from-jar --build-arg ("APP_JAR_SHA256="+$jarHash) --label ("org.opencontainers.image.revision="+$artifact.head) --provenance=false --load --metadata-file (Join-Path $OutputDirectory 'image-metadata.json') --tag $ImageTag --file (Join-Path $context 'Dockerfile') $context *> (Join-Path $OutputDirectory 'image-build.log')
    if ($LASTEXITCODE) { throw "ARM64 image build failed; see $OutputDirectory/image-build.log." }
    $image = (& docker image inspect $ImageTag | ConvertFrom-Json)[0]
    if ($image.Architecture -ne 'arm64' -or $image.Os -ne 'linux') { throw 'Built image is not linux/arm64.' }
    $runtime = & docker run --rm --platform linux/arm64 --network none --entrypoint java $ImageTag -XshowSettings:properties -version 2>&1 | Out-String
    if ($LASTEXITCODE -or $runtime -notmatch 'java.runtime.version = 17.0.21.1\+2' -or $runtime -notmatch 'os.arch = aarch64') {
        throw 'Built runtime version/architecture mismatch.'
    }
    [IO.File]::WriteAllText((Join-Path $OutputDirectory 'java-runtime.txt'),$runtime,[Text.UTF8Encoding]::new($false))
    $metadata = Get-Content (Join-Path $OutputDirectory 'image-metadata.json') -Raw | ConvertFrom-Json
    $layerBytes = (& docker image history --no-trunc --human=false --format '{{.Size}}' $ImageTag | Measure-Object -Sum).Sum
    $displaySize = & docker images $ImageTag --format '{{.Size}}'
    [ordered]@{image=$ImageTag;imageId=$image.Id;manifestDigest=$metadata.'containerimage.digest';
        configDigest=$metadata.'containerimage.config.digest';architecture=$image.Architecture;
        contentSizeBytes=$image.Size;layerUncompressedBytes=$layerBytes;dockerReportedSize=$displaySize;
        jarSHA256=$jarHash;javaRuntime='17.0.21.1+2';published=$false} |
        ConvertTo-Json | Set-Content (Join-Path $OutputDirectory 'image-result.json')
    Write-Host "Quality Gate passed; linux/arm64 runtime built locally: $ImageTag"
} finally {
    Pop-Location
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
}

