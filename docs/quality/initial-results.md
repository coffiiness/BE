# Gradle 품질 검증 최초 결과

이 문서는 15-5 최초 측정의 이력이다. 현재 적용되는 고정 임계값과 개별 Baseline 정책은
[최종 Quality Gate](gradle-quality.md)와 [후속 코드 검토](spotbugs-review.md)를 따른다.
아래의 전체 경고 실패 상태와 opt-in 임계값 제안은 당시 상태이며, 현재 설정을 의미하지 않는다.

측정일: 2026-11-27. 기준 HEAD: integration/oracle-cicd `7f54ba77`와 이 worktree의
커밋하지 않은 품질 설정 변경. Java 소스, 기존 테스트, 운영 환경변수, DB 스키마는 변경하지 않았다.
SonarCloud 프로젝트/Secret, 기존 원격 Workflow 상태, main/dev/FE, 운영 서버는 변경하지 않았다.

## 검증 환경과 결과

Windows Java 17.0.3와 격리된 Ubuntu Linux Temurin 17.0.21.1에서 Gradle 8.14.3을 사용했다.
기존 local H2 및 검증 전용 설정을 사용했다. Linux 테스트 실행은 Docker network none으로
외부 네트워크를 차단했고, Windows Java 실행은 검증 전용 hosts 파일로 외부 이름 해석을 막았다.
라이브러리 준비 단계만 네트워크를 사용했고 최종 검증은 Gradle offline 모드였다.

최종 플랫폼 대조 결과는 아래 측정 표를 따른다. 기존 `@Disabled` 12개는 그대로이며
신규 비활성화/삭제는 없다. 정규 테스트와 REST Docs, develop 그룹 Task를 모두 요청했다.
실행 대상 develop 태그 테스트가 없는 경우 NO-SOURCE/빈 실행은 테스트 생략 설정이 아니다.

`build`와 `qualityCheck`는 SpotBugs 경고를 실패로 처리한다. 전체 테스트 통과와 전체 빌드
성공을 혼동하면 안 된다. `bootJar` 생성 및 Spotless는 정상이며, 현재 CI를 통과시킬 수 있는
품질 상태라고 보고하지 않는다. 최초 분석을 위해 `--continue`로 전체 모듈과 결과를 수집했다.

## 전체 커버리지

Windows Line은 **7,018 / 9,277 = 75.64946%**, Linux는 **7,002 / 9,277 = 75.47699%**다.
Branch는 두 환경 모두 **1,304 / 2,726 = 47.83566%**다. Instruction은 Windows
29,727 / 39,644 = 74.98487%, Linux 29,668 / 39,651 = 74.82283%다. 모든 모듈의 main bytecode와 모든 테스트 그룹의
실행 데이터를 합쳤다. DTO, entity, 설정, clients/support/storage를 제외하지 않았다.

Line 차이는 core-api -3, billing -9, db-core -5, security +1로 합계 -16줄이다.
결제 이벤트 handler가 `@Async`인 점과 관련 서비스/저장 경로의 실행 차이를 확인했으며,
비동기 처리 시점이 일부 차이의 원인일 가능성이 있다. 실패한 테스트나 분석 대상 제외는 없다.
별도로 javap 비교에서 ExternalCalendar record의 toString에 최신 JDK가 추가
String.valueOf 명령을 생성하는 차이도 확인했다. Instruction 분모는 calendar +1,
interview +4, meeting-room +2다. 후속 CI에서는 JDK 배포판/패치 버전을 고정하고
비동기 이벤트 테스트의 완료 대기 조건을 검토해야 한다. 두 OS 수치를 억지로 같게 만들지 않았다.

기존 통합 보고서의 XML 경로와 core/storage 범위는 유지했다. 기존 보고서는 비교용으로
존속하며, 새 전체 측정의 분모를 대신하지 않는다. 실행 데이터는 원래 정규 test.exec 범위를
명시적인 Test Task provider로 연결해 Gradle의 암묵적 의존성 오류를 해결했다.

모듈별 Line/Branch와 정적 분석 수치는 아래 측정 표에 모두 기록한다. 분모가 0인 카운터는
N/A이며 0%나 100%로 환산하지 않는다. 전체 비율은 모듈 퍼센트의 단순 평균이 아니다.

## JaCoCo 기준 제안

