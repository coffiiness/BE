## 🌊 Git Flow & Convention

우리는 **GitHub Flow**를 기반으로, **Issue**를 먼저 생성하고 이를 해결하기 위해 **Branch**를 파고 **PR**을 보내는 순서로 개발합니다.

> **⚠️ 중요 규칙**
> - **`main`**: 배포 가능한 상태의 최종 코드 (직접 푸시 금지)
> - **`dev`**: 개발 중인 코드의 중심 (Base Branch)
> - **`feat/...`**: 실제 작업 브랜치 (`dev`에서 생성)

<br/>

### 1️⃣ Commit Message Convention
커밋 메시지는 **"무엇을(Type)", "어디를(Scope)", "어떻게(Subject)"** 수정했는지 명확하게 적습니다.

#### 📌 포맷 (Format)
```text
type: [scope] subject (#issue_number)

// 예시
feat: [FE] 메인 배너 슬라이더 구현 (#12)
fix:  [BE] 회원가입 중복 체크 로직 수정 (#15)
refactor: [BE] UserService 책임 분리 (#18)
chore: [common] 초기 프로젝트 구조 세팅 (#1)
```

#### 🏷 타입 (Type)
| 타입 | 설명 |
| :--- |  :--- |
| **`feat`**  | 새로운 기능 추가 |
| **`fix`**  | 버그 수정 |
| **`design`**  | CSS, UI/UX 레이아웃 변경 |
| **`refactor`**  | 코드 리팩토링 (기능 변경 없음) |
| **`chore`**  | 빌드 설정, 패키지 매니저, 라이브러리 추가 등 |
| **`docs`**  | 문서 수정 (README, Wiki 등) |
| **`style`**  | 코드 포맷팅 (세미콜론, 줄바꿈 등 로직 변경 없음) |
| **`test`**  | 테스트 코드 작성 |

#### 🎯 범위 (Scope) - *필수*
모노레포 구조이므로 작업 영역을 명시하는 것을 권장합니다.
- **`FE`**: Frontend (`frontend/` 폴더 작업)
- **`BE`**: Backend (`backend/` 폴더 작업)
- **`common`**: 공통 작업 (`root` 폴더 작업)

<br/>

### 2️⃣ Issue Creation Rules
작업을 시작하기 전, 반드시 이슈를 먼저 생성합니다.

1. **템플릿 선택:** `New Issue` 버튼을 누르고 **Feature** 또는 **Bug** 템플릿을 선택합니다.
2. **제목 작성:** 말머리를 사용하여 명확하게 작성합니다.
    - 예: `[FE] 메인 페이지 UI 구현` / `[BE] 상품 조회 API 개발`
3. **라벨(Label) 설정:**
    - 영역: `FE` 또는 `BE` (필수)
    - 타입: `Feature`, `Bug`, `Design` 등 (필수)
4. **Assignees 설정:** 담당자(본인)를 지정합니다.

<br/>

### 3️⃣ Issue Hierarchy (Parent-Child Structure)
우리는 지라(Jira)의 Epic-Task 구조를 **상위 이슈에 하위 이슈를 링크하는 방식**으로 구현합니다.

**주의:** 상위 이슈에서 'Convert to issue' 버튼을 누르면 템플릿이 적용되지 않으므로, 반드시 아래 순서를 따라주세요.

#### 1. 상위 이슈(Epic) 생성
먼저 큰 단위의 작업(Epic)을 이슈로 생성합니다. ('epic' 템플릿 사용 권장)
- **제목 예시:** `[Epic] 상품 조회`
- **본문:** 전체적인 목표와 기간 등을 적습니다.

#### 2. 하위 이슈(Task) 생성
새 탭을 열어 실제 구현할 기능들을 **Feature 템플릿**을 사용하여 생성합니다.
- 예: `[BE] 상품 조회 컨트롤러 생성 (#12)`
- 예: `[FE] 상세 상품 조회 화면 구현 (#13)`

#### 3. 링크 연결 (Tracking)
다시 **상위 이슈**로 돌아와 본문에 하위 이슈 번호를 **체크리스트 문법**으로 작성합니다.
이렇게 하면 상위 이슈에서 전체 진행률(Progress)을 한눈에 볼 수 있습니다.

```markdown
## 🎯 하위 작업 목록 (Tasks)
- [x] #12  <-- 이슈 번호만 적으면 제목이 자동 완성됩니다.
- [ ] #13
- [ ] #14
```

<br/>

### 4️⃣  Branch & PR Rules
이슈가 생성되면 해당 이슈 번호를 기반으로 브랜치를 생성합니다.

#### 🌱 Branch Naming
> `타입/이슈번호-설명`
- **`feat/issue-12-main-banner`**
- **`fix/issue-15-login-error`**

#### 🚀 Pull Request (PR) Process
1. **템플릿 작성:** PR 생성 시 나타나는 템플릿 양식을 채워주세요.
2. **PR 제목:** : 제목은 `type(scope) : 내용 (#issue_number)`로 통일하여 작업 성격이 한 눈에 보이게끔 합니다.
    - 예: `feat(FE): 메인 배너 슬라이더 구현 (#12)`
3. **이슈 연결:** 내용(Description)에 `Closes #이슈번호`를 반드시 적어서, 머지될 때 이슈가 자동으로 닫히도록 합니다.
    - 예: `Closes #12`
4. **리뷰 요청:** Reviewers에 팀원을 지정하고 코드 리뷰를 요청합니다.
5. **Merge:** 최소 1명 이상의 승인(Approve)을 받은 후 `Merge Commit`을 진행합니다.
