# 단일 ACTIVE 워크스페이스 정책

## 작업 기준과 변경 범위

BE 기준은 `origin/dev`의 `9e872c0868424184307df8bf51074f30db691bcc`, FE 기준은
`d01b7f197a1a7e08b5beae05399f860729aa325c`다. 두 저장소 모두
`feat/single-active-workspace` 별도 worktree에서 작업했다. 확인 당시 각각의
`origin/main`(`b3dfeb67…`, `e4d570f7…`)과 dev의 파일 트리는 같고 merge 이력만 달랐다.
원래 BE worktree의 기존 미추적 파일은 보존했다. 운영 서버와 DB에는 접속하지
않았으므로 실제 실행 이미지의 revision/digest를 확인했다고 주장하지 않는다.

| 저장소 | 작업 HEAD / origin/dev | origin/main | main 전용 / dev 전용 커밋 | 파일 트리 차이 |
| --- | --- | --- | --- | --- |
| BE | `9e872c0868424184307df8bf51074f30db691bcc` | `b3dfeb67ebbeac799bfb47e3b75c8a22a41d0daa` | 2 / 0 | 없음 |
| FE | `d01b7f197a1a7e08b5beae05399f860729aa325c` | `e4d570f7409fc62e11f3a821800fe273310df5cf` | 1 / 0 | 없음 |

원래 BE는 dev, 원래 FE는 main에 두고 별도 feature worktree에서만 수정했다.
이전 분석 전용 SHA와 실제 서버 이미지 revision을 별도 확인할 수 있는 자료는 없으므로
그 둘의 일치를 단정하지 않는다. 위 비교는 fetch한 원격 브랜치와 로컬 소스를 기준으로 한다.
전체 변경 파일 목록은 [single-active-workspace-files.txt](single-active-workspace-files.txt)에 있다.

기업별 Hibernate discriminator 멀티테넌시는 유지한다. 사용자별 ACTIVE 멤버십은
전역으로 최대 하나이며, DELETED 이력 개수는 제한하지 않는다. 탈퇴 후에는 같은
기업 또는 다른 기업에 다시 가입할 수 있다. 마지막 HR은 탈퇴·제거할 수 없고,
역할을 먼저 낮춰 이 보호를 우회할 수도 없다. 역할 변경도 같은 기업 잠금 아래 검증한다.

| 경로 | 이전 | 변경 후 |
| --- | --- | --- |
| 워크스페이스 생성 | 생성과 HR 등록이 분리됨 | 새 tenant를 Session 생성 전에 설정하고 하나의 트랜잭션에서 사용자 잠금·전역 소속 재검증·생성·HR 등록 |
| 초대 수락 | 기존 소속과 경합에 취약 | 사용자 행 잠금 후 전역 ACTIVE 조회, 초대 행 잠금 후 상태·만료·이메일·목적지·역할 재검증 |
| 공통 멤버 저장 | 직접 호출로 중복 등록 가능 | 동일 정책을 다시 적용하고 존재하는 ACTIVE 워크스페이스만 허용 |
| 사용자 소속 조회 | `LIMIT 1` 임의 선택 | 최대 두 행 조회로 0/1/복수 구분, 복수면 명시적 409 |
| 일반 회원 테넌트 | 헤더를 신뢰 | 유일한 실제 소속과 헤더·URL workspaceId 비교 후 TenantContext 설정 |
| Calendar OAuth | FE 임시 정보에 의존 | 서버 발급 256-bit state의 해시, 사용자·workspace·redirect·5분 만료를 DB에 결합, 행 잠금과 일회성 소비 |
| FE HTTP 400 | 소속 재조회 후 임의 재전송 | 자동 재전송 제거, 각 화면에서 오류 표시 |
| 캘린더 표시/해제 | 전역 localStorage 표시 | 서버 연결 상태 조회, 서버 DELETE로 논리 중지 및 watch 정리 |