초기 회귀 방지 기준은 전체 **Line 75%, Branch 47%**를 제안한다. 더 낮은 Linux 실측에서 각각 0.48/0.84
퍼센트포인트 아래의 정수 기준으로, 무작위 80% 목표와 구분한다. 기존 모듈 INSTRUCTION 10%
검증과 db-core의 기존 검증 비활성화는 바꾸지 않았다. 새 전체 측정에는 db-core도 포함한다.

새 `jacocoAllModulesVerification`은 두 속성을 명시해야 하며, 아직 기본 check에 고정 임계값으로
연결하지 않았다. 최종 CI 기준 결정 후 CI 명령에 추가하는 단계가 필요하다.

```bash
./gradlew jacocoAllModulesVerification \
  -PqualityMinLineCoverage=0.75 -PqualityMinBranchCoverage=0.47
```

두 OS 모두 위 기준이 실제 통과했다. Windows에서는 Line/Branch 100%를 의도적으로 요구하면
실패하는 것도 확인했다. PowerShell에서는 소수점을 포함한 속성 전체를 인용한다.

```powershell
.\gradlew.bat jacocoAllModulesVerification '-PqualityMinLineCoverage=0.75' '-PqualityMinBranchCoverage=0.47'
```

기준만 낮춰 두는 것으로 끝내지 않는다. google-calendar Line 0.99%/Branch 0%,
payment 49.84%/6.82%, email 51.58%/29.41%부터 성공/실패/경계 조건 테스트를 보강하는 것이
우선이다. 다른 모듈의 높은 Line 수치로 가려진 분기 공백도 별도 추적한다. 모듈별 임계값은
해당 모듈 테스트 계획을 확인한 후 정하고, 관측 수치보다 높은 값을 일괄 강제하지 않는다.

## SpotBugs 해석과 우선 검토 대상

최종 완전 분석은 **255건: main 223, test 32**다. High priority 0건, Normal 228건,
Low 27건이다. rank 1..9는 0건, 10..14는 8건, 15..20은 247건이다.
rank와 priority는 다른 척도이며, 실제 서비스 장애/취약점의 확정 건수를 뜻하지 않는다.

- rank 13의 `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` 8건은 모두 core-api 테스트다.
  일정/SSE 및 지원서 상세·엑셀 테스트에서 nullable 결과에 대한 assertion/계약을 우선 확인한다.
- `EI_EXPOSE_REP` 105건과 `EI_EXPOSE_REP2` 107건은 컬렉션/객체의 변경 가능성 노출이다.
  record DTO와 Spring 주입 객체가 포함되므로 객체의 소유권과 불변성 계약을 개별 검토한다.
  도구 범주명 `MALICIOUS_CODE`는 악성 코드 212개가 있다는 의미가 아니다.
- `PnlService.getReport`의 line 69/99 dead store는 계산 결과 누락인지 불필요 대입인지 확인한다.
  나머지 dead store 2건은 테스트 fixture 관련이다.
- 문자열 대소문자 변환 8건은 Locale에 따른 enum/외부 응답 파싱 차이를 검토한다.
- `JwtTokenProvider` 생성자의 `CT_CONSTRUCTOR_THROW` 1건(rank 16)은
  finalizer 공격 전제/상속 가능성과 키 검증 실패 계약을 검토한다. 이번 변경에서 보안 로직을
  임의 수정하거나 경고를 억제하지 않았다.
- 알려진 null 값 로드 3건은 Google 일정 동기화 테스트에서 null 상황을 의도적으로 만드는지
  확인한다. 포괄적인 throws/catch, 네이밍, 미사용 private 메서드 등도 전부 원본 보고서에 남겼다.

모든 유형/범주/모듈별 수와 개별 위치는 아래 표와 JSON/XML/HTML 보고서에 있다.
baseline, exclude filter, detector 제외 또는 `ignoreFailures=true`는 없다.

## CI 실패 조건 제안

현재 구현은 **모든 SpotBugs 경고에서 실패**하므로 255건을 남겨 둔 채 CI를 성공으로 만들지 않는다.
향후 CI의 필수 조건은 테스트 실패 0, Spotless 통과, 분석기 오류/누락 클래스/누락 보고서 0,
합의한 JaCoCo Line/Branch 기준 통과다.

