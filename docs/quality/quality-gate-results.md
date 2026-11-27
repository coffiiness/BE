# 최종 Quality Gate 검증 결과

아래 ARM64 이미지 보류는 15-6 당시 기록이다. 이후 같은 Java 패치의 공식 aarch64 바이너리로
실제 이미지 빌드와 격리 QA 검증을 완료한 [ARM64 런타임 결과](../deployment/oracle-arm64-runtime.md)를 따른다.

2026-11-27, integration/oracle-cicd HEAD `7f54ba77`에서 수행한 결과다.
최종 `check qualityCheck build`는 Windows/Linux 모두 성공했다.
기존 255건은 개별 검토·수용한 기술 부채로 표시되고, 신규/High/악화 경고는 0건이다.
운영 소스와 기존 테스트는 유지했고 Commit, Push, 원격 Merge 또는 운영 배포는 하지 않았다.

## 확정 기준과 실제 결과

| 항목 | 확정 기준 | Windows | Linux |
| --- | --- | --- | --- |
| JUnit | 실패 0 | 성공 585 / 기존 비활성화 12 / 실패 0 | 성공 585 / 기존 비활성화 12 / 실패 0 |
| 전체 Line | 75% 이상 | 7002/9277 = 75.47699% | 7002/9277 = 75.47699% |
| 전체 Branch | 47% 이상 | 1304/2726 = 47.83566% | 1304/2726 = 47.83566% |
| Spotless | 위반 0 | 통과 | 통과 |
| SpotBugs 전체 | 전체 공개 | 255 | 255 |
| SpotBugs 신규 / High / 악화 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| 분석 오류 / missing class / 보고서 누락 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| 전체 Gate 및 Gradle build | 모두 통과 | 성공 | 성공 |
| 정책 회귀 검사 | 모두 통과 | 14개 | 14개 |

Temurin **17.0.21.1+2**와 Gradle **8.14.3**을 두 OS에서 사용했다. Gradle JVM뿐 아니라
각 compiler/test/analyzer launcher의 runtime version도 검사했다.
전체 테스트를 실제 실행한 빌드는 Windows 7분 54초, Linux 9분 41초에 성공했다.
마지막 launcher 검사 강화 후 동일한 Gate/build 재확인은 각각 35초/38초에 성공했으며,
변경되지 않은 테스트 결과는 Gradle up-to-date 판정을 사용했다.

이번 실행의 전체 Line/Branch와 Instruction 29668/39651 = 74.82283%는 두 OS에서 같다.
이것이 기존 비동기 테스트의 시점 변동까지 해결했다는 의미는 아니다.
모든 27개 모듈과 모든 테스트 그룹의 측정 범위는 유지했다. 모듈별 수치는 아래 표에 있다.

## 실제 실패 유도 검증

| 검증 | 실제 결과 |
| --- | --- |
| 임시 null 역참조 코드 추가 | 총 257, 신규 2, High 1로 spotbugsVerification 실패 |
| 기존 Google DTO 경고 2건 해소 + 다른 신규 경고 2건 | 총 255 유지, known 253/new 2/resolved 2/High 0으로 실패 |
| 미실행 bytecode를 검증 범위에 임시 추가 | Line ratio 0.64 < 0.75, Branch 0.40 < 0.47로 실패 |
| 운영 bootJar에서 -x test | Task graph 검증 실패 |
| 운영 bootJar에서 -x qualityCheck | Task graph 검증 실패 |
| Line 기준을 0.74로 낮추기 | 설정 단계에서 실패 |

커버리지 실패는 기준을 올려서 만든 것이 아니라, 임시 미실행 클래스를 검증 범위에 추가하여
실제로 커버리지를 낮췄다. 원래 범위를 줄이지 않았다. 검증 후 class/source/init 파일을 제거했고
정상 범위로 다시 전체 Gate/build를 통과시켰다.

정책 회귀 14개에는 같은 해시/동일 개수에서 위치가 바뀐 신규 경고, 알려진 identity의 High,
rank 악화, 중복 identity, 검토 누락, High의 Baseline 등록 시도, 엔진 변경, 분석 오류,
missing class의 XML attribute/node 형태 등이 포함된다.

## 원상 복구와 Baseline 무결성

