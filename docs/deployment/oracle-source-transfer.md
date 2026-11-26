# Windows → Oracle 소스 전송 (배포 실행 전 준비)

대상은 Ubuntu ARM64 서버의 **신규 MySQL 8.4.13 시연 환경**이다. 기존 AWS DB/볼륨은
옮기지 않는다. 아래 SSH/SCP/압축 해제 명령은 사용자가 전송을 결정한 뒤 실행할 예시이며,
이번 작업에서는 로컬 패키지 생성·검사만 수행한다. 원격 접속/배포/Push는 실행하지 않는다.

## 1. 전송할 소스만 로컬 패키징

Windows PowerShell에서 BE 디렉터리로 이동한다.

```powershell
Set-Location 'C:\Users\guddn\dev\swcamp-Sideproject\05_final_project\BE'
$bundle = & .\tools\deployment\package-oracle.ps1
$bundle | Format-List
Get-Content -LiteralPath $bundle.Manifest
tar -tzf $bundle.Archive
Get-FileHash -LiteralPath $bundle.Archive -Algorithm SHA256
```

스크립트는 로컬 파일을 읽고 `.gradle-user-codex/oracle-transfer`에 압축본·SHA-256·파일
목록·staging 복사본을 만든다. 기존 파일/볼륨을 삭제하지 않고 Git commit/Push도 하지 않는다.

- Git 추적 파일과 ignore되지 않은 새 파일의 **현재 worktree 내용**을 사용한다.
- `git archive HEAD`처럼 미커밋 Dockerfile/신규 Compose를 빠뜨리지 않는다.
- BE 빌드 입력과 Oracle 배포 설정·가이드만 allowlist로 복사한다. FE는 Pages에서 별도 배포한다.
- .git, .env 실값, application-local, SSH/개인키, DB 백업, 빌드·Gradle 캐시를 제외한다.
- `.env.prod.example`만 템플릿으로 포함하고 해당 파일의 secret 필드가 비어 있는지 검사한다.
- symlink/junction은 따라가지 않고, 명백한 내장 개인키/AWS access key 패턴이 발견되면 중단한다.
- 필요한 classpath SQL(shedlock.sql 등)은 포함한다. DB dump는 포함하지 않는다.

파일명 필터/패턴 검사만으로 소스의 모든 민감정보를 판별할 수 있는 것은 아니다. 파일 목록과
변경 diff를 확인한 뒤 전송한다. 이미 추적된 비밀 파일은 .gitignore만으로 추적 해제되지 않는다.
[git ls-files 기준](https://git-scm.com/docs/git-ls-files)

## 2. SSH 호스트 확인 및 SCP 전송

NSG의 관리자 공인 IP가 현재 PC의 공인 IP와 일치해야 한다. SSH 22 외 인바운드 포트는
추가하지 않는다. 개인키는 PC에 보관하며 전송 압축본에 넣지 않는다.

기존에 신뢰한 SSH 호스트키가 known_hosts에 등록되어 있어야 아래 StrictHostKeyChecking=yes가
통과한다. 처음 접속하거나 호스트키가 달라졌다면 Oracle 콘솔/신뢰하는 기존 세션에서 지문을
확인한 뒤 등록한다. 검증을 우회하거나 known_hosts를 무조건 삭제하지 않는다.
예: 신뢰하는 서버 세션에서 `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`로 지문 확인.

```powershell
$OracleHost = 'YOUR_ORACLE_PUBLIC_IP'
$OracleKey = "$env:USERPROFILE\.ssh\calfit-oracle.key"

# 위 변수만 실제 값으로 변경한다. 이 명령들은 전송을 결정한 뒤 실행한다.
ssh -i $OracleKey -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes "ubuntu@$OracleHost" `
  'install -d -m 700 /home/ubuntu/calfit-upload'
scp -i $OracleKey -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes `
  $bundle.Archive $bundle.ChecksumFile "ubuntu@${OracleHost}:/home/ubuntu/calfit-upload/"
```

SCP는 SSH 인증/암호화를 사용한다. 실제 .env.prod, 개인키, DB dump를 별도로 함께 복사하지 않는다.
[OpenSSH SCP](https://man.openbsd.org/scp),
[호스트키 확인 옵션](https://man.openbsd.org/ssh_config#StrictHostKeyChecking)

## 3. 서버에서 해시 확인 후 새 디렉터리에만 해제

다음은 Oracle SSH 터미널에서 실행할 예시다. archive 이름은 `$bundle.Archive`의 파일명으로
바꾼다. 기존 서비스 디렉터리에 덮어쓰거나 DB 볼륨 경로에 압축을 풀지 않는다.

```bash
cd /home/ubuntu/calfit-upload
archive='calfit-oracle-source-실제파일명.tar.gz'
sha256sum -c "$archive.sha256"
tar -tzf "$archive"

# checksum이 OK이고 파일 목록이 예상대로일 때만 새 경로에 해제한다.
release="/home/ubuntu/calfit-releases/${archive%.tar.gz}"
if [ -e "$release" ]; then
  echo 'Release directory already exists; choose a new path.'
  exit 1
fi
install -d -m 700 "$release"
tar --no-same-owner --no-same-permissions -xzf "$archive" -C "$release"
cd "$release/BE"
cp -n .env.prod.example .env.prod
chmod 600 .env.prod
```

실제 비밀번호·S3 인증 정보·JWT/Google/Toss/SMTP 값은 서버의 .env.prod에 별도로 입력한다.
MYSQL_VOLUME_NAME=calfit_oracle_mysql84와 JPA_DDL_AUTO=validate를 확인한다. 최초의 빈 DB에만
일회성 update로 초기화하는 절차는 [Oracle 배포 가이드](oracle.md)를 따른다.

압축 해제까지는 컨테이너 실행이 아니다. 기존 AWS workflow는 여전히 main Push에 반응하므로
소스 전송을 위해 GitHub Push를 우회적으로 실행하지 않는다. 전송/압축 해제를 자동 배포와
연결하는 작업은 별도 승인 후 진행한다.