정적 분석은 먼저 rank 13의 테스트 null 경고와 main의 가변 객체 노출 계약을 검토한다.
단계적 gate를 선택하더라도 rank 1..14 신규 발견, 실제 결함으로 판정한 경고, 새 경고 증가는
실패로 처리하고 기존 255건은 전체 산출물과 검토 기록으로 계속 노출하는 방식이 적절하다.
이 경우에도 전체 경고를 지우는 baseline을 만들지 말고, 예외가 필요하면 위치·판정 근거·담당자·
만료 조건을 개별 승인해야 한다. 이번 단계에서는 그러한 완화나 예외를 구현하지 않았다.
SonarCloud와 달리 SpotBugs는 bytecode 결함 분석이며 secret 탐지/모든 보안 취약점 분석을
대체하지 않는다.

## 해결한 검증 도구 문제와 남은 작업

Spring BOM이 분석 전용 commons-lang3를 3.17.0으로 내리는 충돌을 확인해 `spotbugs` 구성에만
3.21.0을 적용했다. Checker Framework 누락 annotation은 분석 전용 checker-qual 3.37.0으로
보완했다. 애플리케이션 런타임 의존성은 유지한다. 분석 실행 전 자신의 기존 XML/HTML을
제거하고 분석 오류와 missing class를 실패로 처리하여 중단된 실행이 과거 보고서를 재사용하지 않는다.
중간 실패 실행에서 관측된 186건은 최종 완전 분석 결과로 사용하지 않는다.

남은 작업은 경고 개별 판정과 별도 기능 변경, 취약한 모듈의 테스트 보강, CI 임계값 확정,
후속 신규 CI/CD Workflow 구축이다. 기존 Workflow에는 AWS 단계와 테스트 생략 설정이 남아 있어
재활성화하면 안 된다. Gradle 9 비호환 deprecation 경고도 후속 업그레이드 시 별도로 처리한다.
현재 버전은 8.14.3을 유지한다. 이번에는 실제 Oracle/ARM64 운영 배포를 다시 실행하지 않았다.

검증은 단일 worker, Gradle heap 256~384MB, SpotBugs heap 512MB로 수행하고 기존 Gradle 캐시와
검증 Docker 이미지를 재사용했다. QA/운영 볼륨, 기존 Docker 데이터는 삭제하지 않았다.
실행 명령과 원본 SonarCloud 재연동 절차는 [운영 방법](gradle-quality.md)과
[설정 보관 문서](sonarcloud-legacy.md)를 참고한다.

## 최종 측정 표

| 항목 | Windows | Linux |
| --- | --- | --- |
| 테스트 성공 / 기존 비활성화 / 실패 | 585 / 12 / 0 | 585 / 12 / 0 |
| Spotless | 통과 | 통과 |
| bootJar 생성 | 통과 | 통과 |
| build / qualityCheck | 실패 — SpotBugs 경고 | 실패 — SpotBugs 경고 |
| SpotBugs 경고 | 255 | 255 |
| 분석 오류 / missing class / missing report | 0 / 0 / 0 | 0 / 0 / 0 |
| Line | 7018/9277 = 75.64946% | 7002/9277 = 75.47699% |
| Branch | 1304/2726 = 47.83566% | 1304/2726 = 47.83566% |
| Instruction | 29727/39644 = 74.98487% | 29668/39651 = 74.82283% |

### 전체 모듈 커버리지와 SpotBugs