`WorkspaceCreatedEvent`는 실제 생성 트랜잭션 커밋 후 기존 비동기 Trial 생성기를
호출한다. HR 등록 실패 시 워크스페이스와 이벤트 후속 처리가 함께 취소된다.
가입 트랜잭션에는 READ COMMITTED를 사용한다. MySQL 기본 REPEATABLE READ에서
존재하지 않는 서로 다른 active_user_id를 잠근 뒤 동시 삽입하면 gap lock 교착이
발생하는 것을 QA에서 재현했기 때문이다. 사용자 행 잠금과 UNIQUE 인덱스는 유지하며
생성·초대 수락·공통 멤버 등록 이외의 트랜잭션 격리 수준은 바꾸지 않는다.

핵심 변경 전후는 다음과 같다.

```java
// 이전: 각 서비스 호출이 별도 트랜잭션이므로 HR 저장 실패 시 기업이 남을 수 있었다.
Workspace workspace = workspaceService.createWorkspace(name, contactPhone, employeeScale);
TenantContext.setTenantId(workspace.workspaceId());
memberService.registerMember(userId, MemberType.HR);
```

```java
// 변경 후: TenantContext는 트랜잭션/Session보다 먼저 지정한다.
TenantContext.setTenantId(workspaceId);
transactions.execute(status -> {
  membershipPolicy.lockAndRequireUnassigned(userId);
  Workspace workspace = workspaceService.createWorkspace(workspaceId, name, contactPhone, employeeScale);
  memberStore.save(userId, MemberType.HR); // 공통 정책 재검증 + ACTIVE 기업 확인
  return workspace;
});
```

예전 `SELECT tenant_id … LIMIT 1`은 임의 선택이었지만, 이제 `LIMIT 2`로 중복 여부를
검출한 뒤 1개인 경우에만 반환한다. 이는 두 워크스페이스를 지원하기 위한 조회가 아니다.

## 오류와 API 호환성

기존 가입·생성 API 경로와 정상 응답은 유지한다. 중복 정책 위반은 `E409`와
`WORKSPACE_ALREADY_JOINED`, `ACTIVE_WORKSPACE_EXISTS`,
`MULTIPLE_ACTIVE_MEMBERSHIPS`로 구분한다. 잘못된 테넌트는
`TENANT_MISMATCH`(403), 무소속 데이터 접근은 `ACTIVE_MEMBERSHIP_REQUIRED`(403)다.
새 UNIQUE 인덱스 위반만 해당 가입 충돌로 변환하며 다른 DB 오류를 숨기지 않는다.

관리자·공개 채용·지원자 인증·공개 초대 조회·Google Webhook의 기존 인증 경로는
유지한다. 초대 수락 목적지는 검증된 토큰에서 얻고 트랜잭션 전에 설정한다.
일반 회원의 유일 소속은 헤더가 없어도 서버에서 결정할 수 있지만, 중복 데이터는
임의 선택하지 않는다. 기존 중복 계정은 로그인/소속 조회에서도 차단될 수 있으므로
배포 전에 아래 데이터 정리가 필요하다.
무소속 사용자의 SSE 연결·heartbeat는 `default` namespace로 유지한다. 요청 헤더를
이 namespace 선택에 사용하지 않으며, 기업별 알림 목록 조회는 소속 검증을 요구한다.

Calendar 연결은 다음 순서로 FE/BE를 함께 반영한다.

1. `POST /api/v1/calendars/google/authorization`, body `{ "redirectUri": "…/auth/callback" }`.
2. 응답 `authorizationUrl`, `state`, `expiresInSeconds` 사용. FE는 탭의 sessionStorage에
   시작 사용자와 소속을 함께 저장하고 Google URL로 이동한다.
3. Callback은 로그인 사용자·소속·state·만료를 확인하고 기존 connect API에
   `{ "authCode": "…", "redirectUri": "…", "state": "…" }`를 한 번 전송한다.
4. BE는 별도 트랜잭션으로 state를 소비·커밋한 뒤 실제 코드 교환을 수행한다.
   upstream 실패 후에도 같은 state는 재사용할 수 없으며 새 인증을 시작해야 한다.
