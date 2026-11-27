[CmdletBinding()]
param([string]$StateFile)
$ErrorActionPreference='Stop'
$PSNativeCommandUseErrorActionPreference=$false
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if(-not $StateFile){$StateFile=Join-Path $root '.gradle-user-codex/arm64-runtime-validation/qa-state.json'}
$state=Get-Content -LiteralPath $StateFile -Raw|ConvertFrom-Json
if($state.project -notmatch '^calfit-arm64qa-' -or $state.cloneVolume -notmatch '^calfit_arm64qa_' -or
   $state.dbContainer -ne ($state.project+'-mysql-1') -or $state.apiContainer -ne ($state.project+'-core-api-1')){
 throw 'Only a dedicated cloned ARM64 QA project is allowed.'
}
$dbInspect=(& docker inspect $state.dbContainer|ConvertFrom-Json)[0]
if(@($dbInspect.Mounts|Where-Object {$_.Name -eq $state.cloneVolume -and $_.Destination -eq '/var/lib/mysql'}).Count -ne 1){
 throw 'The database is not mounted from the declared QA clone.'
}
$out=Split-Path -Parent ([IO.Path]::GetFullPath($StateFile))
$api=$state.apiContainer
$values=@{}
foreach($line in [IO.File]::ReadAllLines($state.envFile)){if($line -match '^([A-Z0-9_]+)=(.*)$'){$values[$Matches[1]]=$Matches[2]}}
$results=[Collections.Generic.List[object]]::new()
function Invoke-Qa([string]$Path,[int]$Expected,[string]$Method='GET',[hashtable]$Headers=@{},[string]$Body='',[string]$ErrorCode=''){
 $curlArgs=@('exec','-i',$api,'curl','--silent','--show-error','--max-time','20','--request',$Method,
 '--dump-header','/tmp/calfit-arm64-http-headers','--output','/tmp/calfit-arm64-http-body','--write-out','%{http_code}')
 foreach($key in $Headers.Keys){$curlArgs+=@('--header',($key+': '+$Headers[$key]))}
 if($Body){$curlArgs+=@('--header','Content-Type: application/json','--data-binary','@-')}
 $curlArgs+=('http://127.0.0.1:8080'+$Path)
 $status=if($Body){$Body|& docker @curlArgs}else{& docker @curlArgs}
 $curlExit=$LASTEXITCODE
 if($curlExit -or [int]$status -ne $Expected){throw "Unexpected QA HTTP result for $Method $Path : $status (curl=$curlExit)"}
 $bodyText=(& docker exec $api cat /tmp/calfit-arm64-http-body)|Out-String
 $headersText=(& docker exec $api cat /tmp/calfit-arm64-http-headers)|Out-String
 if($ErrorCode -and ($bodyText|ConvertFrom-Json).error.code -ne $ErrorCode){throw "Wrong error code at $Path"}
 $results.Add([ordered]@{path=$Path;method=$Method;status=[int]$status;errorCode=$ErrorCode})
 return [pscustomobject]@{body=$bodyText;headers=$headersText}
}
function Set-QaReadOnly([bool]$Enabled){
 $sql=if($Enabled){'SET GLOBAL super_read_only=ON;'}else{'SET GLOBAL super_read_only=OFF; SET GLOBAL read_only=OFF;'}
 $command='MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --execute="'+$sql+'"'
 & docker exec $state.dbContainer sh -c $command
 if($LASTEXITCODE){throw 'QA write protection command failed'}
}
function Snapshot-Qa([string]$Prefix){
 & docker exec $state.dbContainer sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --skip-comments --skip-add-locks --skip-lock-tables --skip-disable-keys --skip-extended-insert --order-by-primary --no-tablespaces --set-gtid-purged=OFF --no-create-info "$MYSQL_DATABASE"' > (Join-Path $out ($Prefix+'-data.sql'))
 if($LASTEXITCODE){throw 'QA data snapshot failed'}
 & docker exec $state.dbContainer sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --skip-comments --skip-add-locks --skip-lock-tables --no-tablespaces --set-gtid-purged=OFF --no-data "$MYSQL_DATABASE"' > (Join-Path $out ($Prefix+'-schema.sql'))
 if($LASTEXITCODE){throw 'QA schema snapshot failed'}
}
try{
 Set-QaReadOnly $true
 $start=[DateTime]::UtcNow.ToString('o')
 $null=Invoke-Qa '/health' 200
 $null=Invoke-Qa '/h2-console/' 404 -ErrorCode 'E404'
 $null=Invoke-Qa '/h2-console/login.do' 404 -ErrorCode 'E404'
 $null=Invoke-Qa '/actuator/health' 404 -ErrorCode 'E404'
 $null=Invoke-Qa '/docs/does-not-exist' 404 -ErrorCode 'E404'
 $null=Invoke-Qa '/api/v1/users/me' 401
 $null=Invoke-Qa '/api/v1/users/me' 401 -Headers @{Authorization='Bearer invalid.jwt.value'}
 $null=Invoke-Qa '/api/v1/does-not-exist' 401
 $cors=Invoke-Qa '/api/v1/users/me' 200 'OPTIONS' @{Origin=$values.CORS_ALLOWED_ORIGINS;'Access-Control-Request-Method'='GET'}
 if($cors.headers -notmatch ('Access-Control-Allow-Origin:\s*'+[regex]::Escape($values.CORS_ALLOWED_ORIGINS))){throw 'CORS allowed origin not returned'}
 $null=Invoke-Qa '/api/v1/users/me' 403 'OPTIONS' @{Origin='https://untrusted.invalid';'Access-Control-Request-Method'='GET'}
 # Seed only this disposable clone via real APIs. Never update the source QA volume.
 Set-QaReadOnly $false
 try{
  $email='arm64-'+[guid]::NewGuid().ToString('N')+'@example.test'
  $password='qa-'+[guid]::NewGuid().ToString('N').Substring(0,14)
  $signup=Invoke-Qa '/api/v1/users/signup' 200 'POST' -Body (@{email=$email;password=$password;name='ARM64 QA'}|ConvertTo-Json -Compress)
  if(($signup.body|ConvertFrom-Json).result -ne 'SUCCESS'){throw 'QA signup failed'}
  $login=Invoke-Qa '/api/v1/users/login' 200 'POST' -Body (@{email=$email;password=$password}|ConvertTo-Json -Compress)
  $accessToken=($login.body|ConvertFrom-Json).data.accessToken
  if(-not $accessToken){throw 'QA login did not return a token'}
 }finally{Set-QaReadOnly $true}
 Snapshot-Qa 'fixture-before'
 $auth=@{Authorization='Bearer '+$accessToken}
 $null=Invoke-Qa '/api/v1/users/me' 200 -Headers $auth
 $null=Invoke-Qa '/api/v1/does-not-exist' 404 -Headers $auth -ErrorCode 'E404'
 $null=Invoke-Qa '/api/v1/admin/dashboard/summary' 403 -Headers $auth
 $null=Invoke-Qa '/api/v1/notifications/stream' 401
 $health=& docker exec $api curl -fsS http://127.0.0.1:8081/actuator/health
 if($LASTEXITCODE -or ($health|ConvertFrom-Json).status -ne 'UP'){throw 'Management health not UP'}
 $inspect=(& docker inspect $api|ConvertFrom-Json)[0]
 if($inspect.HostConfig.PortBindings.'8081/tcp'){throw 'Management port is published'}
 foreach($ip in @($inspect.NetworkSettings.Networks.PSObject.Properties.Value.IPAddress|Where-Object {$_})){
  & docker exec $api curl --noproxy '*' -fsS --max-time 5 ("http://"+$ip+":8081/actuator/health") 2> (Join-Path $out 'management-refused.txt')
  if($LASTEXITCODE -ne 7){throw 'Management endpoint was reachable on a non-loopback address'}
 }
 & docker exec $api curl --silent --show-error --no-buffer --max-time 15 --header ('Authorization: Bearer '+$accessToken) --header 'Accept: text/event-stream' --output /tmp/calfit-arm64-sse.txt http://127.0.0.1:8080/api/v1/notifications/stream 2> (Join-Path $out 'sse-client-timeout.txt')
 if($LASTEXITCODE -ne 28){throw 'SSE stream did not remain open until expected client timeout'}
 $stream=(& docker exec $api cat /tmp/calfit-arm64-sse.txt)|Out-String
 if($stream -notmatch 'event:connected' -or $stream -notmatch 'event:heartbeat'){throw 'SSE connected/heartbeat event missing'}
 Snapshot-Qa 'fixture-after'
 $dataSame=(Get-FileHash (Join-Path $out 'fixture-before-data.sql') -Algorithm SHA256).Hash -eq
           (Get-FileHash (Join-Path $out 'fixture-after-data.sql') -Algorithm SHA256).Hash
 $schemaSame=(Get-FileHash (Join-Path $out 'fixture-before-schema.sql') -Algorithm SHA256).Hash -eq
             (Get-FileHash (Join-Path $out 'fixture-after-schema.sql') -Algorithm SHA256).Hash
 if(-not $dataSame -or -not $schemaSame){throw 'QA fixture schema/data changed during read-only regression'}
 # AUTO_INCREMENT next counters reflect the new QA fixture, not schema migration.
 $originalSchema=[IO.File]::ReadAllText((Join-Path $out 'qa-schema-before.sql')) -replace '\s+AUTO_INCREMENT=\d+',''
 $finalSchema=[IO.File]::ReadAllText((Join-Path $out 'fixture-after-schema.sql')) -replace '\s+AUTO_INCREMENT=\d+',''
 if($originalSchema -ne $finalSchema){throw 'Original QA schema shape changed'}
 $originalRows=@([IO.File]::ReadAllLines((Join-Path $out 'qa-data-before.sql'))|Where-Object {$_.StartsWith('INSERT INTO ')})
 $finalRows=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
 foreach($row in [IO.File]::ReadAllLines((Join-Path $out 'fixture-after-data.sql'))){if($row.StartsWith('INSERT INTO ')){$null=$finalRows.Add($row)}}
 if(@($originalRows|Where-Object {-not $finalRows.Contains($_)}).Count){throw 'An original QA row changed or disappeared'}
 & docker logs --since $start $api *> (Join-Path $out 'qa-http-runtime.log')
 $logs=[IO.File]::ReadAllText((Join-Path $out 'qa-http-runtime.log'))
 if($logs -match 'NoHandlerFoundException|NoResourceFoundException|ERROR.*No endpoint'){throw 'Missing endpoint generated an ERROR/stack trace'}
 $results|ConvertTo-Json -Depth 5|Set-Content (Join-Path $out 'http-results.json')
 [ordered]@{passed=$true;httpChecks=$results.Count;sseConnected=$true;sseHeartbeat=$true;managementLoopbackOnly=$true;
 originalSchemaShapePreserved=$true;originalRowsPreserved=$true;fixtureDataPreserved=$dataSame;fixtureSchemaPreserved=$schemaSame;
 originalQaRows=[regex]::Matches([IO.File]::ReadAllText((Join-Path $out 'qa-data-before.sql')),'(?m)^INSERT INTO ').Count;
 fixtureRows=[regex]::Matches([IO.File]::ReadAllText((Join-Path $out 'fixture-before-data.sql')),'(?m)^INSERT INTO ').Count;
 profile='prod';jpa='validate';externalNetwork='internal Docker networks only';fixtureCreation='One disposable user added only to the clone via application signup/login'}|
 ConvertTo-Json|Set-Content (Join-Path $out 'qa-result.json')
 Write-Output 'ARM64 prod QA passed: HTTP/JWT/CORS/404/H2/SSE/management health and schema/data preservation.'
}finally{
 Set-QaReadOnly $true
 & docker exec $api rm -f /tmp/calfit-arm64-http-body /tmp/calfit-arm64-http-headers /tmp/calfit-arm64-sse.txt 2>$null
}

