[CmdletBinding()]
param(
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if (-not $OutputDirectory) {
    $OutputDirectory = Join-Path $repositoryRoot '.gradle-user-codex/oracle-transfer'
}
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
Get-Command git, tar -ErrorAction Stop | Out-Null

# Include the current worktree, including unstaged edits and untracked deployment
# files. git archive HEAD would silently omit those changes.
$candidates = @(& git -c core.quotepath=false -C $repositoryRoot ls-files --cached --others --exclude-standard)
if ($LASTEXITCODE -ne 0) { throw 'Cannot enumerate repository files.' }

$rootFiles = @(
    'Dockerfile', '.dockerignore', '.gitignore', 'docker-compose.prod.yml', '.env.prod.example',
    'gradlew', 'gradlew.bat', 'build.gradle', 'settings.gradle', 'gradle.properties', 'lint.gradle', 'quality.gradle',
    'docker/install-temurin17.sh', 'docker/verify-temurin17.sh'
)
$sourceRoots = @('core/', 'storage/', 'support/', 'clients/', 'gradle/', 'deploy/oracle/', 'docs/deployment/', 'docs/quality/', 'tools/deployment/')
$selected = [System.Collections.Generic.List[string]]::new()

foreach ($candidate in ($candidates | Sort-Object -Unique)) {
    $relative = $candidate.Replace('\', '/')
    $allowed = $rootFiles -contains $relative
    foreach ($prefix in $sourceRoots) {
        if ($relative.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { $allowed = $true }
    }
    if (-not $allowed) { continue }
    if ($relative -match '(^|/)(\.git|\.ssh|ssh|\.gradle[^/]*|build|out|node_modules|logs|backups|db-backups|dumps)(/|$)') { continue }
    $leaf = [System.IO.Path]::GetFileName($relative)
    if ($relative -ne '.env.prod.example' -and ($leaf -like '.env*' -or $leaf -like '*.env')) { continue }
    if ($leaf -match '(?i)^(id_rsa|id_ed25519|id_ecdsa)(\..*)?$|\.(pem|key|ppk|p12|pfx|jks|keystore|dump|dmp|bak|backup)$') { continue }
    if ($leaf -match '(?i)\.(sql\.(gz|zip|xz|bz2|zst))$|(backup|dump).*\.sql$') { continue }
    if ($leaf -match '(?i)^application-local\.(yml|yaml|properties)$|(secret|credential|oauth|service.account).*(\.(yml|yaml|properties|json))$') { continue }
    # Only classpath SQL source is needed by the backend build, never DB dumps.
    if ($leaf -match '(?i)\.sql$' -and $relative -notmatch '/src/(main|test)/resources/') { continue }

    $absolute = [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $relative))
    if (-not $absolute.StartsWith($repositoryRoot + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path escapes repository: $relative"
    }
    if (-not (Test-Path -LiteralPath $absolute -PathType Leaf)) { continue }
    # Do not follow file symlinks or directory junctions into unrelated locations.
    $cursor = $absolute
    while ($cursor.Length -ge $repositoryRoot.Length) {
        $item = Get-Item -LiteralPath $cursor -Force
        if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Refusing symlink/junction in source path: $relative"
        }
        if ($cursor -eq $repositoryRoot) { break }
        $cursor = [System.IO.Path]::GetDirectoryName($cursor)
    }
    if ($relative -eq '.env.prod.example') {
        foreach ($line in [System.IO.File]::ReadAllLines($absolute)) {
            if ($line -match '^([A-Z0-9_]*(?:PASSWORD|SECRET|TOKEN|ACCESS_KEY|CLIENT_ID|CLIENT_KEY)[A-Z0-9_]*)=(.*)$') {
                if ($Matches[2].Trim().Trim([char[]]@([char]34, [char]39)).Length -gt 0) {
                    throw "Secret field $($Matches[1]) must be blank in .env.prod.example."
                }
            }
        }
    }
    if ($leaf -match '(?i)\.(java|kt|groovy|xml|gradle|properties|yml|yaml|json|ps1|md|sh)$') {
        $content = [System.IO.File]::ReadAllText($absolute)
        if ($content -match '-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----|AKIA[0-9A-Z]{16}') {
            throw "Possible embedded credential in $relative; review before packaging."
        }
    }
    $selected.Add($relative)
}

foreach ($required in @(
    'Dockerfile', 'quality.gradle', 'docker/install-temurin17.sh', 'docker/verify-temurin17.sh', 'docker-compose.prod.yml', '.env.prod.example',
    'gradle/wrapper/gradle-wrapper.jar', 'deploy/oracle/logback.xml',
    'gradle/quality/runtime.gradle', 'gradle/quality/runtime.properties',
    'gradle/quality/spotbugs.gradle', 'gradle/quality/policy.groovy',
    'gradle/quality/policy-tests.gradle', 'gradle/quality/spotbugs-baseline.json'
)) {
    if (-not $selected.Contains($required)) { throw "Required source missing: $required" }
}

$bundleName = 'calfit-oracle-source-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$stageRoot = Join-Path $OutputDirectory ($bundleName + '-staging')
$stageBackend = Join-Path $stageRoot 'BE'
New-Item -ItemType Directory -Path $stageBackend -Force | Out-Null
foreach ($relative in $selected) {
    $destination = Join-Path $stageBackend $relative
    New-Item -ItemType Directory -Path ([System.IO.Path]::GetDirectoryName($destination)) -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $repositoryRoot $relative) -Destination $destination
    # Spotless uses native line endings. Normalize the Linux transfer copy only;
    # keep the Windows worktree and the reviewed baseline bytes unchanged.
    if ($relative -eq 'gradlew' -or $relative -match '\.(java|sh)$') {
        $linuxText = [System.IO.File]::ReadAllText($destination).Replace("`r`n", "`n")
        [System.IO.File]::WriteAllText($destination, $linuxText, [System.Text.UTF8Encoding]::new($false))
    }
}

$archive = Join-Path $OutputDirectory ($bundleName + '.tar.gz')
& tar -czf $archive -C $stageRoot BE
if ($LASTEXITCODE -ne 0) { throw 'tar failed.' }
$checksum = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
$checksumFile = $archive + '.sha256'
[System.IO.File]::WriteAllText($checksumFile, "$checksum  $([System.IO.Path]::GetFileName($archive))`n", [System.Text.Encoding]::ASCII)
$manifest = $archive + '.files.txt'
[System.IO.File]::WriteAllLines($manifest, $selected.ToArray(), [System.Text.UTF8Encoding]::new($false))

Write-Host "Packaged $($selected.Count) current worktree files. Review the manifest before transfer."
[PSCustomObject]@{
    Archive = $archive
    ChecksumFile = $checksumFile
    Manifest = $manifest
    Sha256 = $checksum
    FileCount = $selected.Count
}