5. `GET /api/v1/calendars/google/status`의 `connected`로 표시한다.
   `DELETE /api/v1/calendars/google/connect`는 해당 사용자·소속 연결을 중지한다.

`GOOGLE_CALENDAR_ALLOWED_REDIRECT_URIS`는 정확한 URI의 쉼표 목록이다.
prod 기본값은 `https://calfit.co.kr/auth/callback,https://www.calfit.co.kr/auth/callback`,
local은 localhost 5173/3000이다. Google Console의 허용 URI도 동일해야 한다.
임의 redirect URI나 클라이언트 제공 workspaceId로 서버 소속을 바꿀 수 없다.
서버는 raw state와 인증 코드를 로그에 남기지 않는다.
FE의 소속 조회 중 진행 중인 요청도 access token별로 분리한다. 이전 계정의 지연
응답은 로그인 계정이 바뀐 이후 localStorage의 workspaceId를 덮어쓸 수 없다.

## DB 적용: 운영에서는 수동 검토 후에만

스키마 자동 적용을 추가하지 않았다. 기존 운영 Compose의 JPA `validate`를 유지한다.
코드 배포 전에 별도 SQL을 적용해야 새 `active_user_id`와 OAuth state 테이블 검증이
성공한다. `validate` 자체는 생성식·UNIQUE 인덱스를 검증하지 않으므로 메타데이터
확인도 반드시 수행한다.

- `deploy/oracle/sql/single-active-workspace-precheck.sql`: 읽기 전용 중복·고아 소속·마지막 HR·의심 캘린더 확인.
- `deploy/oracle/sql/single-active-workspace-apply.sql`: 중복 감지 시 `SIGNAL`로 중단하고
  `active_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN status='ACTIVE' THEN user_id ELSE NULL END) VIRTUAL`
  및 전역 UNIQUE와 state 테이블 추가. ACTIVE는 user_id를, DELETED는 NULL을 갖는다.
- `deploy/oracle/sql/single-active-workspace-rollback.sql`: 승인된 코드 롤백 후에만
  UNIQUE와 생성 컬럼을 제거하는 선택 절차. 이력과 state 테이블은 삭제하지 않는다.

DDL은 implicit commit이므로 트랜잭션 ROLLBACK으로 복원되지 않는다.
쓰기 중지, 검증된 백업, 사전 SQL 검토 후 명시적으로 선택한 DB에서 실행한다.
`mysql --force`를 사용하지 않는다. 적용 SQL은 자동 재실행용이 아니며 실패/부분 적용 시
남은 프로시저·컬럼·인덱스를 점검한 후 재시도를 검토한다. 가상 컬럼 추가와 인덱스
생성의 실제 소요 시간 및 잠금 영향은 운영 크기의 별도 복제본에서 확인한다.
앱 코드만 롤백할 때는 이 추가 제약을 그대로 남기는 편이 중복 재발을 막는다.
제약 자체를 제거하면 중복 가입이 다시 가능하므로 가입 쓰기를 통제해야 한다.

## 기존 중복 데이터와 잘못 연결된 Calendar 정리

이 작업은 기존 운영 데이터를 자동 수정하지 않는다. 정리 대상 목록에는 사용자 ID,
멤버십 ID·역할·상태·tenant, 캘린더 행 ID·tenant·채널 메타데이터만 사용한다.
토큰과 실제 인증정보를 보고서나 터미널에 출력하지 않는다.

1. 생성·가입·탈퇴와 해당 사용자 캘린더 동기화 작업을 일시 통제하고 DB 백업과
   멤버십/캘린더/일정 매핑의 복구 스냅샷을 접근 제한된 위치에 보관한다.
2. 계정 소유자와 기업 관리자가 유지할 워크스페이스와 정확한 멤버십 ID를 승인한다.
   최근 행, 낮은 ID, localStorage 값만으로 정하지 않는다. 다른 워크스페이스의
   마지막 HR이라면 대체 HR을 먼저 지정하거나 별도로 기업 종료 절차를 승인한다.