임시 High/교체 경고의 Java 소스와 생성 class를 제거했다. 변경했던 Google DTO는 원본 바이트로
복원하고 SHA-256 일치를 확인했다. 기존 Java/resource 파일의 HEAD 대비 diff는 없다.
최종 정상 보고서에는 검증용 Probe가 없고 Baseline은 최초 검토 후의 파일 SHA-256과 같다.
일반 Gradle 실행에 Baseline 생성·갱신 Task는 없다.

Linux는 기존 격리 컨테이너/캐시를 재사용하고 network none으로 실행했다.
Windows는 검증용 Java hosts와 local H2 및 가짜 AWS 자격 값으로 실행했다.
실제 운영 환경변수/Secret, Oracle 서버/DB, Cloudflare, AWS 자원, QA 볼륨은 변경하지 않았다.
저장공간 확보를 위한 Docker 데이터 삭제나 새 중복 검증 이미지 생성도 하지 않았다.

## 코드 검토 결론

- PnlService의 미사용 두 변수는 중복 조회/계산이며, 실제 비용은 월별 카테고리 합계로 계산한다.
  실제 컴파일된 서비스에 매출 10000, 비용 100/200/300/400, 미사용 추이 값 999999를 넣어
  비용 1000/영업이익 9000을 확인했다. 계산 결과 누락은 확인하지 못했다.
- JWT 생성자의 키 검증 실패는 의도한 fail-fast다. non-final 클래스의 일반 finalizer 경고를
  별도 보안 경계 검토로 남기며 키 검증을 제거하거나 예외를 숨기지 않았다.
- rank 13 null 경고 8건은 테스트 코드다. 반복 nullable getter, Excel body 단언 및 SSE cleanup의
  보강 대상으로 남기고 운영 결함 8개로 세지 않는다.
- EI 212건은 DTO record 100, 도메인/이벤트/projection record 94, 설정 record 4, 테스트 record 10,
  JPA JSON 이력 2, Spring DI 1, List.of 기반 enum 1로 구분했다.
- JPA Map aliasing, Locale 의존 변환, 테스트 신뢰성은 후속 개선 대상이다.
  특히 구독 상세 테스트의 300ms sleep 뒤 빈 결과면 return 하는 기존 경로는 실제 비동기 완료를
  검증하지 못한다. 이를 은폐하거나 테스트 성공을 완료 보장으로 설명하지 않는다.

상세 근거는 [코드 검토](spotbugs-review.md), 255개의 검토 ID와 위치는
[개별 등록부](spotbugs-review-register.md), 전체 identity/수용 기록은
`gradle/quality/spotbugs-baseline.json`에 있다.
기록의 주체는 사용자 요청에 따른 Codex 위임 검토이며 사람의 개별 승인으로 위장하지 않았다.

## 이번 단계 변경 파일

| 파일 | 변경 |
| --- | --- |
| build.gradle | SpotBugs base plugin으로 엔진을 공급하고 보고서/정책 판정을 분리 |
| quality.gradle | 기본 75%/47% Gate, 전체 검사 연결, 운영 packaging/publishing 및 제외 우회 차단 |
| gradle/quality/runtime.properties | Java vendor/정확한 patch와 Gradle 버전 고정 |
| gradle/quality/runtime.gradle | Gradle JVM/compiler/test/analyzer 런처 버전 확인 |
| gradle/quality/spotbugs.gradle | 모든 main/test 미필터링 XML/HTML, 개별 Baseline 정책 Task |
| gradle/quality/policy.groovy | 개별 fingerprint, High/new/escalation/분석 무결성 검사 |
| gradle/quality/policy-tests.gradle | 정책 회귀 14개 |
| gradle/quality/spotbugs-baseline.json | 검토 근거를 가진 255개 고정 항목 |
| Dockerfile | JDK/JRE patch+digest 고정, 테스트 생략 제거, Gate 전 운영 bootJar 방지, Java LF 처리 |
| tools/deployment/package-oracle.ps1 | 필수 품질 파일 포함, Groovy credential 검사, 전송용 Java/셸 LF 처리 |
| docs/quality/gradle-quality.md | 최종 정책, 명령, 보고서, CI/Docker 경계 |
| docs/quality/initial-results.md | 15-5 측정 이력임을 명시하고 현재 정책으로 연결 |
| docs/quality/spotbugs-review.md | 유형별 실제 코드 검토와 후속 문제 |
| docs/quality/spotbugs-review-register.md | 255개 개별 검토 ID/위치/판정 |
| docs/quality/quality-gate-results.md | 최종 정상·실패 검증 및 변경 범위 |

