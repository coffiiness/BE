# Gradle 최종 Quality Gate

2026-11-27 기준으로 SonarCloud 없이 Gradle 자체에서 배포 전 품질 기준을 강제한다.
기존 255건은 검토 근거를 가진 개별 Baseline으로 추적하며, 신규 경고를 자동 수용하지 않는다.
전체 미필터링 보고서 생성과 품질 판정은 분리되어 있다.

## 필수 기준과 실행 순서

| 검사 | 기준 |
| --- | --- |
| JUnit | 실패 0; 정규, REST Docs, develop Test Task 모두 실행 |
| 전체 모듈 JaCoCo | Line 75% 이상, Branch 47% 이상 |
| Spotless | 위반 0 |
| SpotBugs priority | High(1) 0, Baseline 등록 여부와 무관 |
| SpotBugs 신규 경고 | 개별 식별자 기준 0 |
| 기존 경고 악화 | priority 상승 또는 rank 숫자 감소 0 |
| 분석 무결성 | 비정상 프로세스 종료, 분석 오류, missing class, 누락 XML/HTML 모두 실패 |
| 실행 환경 | Eclipse Adoptium Temurin 17.0.21.1+2, Gradle 8.14.3 |

root `check` → `qualityCheck`에서 모든 모듈 check, 전체 Test 그룹, Spotless,
`jacocoAllModulesVerification`, `spotbugsVerification`, 정책 회귀 검사 및 보고서를 실행한다.
기존 JaCoCo 0.8.12, Spotless 7.0.2, SpotBugs 플러그인 6.5.12/엔진 4.10.5를 유지한다.
기존 모듈 INSTRUCTION 10% 기준과 db-core의 과거 개별 검증 비활성화는 변경하지 않았다.
새 전체 LINE/BRANCH 검증에는 db-core를 포함한 모든 main bytecode가 들어간다.

일반 컴파일과 의존성 라이브러리 jar는 테스트 입력으로 생성될 수 있다. 운영 `bootJar`,
`bootWar`, `assemble` 및 publication Task는 전체 Gate가 끝나야 실행된다.
`-x test`, `-x spotbugsVerification`, `-x qualityCheck` 등으로 필수 Task를 빼고
배포용 빌드를 실행하면 Task graph 검사가 실패한다. Gradle 9 업그레이드는 포함하지 않는다.

## 실행 명령

Windows에서는 JAVA_HOME을 위 Temurin 패치로 지정한다. 시스템 Java 설정을 바꿀 필요는 없다.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17.0.21.1+2'
.\gradlew.bat --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx384m -XX:ActiveProcessorCount=2' -PspotbugsMaxHeapSize=512m check qualityCheck build
```

Linux는 같은 배포판/패치를 사용하고 정상 Git LF checkout으로 실행한다.

```bash
./gradlew --no-daemon --max-workers=1 \
  '-Dorg.gradle.jvmargs=-Xmx384m -XX:ActiveProcessorCount=2' \
  -PspotbugsMaxHeapSize=512m check qualityCheck build