| 모듈 | Line Windows | Line Linux | Branch (두 OS 동일) | SpotBugs main | test |
| --- | ---: | ---: | ---: | ---: | ---: |
| :clients | N/A | N/A | N/A | 0 | 0 |
| :clients:google-calendar | 0.99% | 0.99% | 0.00% | 7 | 0 |
| :core | N/A | N/A | N/A | 0 | 0 |
| :core:core-api | 72.24% | 72.10% | 50.16% | 24 | 32 |
| :core:core-enum | 93.14% | 93.14% | N/A | 1 | 0 |
| :core:domain-announcement-board | 96.23% | 96.23% | 53.12% | 0 | 0 |
| :core:domain-applicant | 97.37% | 97.37% | 70.00% | 0 | 0 |
| :core:domain-application | 52.17% | 52.17% | 37.10% | 12 | 0 |
| :core:domain-billing | 84.54% | 82.73% | 46.43% | 42 | 0 |
| :core:domain-calendar | 79.19% | 79.19% | 52.71% | 26 | 0 |
| :core:domain-career | 100.00% | 100.00% | N/A | 0 | 0 |
| :core:domain-interview | 83.92% | 83.92% | 51.90% | 33 | 0 |
| :core:domain-meeting-room | 87.19% | 87.19% | 52.98% | 15 | 0 |
| :core:domain-notification | 73.71% | 73.71% | 43.28% | 3 | 0 |
| :core:domain-payment | 49.84% | 49.84% | 6.82% | 0 | 0 |
| :core:domain-recruitment | 94.34% | 94.34% | 66.26% | 50 | 0 |
| :core:domain-user | 79.61% | 79.61% | 60.00% | 0 | 0 |
| :core:domain-workspace | 86.52% | 86.52% | 61.76% | 0 | 0 |
| :storage | N/A | N/A | N/A | 0 | 0 |
| :storage:db-core | 79.77% | 78.93% | 31.82% | 4 | 0 |
| :support | N/A | N/A | N/A | 0 | 0 |
| :support:email | 51.58% | 51.58% | 29.41% | 2 | 0 |
| :support:error | 79.73% | 79.73% | 25.00% | 0 | 0 |
| :support:event | 100.00% | 100.00% | N/A | 0 | 0 |
| :support:logging | N/A | N/A | N/A | 0 | 0 |
| :support:monitoring | N/A | N/A | N/A | 0 | 0 |
| :support:security | 96.58% | 97.44% | 68.75% | 4 | 0 |

### SpotBugs byType

| 항목 | 건수 (Windows/Linux 동일) |
| --- | ---: |
| DM_CONVERT_CASE | 8 |
| EI_EXPOSE_REP | 105 |
| EI_EXPOSE_REP2 | 107 |
| REC_CATCH_EXCEPTION | 5 |
| BX_UNBOXING_IMMEDIATELY_REBOXED | 1 |
| NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | 8 |
| THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION | 7 |
| NP_LOAD_OF_KNOWN_NULL_VALUE | 3 |
| NM_CLASS_NAMING_CONVENTION | 2 |
| DLS_DEAD_LOCAL_STORE | 4 |
| UPM_UNCALLED_PRIVATE_METHOD | 1 |
| THROWS_METHOD_THROWS_RUNTIMEEXCEPTION | 2 |
| SIC_INNER_SHOULD_BE_STATIC_ANON | 1 |
| CT_CONSTRUCTOR_THROW | 1 |

### SpotBugs byCategory

| 항목 | 건수 (Windows/Linux 동일) |
| --- | ---: |
| I18N | 8 |
| MALICIOUS_CODE | 212 |
| STYLE | 20 |
| PERFORMANCE | 3 |
| BAD_PRACTICE | 12 |

### SpotBugs byPriority

| 항목 | 건수 (Windows/Linux 동일) |
| --- | ---: |
| 3 | 27 |
| 2 | 228 |

## 변경 파일

| 파일 | 변경 |
| --- | --- |
| build.gradle | Sonar 제거, SpotBugs 적용, 분석 전용 의존성, Spotless/check 연동 |
| gradle.properties | Sonar 버전 제거, SpotBugs 플러그인/엔진 버전 고정 |
| quality.gradle | 전체·모듈별 JaCoCo, 선택적 검증, 품질 집계와 check 연결 |
| core/core-api/build.gradle | 기존 통합 JaCoCo 실행 데이터의 producer 의존성 명시 |
| .github/workflows/ci.yml | 비활성 기존 Workflow에서 Sonar Job만 제거 |
| Dockerfile | 새 root Gradle 스크립트를 빌드 컨텍스트에 복사 |
| tools/deployment/package-oracle.ps1 | 새 Gradle 파일/품질 문서를 소스 패키지에 포함, 필수 파일 확인 |
| docs/quality/gradle-quality.md | 버전, 범위, Task 순서, 명령, 자원/보고서 안내 |
| docs/quality/initial-results.md | 최초 실측, 플랫폼 차이, 경고 분류와 CI 기준 제안 |
| docs/quality/sonarcloud-legacy.md | 기존 Sonar 설정 및 재연동 절차 |
| docs/quality/sonarcloud-gradle-original.gradle.txt | 기존 Gradle Sonar 설정 원본, 실행하지 않는 보관 파일 |
| docs/quality/sonarcloud-workflow-original.yml.txt | 제거한 Sonar Job 원본, 실행하지 않는 보관 파일 |