3. 관련 사용자·기업·멤버 행을 잠근 한 트랜잭션에서 승인된 추가 멤버십만
   `status='DELETED'`로 전환한다. 사용자 또는 이력 행을 DELETE하지 않는다.
4. 잘못 연결된 Calendar는 **먼저** 해당 연결의 `is_sync_enabled=false`로 바꾸고
   관련 미사용 OAuth state를 소비 처리한다. tenant_id만 변경하여 토큰·캘린더·일정
   매핑을 다른 기업으로 옮기지 않는다. 중복 상태 때문에 일반 API가 차단될 때는
   승인된 행 ID로 수행하는 별도 운영 정리 작업이 필요하다.
5. DB 논리 중지 커밋 후 저장된 channelId/resourceId와 해당 연결의 적법한 자격으로
   `channels.stop`을 호출한다. 실패·토큰 만료 시 채널 메타데이터를 보존하고 수동
   재시도하거나 채널 만료를 확인한다. Google 캘린더/이벤트를 자동 삭제하거나
   계정 전체 토큰을 일괄 revoke하지 않는다.
6. 서버의 유일 소속을 다시 검증하고 새 state로 올바른 기업에 연결한다. 기존
   Calendar의 이름/description 마커 및 일정 매핑은 소유자와 검토한다. 공유 중인
   기업 캘린더를 다른 사용자의 연결까지 포함하여 삭제하지 않는다.
7. 사전 점검이 0건이고 마지막 HR/이력/일정 보존이 확인되면 제약 SQL을 적용한다.

일반 탈퇴·관리자 멤버 제거·연결 해제는 동일 트랜잭션에서 연결을 논리 중지하고
pending state를 무효화한다. watch 중지는 AFTER_COMMIT 이벤트에서 수행한다.
실패하더라도 비활성 연결은 Scheduler·Webhook·비동기 일정 이벤트 및 토큰 갱신에서
사용되지 않는다. 동기화와 탈퇴는 사용자 행 잠금으로 직렬화한다.
프로세스 중단/네트워크 실패 후 watch 중지 재처리를 위한 영속 outbox는 이번 범위에
추가하지 않았으며, 메타데이터로 확인하고 운영 재시도해야 한다.

## 배포 순서와 확인

1. 실제 배포 revision을 별도로 확인하고 새 브랜치 변경과 대조한다.
2. 백업·쓰기 통제·기존 중복/잘못된 연결의 승인된 정리.
3. DB 사전 점검 → 스키마 적용 → 생성식·UNIQUE·DELETED 이력 및 데이터 수 확인.
4. **BE 먼저** 배포하여 `prod/validate` 기동과 기존 Health/JWT/CORS/404를 확인한다.
5. **FE 다음** 배포하여 로그인, 무소속 생성, 초대 충돌, 새 state 기반 연동을 확인한다.
   중간에는 기존 FE의 state 없는 Google 연결이 거절되므로 짧은 연동 점검 시간을 둔다.
   보안을 위해 legacy state 없는 연결을 허용하는 호환 우회는 넣지 않는다.
6. 마지막 HR/재가입/DELETED 이력, 잘못된 헤더와 URL의 403, 신규 중복의 409,
   같은 OAuth state의 재사용 거절 및 로그의 인증정보 미노출을 확인한다.

## 로컬 검증 명령

Java/Gradle 정책은 기존 Temurin `17.0.21.1+2` / Gradle `8.14.3` 그대로다.
실제 `.env`를 로드하지 않는 격리된 작업 디렉터리 및 합성 자격을 사용한다.

```bash
./gradlew check qualityCheck :core:core-api:bootJar
# 신규 통합 테스트: 기본 격리 H2, Google/메일은 Mock
./gradlew :core:core-api:test --tests '*SingleActiveWorkspaceTest' \
  --tests '*TenantInterceptorTest' --tests '*GoogleCalendarOAuthStateTest'
```