```

최초 라이브러리 준비 후에는 `--offline`으로 반복할 수 있다. `--continue` 또는
`continue-on-error`는 정상 Gate 명령에 필요하지 않으며, 필수 검사를 성공으로 바꾸는 설정은 없다.
운영 DB와 실제 환경변수 대신 기존 local H2 및 검증 전용 구성을 사용한다.
JaCoCo 속성은 기준을 높이는 경우에만 허용한다. 0.75/0.47보다 낮추면 설정 단계부터 실패한다.

## SpotBugs 개별 Baseline

`gradle/quality/spotbugs-baseline.json`은 실제 255개 경고 각각의 identity, priority, rank,
검토 ID, 검토·수용 주체, 날짜, 판단, 소스 근거와 후속 작업을 보관한다.
검토 내용은 [코드 검토 결과](spotbugs-review.md)와 개별 등록부에 설명한다.
이 요청에서 위임받은 Codex 검토·기술 부채 수용 기록이며, 사용자나 다른 사람이 개별 경고를
승인한 것처럼 기록하지 않았다.

identity에는 모듈/source set/유형, 엔진 instanceHash와 occurrence, 클래스, 메서드 signature,
필드 signature, 파일, 줄, 정규화한 소스 문장이 들어간다. 키를 정렬한 JSON의 SHA-256으로
비교한다. 같은 해시를 가진 여러 경고도 구분하며, 개수의 증가/감소는 허용 기준으로 쓰지 않는다.
줄 이동이나 소스 문장 변경도 보수적으로 재검토 대상으로 삼는다. 기존 경고 2개가 해결되고
다른 2개가 생겨 총개수가 같아져도 신규 경고로 실패한다.

Gradle에는 Baseline 생성·갱신 Task가 없다. 모든 검증은 파일을 읽기만 한다.
엔진/JDK 버전 변경, 중복 identity, 검토 근거 누락, High를 수용하려는 Baseline도 실패한다.
해결된 항목은 Gate 결과에서 resolvedEntries로 표시하며 신규 항목을 상쇄하지 않는다.
해결 항목은 개별 검토 후 수동으로 폐기하고, 신규 항목의 수용은 별도 변경으로 판단해야 한다.

Gradle은 검토 필드와 identity의 무결성을 검사한다. 사람의 실제 승인 여부를 인증하는
시스템은 아니므로 후속 CI에서는 Baseline 변경의 코드 리뷰/저장소 보호 규칙을 함께 적용해야 한다.

## 전체 보고서와 판정 보고서

`spotbugsMainReport`/`spotbugsTestReport`는 공식 SpotBugs CLI의 보고서 모드로 모든 경고를
기록한다. `exclude`, `excludeBugs`, 패키지/유형 필터, Baseline 필터를 전달하지 않는다.
JavaExec의 비정상 종료는 그대로 실패하고 XML/HTML 생성 및 분석 진단도 확인한다.
보고서 생성 성공만으로 품질 통과로 보지 않는다. 필수 `spotbugsVerification`에서 전체 결과와
검토된 Baseline을 비교하여 High/신규/악화 경고와 분석 오류를 판정한다.

전체 보고서만 볼 때는 `spotbugsReport`, 정적 분석 정책까지 검증할 때는
`spotbugsVerification`을 실행한다. 이 단독 명령은 전체 빌드/배포 허가를 대신하지 않는다.

- 각 모듈 `build/reports/spotbugs/spotbugsMain.xml` 및 HTML: 전체 main 경고.
- 각 모듈 `build/reports/spotbugs/spotbugsTest.xml` 및 HTML: 전체 test 경고.
- root `build/reports/quality/spotbugs-summary.json`: 전체 경고와 분석 오류, priority/rank 분리 집계.
- root `build/reports/quality/spotbugs-gate.json`: known/new/High/escalated/resolved와 판정 이유.
- root `build/reports/quality/policy-tests.json`: 정책 회귀 검증 결과.
- root `build/reports/quality/coverage-summary.json`: 전체/모듈별 커버리지.
- root `build/reports/jacoco/jacocoAllModulesReport/`: 전체 XML/HTML/CSV.
- 기존 `core/core-api/build/reports/jacoco/jacocoMergedReport/jacocoMergedReport.xml`: 과거 범위 보고서 유지.

## Java 패치와 CI 및 Docker 경계

JDK 배포판과 패치를 `gradle/quality/runtime.properties`에서 고정하며, Gradle 실행 JVM과
컴파일 toolchain을 확인한다. CI에서 다른 Java를 선택하면 Gate가 실패한다.
Windows와 Linux 검증 모두 공식 Temurin 17.0.21.1+2를 사용한다.
Windows portable JDK는 공식 SHA-256을 검증했고 시스템 JAVA_HOME은 변경하지 않았다.

공식 Windows 릴리스:
[Temurin 17.0.21.1+2](https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.21.1%2B2).
검증한 zip SHA-256은 `eafba0944f777abdc74d5aad16049c9b5a1275b6ce99e2939ba9f7f3e6f08c87`다.

Dockerfile은 digest 고정 Jammy OS와 같은 패치의 공식 CPU별 JDK/JRE 바이너리를 사용한다.
빌드 단계의 `-x test`를 제거하고 Gate 이후에만 운영 bootJar를 복사한다.
Windows에서 전송하는 Java/셸 파일은 패키지 staging에서 LF로 바꾸며 Docker builder에서도
Java LF를 맞춘다. 호스트 소스와 Baseline 파일은 바꾸지 않고 OS 줄바꿈에 의한 Spotless 차이를 방지한다.
테스트용 프로필/가짜 AWS 자격 값은 builder 단계에서만 사용하며 runtime 단계로 복사하지 않는다.
공식 registry 최종 확인에서 이 고정 패치 태그에는 linux/arm64 manifest가 없었다.
floating 17 태그는 amd64에 17.0.21.1+2, arm64에 17.0.21+9를 제공했다.
이 manifest 제약은 같은 패치의 공식 aarch64 바이너리를 SHA-256으로 검증하는 방식으로 해소했다.
기존 OS 공급 이미지의 Java는 최종 이미지에 남기지 않고 새 JRE로 교체한다.
실제 ARM64 이미지·prod QA 결과와 JAR 재사용 경계는
[ARM64 런타임 검증](../deployment/oracle-arm64-runtime.md)에 있다. Java 패치를 낮추지 않는다.
기본 Docker source build는 Gate를 유지하며, runtime-from-jar는 검증된 artifact 전용 target이다.

기존 AWS Workflow는 계속 비활성 상태다. 신규 Workflow는 작성하지 않았다.
기존 `actions/setup-java@v4`의 SemVer parser는 네 자리 패치 문자열을 그대로 처리하지 못하므로,
후속 CI에서는 고정한 공식 archive와 SHA-256으로 설치하는 방식 등을 사용하고
`verifyQualityRuntime`까지 통과시켜야 한다. 기존 AWS Workflow를 재활성화하지 않는다.
GitHub 배포 Job/GHCR 권한 연결은 후속 CI/CD 단계에서 품질 Job 성공에 의존하도록 구현해야 한다.

## 기존 코드의 남은 문제

구독 상세 테스트의 `Thread.sleep(300)` 뒤 빈 결과면 return 하는 경로는 실제 완료를 보장하지 않는다.
구독 목록 테스트도 고정 sleep을 사용한다. 이를 변경하지 않았으며, 후속 테스트 개선에서는
구독 생성 상태를 Awaitility로 기다리고 빈 결과를 실패로 단언해야 한다.
새 Gate는 JUnit 실패 0을 강제하지만 기존 테스트가 검증하지 않는 동작까지 증명하지 않는다.

경고 검토, 정상/실패 검증 결과와 변경 파일은 [최종 검증 결과](quality-gate-results.md)에,
15-5 당시 측정 원본은 [최초 분석 결과](initial-results.md)에 있다.
SonarCloud 외부 프로젝트/Secret은 유지하며 [재연동 기록](sonarcloud-legacy.md)도 보존한다.