이전 15-5의 Sonar 제거 변경과 원본 보관 문서는 그대로 남아 있다.
이번 단계에서 신규 CI/CD Workflow를 만들거나 활성화하지 않았고 SonarCloud 외부 프로젝트/Secret도 유지했다.
기존 main/dev/FE와 원격은 수정하지 않았다.

## 실행 및 배포 경계

```powershell
# JAVA_HOME은 Eclipse Temurin 17.0.21.1+2
.\gradlew.bat --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx384m -XX:ActiveProcessorCount=2' -PspotbugsMaxHeapSize=512m check qualityCheck build
```

이 명령에 기준 속성이나 Baseline 갱신 옵션이 필요하지 않다.
배포용 bootJar와 Dockerfile의 builder는 이 Gate에 의존한다.
실제 운영 Docker 이미지 빌드·발행·Push·배포는 실행하지 않았다.
향후 GitHub Workflow의 GHCR 발행/배포 Job도 품질 Job 성공에 의존하도록 연결해야 하며,
현재의 로컬 강제 정책이 아직 작성하지 않은 원격 Workflow나 운영 서버의 동작까지 대신하지는 않는다.

**ARM64 운영 이미지 발행은 보류 상태다.** 공식 registry의 고정
17.0.21.1_2-jdk-jammy/JRE 태그에는 linux/arm64가 없었다. floating 17-jammy는 amd64
17.0.21.1+2와 arm64 17.0.21+9를 혼합하고 있어 대체하지 않았다. 현재 고정 digest로는
ARM64 Docker 빌드가 실패한다. 같은 Java 패치의 ARM64 지원 베이스를 확인하고 digest를
별도 검증한 후에만 운영 이미지 발행을 진행해야 한다. Windows/Linux Gradle 검증 성공과
운영 ARM64 이미지 준비 완료를 구분한다. 기존 Oracle 운영 서비스는 변경하지 않았다.

운영 방법과 고정 Java 설치 주의사항은 [최종 Quality Gate](gradle-quality.md)에 있다.
후속 작업은 기술 부채 개별 개선, 비동기 테스트 완료 단언 보강, 새로운 CI/CD 배선이다.

## 전체 모듈 커버리지


| 모듈 | Line Windows | Line Linux | Branch Windows | Branch Linux |
| --- | ---: | ---: | ---: | ---: |
| :clients | N/A | N/A | N/A | N/A |
| :clients:google-calendar | 0.99% | 0.99% | 0.00% | 0.00% |
| :core | N/A | N/A | N/A | N/A |
| :core:core-api | 72.10% | 72.10% | 50.16% | 50.16% |
| :core:core-enum | 93.14% | 93.14% | N/A | N/A |
| :core:domain-announcement-board | 96.23% | 96.23% | 53.12% | 53.12% |
| :core:domain-applicant | 97.37% | 97.37% | 70.00% | 70.00% |
| :core:domain-application | 52.17% | 52.17% | 37.10% | 37.10% |
| :core:domain-billing | 82.73% | 82.73% | 46.43% | 46.43% |
| :core:domain-calendar | 79.19% | 79.19% | 52.71% | 52.71% |
| :core:domain-career | 100.00% | 100.00% | N/A | N/A |
| :core:domain-interview | 83.92% | 83.92% | 51.90% | 51.90% |
| :core:domain-meeting-room | 87.19% | 87.19% | 52.98% | 52.98% |
| :core:domain-notification | 73.71% | 73.71% | 43.28% | 43.28% |
| :core:domain-payment | 49.84% | 49.84% | 6.82% | 6.82% |
| :core:domain-recruitment | 94.34% | 94.34% | 66.26% | 66.26% |
| :core:domain-user | 79.61% | 79.61% | 60.00% | 60.00% |
| :core:domain-workspace | 86.52% | 86.52% | 61.76% | 61.76% |
| :storage | N/A | N/A | N/A | N/A |
| :storage:db-core | 78.93% | 78.93% | 31.82% | 31.82% |
| :support | N/A | N/A | N/A | N/A |
| :support:email | 51.58% | 51.58% | 29.41% | 29.41% |
| :support:error | 79.73% | 79.73% | 25.00% | 25.00% |
| :support:event | 100.00% | 100.00% | N/A | N/A |
| :support:logging | N/A | N/A | N/A | N/A |
| :support:monitoring | N/A | N/A | N/A | N/A |
| :support:security | 97.44% | 97.44% | 68.75% | 68.75% |