MySQL 통합 테스트는 `CALFIT_QA_MYSQL_URL`이 loopback의 전용
`calfit_membership_qa` DB일 때만 허용한다. 이 DB의 root 비밀번호는 테스트 코드에
명시된 **합성 QA 전용 값**이며 실제 환경변수/Secret과 무관하다. 임시 컨테이너는
`/var/lib/mysql`을 tmpfs로 사용하고 기존 volume을 마운트하지 않는다.
`CALFIT_QA_SCHEMA_MODE=validate`로 재기동 시 스키마 자동 변경 없이 검증할 수 있다.
수동 SQL 검증 도구는 고정 QA 이름/라벨/tmpfs를 확인한 후 **새 schema-only 복사본**에만
마이그레이션을 실행하며, 원래 QA DB와 기존 Docker 데이터는 변경하지 않는다.

다음은 **새 로컬 QA 전용** 예시다. 같은 이름의 컨테이너/DB가 이미 있다면 먼저
확인하고 중단한다. 기존 컨테이너를 지우거나 기존 volume을 붙이지 않는다.

```bash
docker run -d --name calfit-membership-qa-20261128 \
  --label calfit.qa=single-active-workspace \
  --tmpfs /var/lib/mysql:rw,size=768m --memory 768m \
  -p 127.0.0.1:33070:3306 \
  -e MYSQL_ROOT_PASSWORD=synthetic-membership-root \
  -e MYSQL_DATABASE=calfit_membership_qa mysql:8.4.13
# mysqladmin ping으로 준비 상태 확인 후에만 테스트 실행
export CALFIT_QA_MYSQL_URL='jdbc:mysql://127.0.0.1:33070/calfit_membership_qa?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
./gradlew :core:core-api:test --tests '*SingleActiveWorkspaceTest'
# 빈 Docker CLI 설정 디렉터리 경로를 명시한다. 새 QA 스키마 복사본에 SQL 검증.
mkdir -p /tmp/calfit-empty-docker-config
python tools/deployment/test_single_workspace_schema.py \
  --container calfit-membership-qa-20261128 --docker-config /tmp/calfit-empty-docker-config
export CALFIT_QA_MYSQL_URL='jdbc:mysql://127.0.0.1:33070/calfit_membership_qa_migration?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export CALFIT_QA_SCHEMA_MODE=validate
./gradlew :core:core-api:test --tests '*SingleActiveWorkspaceTest'
unset CALFIT_QA_MYSQL_URL CALFIT_QA_SCHEMA_MODE
```

실제 검증에서는 local profile과 합성 JWT/AWS/Google 설정, 이메일 비활성화를
명시하고 자동 Google Scheduler의 초기 실행을 지연했다. 기존 테스트는 비활성화하지
않았다. Test의 workingDir를 새 빈 `…/runtime/a/b`로 지정하는 임시 Gradle init
script를 `--init-script`로 전달해 application.yml의 상대 경로 `.env` import가
실제 환경 파일을 찾지 못하도록 했다. 위 명령을 기존 개발 쉘에서 그대로 실행하기
전에 같은 격리를 준비해야 한다. MySQL 테스트의 기본 `update`는 이름 검증을 통과한
새 QA DB 최초 bootstrap에만 사용하고, 이후에는 `validate`로 실행한다.
tmpfs 데이터는 컨테이너 중지 시 사라지므로 실제 데이터 보존용으로 사용하지 않는다.

기존 API 테스트 fixture도 새 정책에 맞췄다. 두 기업의 지원자 로그인 격리 테스트는
서로 다른 소유자 계정을 사용한다. 빈 그룹 조회는 실제 소속 기업에서 검증하고,
무소속 사용자의 임의 기업 멤버 목록 조회는 403을 검증한다. 파일 API 테스트는
ACTIVE 소속과 리소스의 기업을 일치시킨 후 기존 인증/응답 검증을 수행한다.
Calendar fixture는 서버 authorization API에서 받은 state로 기존 연결 시나리오를
실행한다. 기존 테스트를 삭제하거나 비활성화하지 않았다.
공지·회의실의 비HR 권한 테스트는 마지막 HR을 강등시키는 fixture 대신 별도의
INTERVIEWER 멤버를 초대해 실행한다. 비HR의 생성·수정·삭제 차단 검증은 그대로다.

FE는 Node 22에서 기존 명령과 추가 Node 내장 회귀 테스트를 실행한다. 신규 도구나
패키지, Workflow는 추가하지 않는다.

```bash
npm ci
node --experimental-vm-modules --test tools/test-workspace-policy.mjs
VITE_API_BASE_URL=https://api.calfit.co.kr/api/v1 npm run build
```

## 후속 보안 과제

OAuth access/refresh token의 저장 암호화 및 키 회전, ID Token 서명·issuer·audience·nonce
검증, PKCE 적용 여부, state 만료 행의 보존 기간/정리 정책, watch 중지의 영속 재처리와
토큰 폐기 정책은 별도 검토 대상으로 남긴다. 이번 수정으로 ID Token 검증이나 기존
토큰 암호화까지 완료했다고 간주해서는 안 된다.

공식 기준: [Google OAuth state](https://developers.google.com/identity/protocols/oauth2/web-server#state-param),
[Google watch 중지](https://developers.google.com/workspace/calendar/api/guides/push#stop-notifications),
[MySQL 생성 컬럼](https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html).

## 실제 실행 결과

2026-11-28 로컬 검증 완료. BE는 Temurin `17.0.21.1+2` / Gradle `8.14.3`,
FE는 Node `22.23.5`를 사용했다.

| 검사 | 최종 결과 |
| --- | --- |
| 기존 CI 명령 | `check qualityCheck :core:core-api:bootJar` BUILD SUCCESSFUL, 11분 37초 |
| 전체 JUnit | 총 638개: 성공 626, 기존 비활성화 12, 실패/오류 0 |
| JaCoCo Line | 76.6538% (7,335 / 9,569), 기존 75% 기준 통과 |
| JaCoCo Branch | 50.0694% (1,443 / 2,882), 기존 47% 기준 통과 |
| Spotless | 전체 필수 검사 통과 |
| SpotBugs | 기존 255, 신규 0, High 0, 악화 0, 분석 오류/누락 0 |
| MySQL 신규 회귀 | 8.4.13 + 수동 SQL 적용 스키마 + JPA validate에서 29개 통과 |
| 공지·회의실 권한 회귀 | 기존 80개 관련 테스트 통과; 비HR 차단 검증 유지 |
| DB SQL | 중복 사전 차단, 적용, 사전 점검 구문, 별도 QA 복사본 롤백 통과 |
| 데이터 보존 | ACTIVE 중복 0, DELETED 생성 컬럼 오염 0, 검증용 이력 2건과 전체 3행 보존 |
| Compose | 합성 env로 config 검증 통과; validate/내부 MySQL/외부 volume/Health Check 유지 |
| FE | Node 내장 회귀 12개 성공, npm ci 및 운영 API URL Vite build 성공 |
| Whitespace | BE/FE git diff --check 통과 |

bootJar는 `core/core-api/build/libs/core-api-0.0.1-SNAPSHOT.jar`에 생성됐다
(119,358,144 bytes). Docker 이미지 빌드·발행·운영 배포는 하지 않았다.
기존 테스트를 새로 비활성화하거나 품질 기준, 분석 범위, SpotBugs Baseline을
변경하지 않았다. 초기 회귀에서 발견한 새 정책과 충돌하는 fixture는 위에 설명한
방법으로 수정했으며 최종 전체 실행에서는 실패가 없다.

검증 자료:

- `build/reports/quality/coverage-summary.json`, `spotbugs-gate.json`
- `core/core-api/build/reports/tests/test/index.html` 및 각 모듈 JUnit XML
- `.gradle-user-codex/single-workspace-validation/ci-quality-final.log`
- 같은 폴더의 `mysql-and-static-final.log`, `mysql-schema.log`,
  `mysql-preservation-final.log`, `fe-policy-tests.log`, `fe-build.log`

로컬 QA 컨테이너 `calfit-membership-qa-20261128`은 데이터 확인을 위해 유지했다.
호스트 loopback 포트 9568을 사용하며 `/var/lib/mysql`은 tmpfs다. 기존 volume이나
bind mount는 연결하지 않았다. 이 컨테이너의 데이터는 실제 운영 데이터가 아니고,
중지하면 tmpfs 데이터가 사라진다. 기존 Docker 이미지·volume은 삭제하지 않았다.

신규 회귀의 검증 범위는 다음과 같다. 실제 Google 인증·일정 변경과 운영 DB 접근은 하지 않는다.

| 요구 시나리오 | 검증 수단 |
| --- | --- |
| 무소속 생성, 추가 생성·동일/다른 기업 가입 차단 | `SingleActiveWorkspaceTest`: 공통 저장 경로 포함 |
| 기업 생성·HR 원자성, 롤백 시 Trial 미생성 | 실제 DB 트랜잭션 + 멤버 저장 실패 주입 + Trial port Mock |
| 복수 초대/동일 초대 동시 수락, 생성과 수락 경쟁 | 실제 MySQL 사용자 잠금 + 동시 실행 + 최종 ACTIVE/초대 상태 확인 |
| 서로 다른 사용자 동시 생성 | MySQL gap lock 교착 재현 후 READ COMMITTED 회귀 |
| 초대 만료·처리 상태·이메일·목적지 불일치 | 잠긴 초대를 사용하는 트랜잭션 내부 재검증 |
| UNIQUE, 탈퇴 후 재가입, 복수 DELETED 이력 | 직접 중복 쓰기 거절과 재가입 후 생성 컬럼/이력 확인 |
| 마지막 HR 탈퇴·제거·강등 우회 | 같은 기업 잠금 아래 역할 변경/탈퇴/제거 거절 |
| 기존 중복 소속 임의 선택 금지 | QA 전용 제약 일시 해제 후 합성 중복 구성, 409 검증, 이력 보존 후 제약 복원 |
| 헤더/URL tenant 위조, 무소속 접근 | `TenantInterceptorTest`, 가입 서비스의 목적지 검증 |
| OAuth 사용자·기업·redirect·TTL·일회성·동시 소비 | `GoogleCalendarOAuthStateTest` + 실제 DB state 행 잠금 |
| 코드 교환 실패 후 state 재사용 | 별도 소비 트랜잭션 커밋 후 Mock upstream 오류 주입 |
| Calendar의 tenant와 실제 Hibernate Session 일치 | Mock Google 호출 지점에서 실제 Session tenant 및 DB tenant 확인 |
| 탈퇴 후 동기화·Webhook·토큰 사용 차단 | 비활성 DB 연결 조회, Mock Google 호출 0건, 토큰 서비스 거절 |
| watch 중지 성공/실패 | 논리 중지 후 Mock API 실패 시 메타데이터 유지, 성공 시 채널 필드 정리 |
| Scheduler·비동기 경로 | 기존 enabled 조회 조건 및 공통 소속 재검증·사용자 잠금·tenant 설정 순서 검토; 기존 전체 회귀 포함 |
| FE 생성·초대 충돌, 400 재전송 금지 | Node 내장 테스트로 실제 Axios interceptor/화면 script 실행 |
| FE OAuth 콜백·계정/기업 변경·캐시·지연 응답 | state별 1회 교환, 콜백 전후 컨텍스트 검증, 계정별 진행 중 조회 분리 |
| 기존 JWT·CORS·404·SSE·로컬 H2 | 기존 EndpointSecurity 및 전체 API/모듈 회귀 |

FE 검증은 기존 CI의 `npm ci`/Vite build와 위 로직 회귀다. 실제 브라우저 전체 E2E나
실제 Google OAuth 성공까지 확인한 결과는 아니다. 운영 전에는 허용된 도메인의
redirect URI와 새 연결/해제 화면을 별도로 점검해야 한다.
