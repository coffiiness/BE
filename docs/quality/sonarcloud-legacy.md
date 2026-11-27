# SonarCloud 설정 보관과 재연동

Calfit은 외부 계정 없이 실행하는 Gradle 품질 검증을 기본으로 사용한다. SonarCloud 플러그인,
프로젝트 속성, `sonar` Task 의존성과 기존 Workflow의 Sonar Job은 로컬 소스에서 제거했다.
외부 SonarCloud 프로젝트와 GitHub Secret은 변경하거나 삭제하지 않았다.

## 이전 설정

| 항목 | 기존 값 |
| --- | --- |
| Gradle 플러그인 | org.sonarqube 6.0.1.5171 |
| 프로젝트 키 | coffiiness_BE |
| 조직 | coffiiness |
| 서버 | https://sonarcloud.io |
| XML 입력 | core/core-api/build/reports/jacoco/jacocoMergedReport/jacocoMergedReport.xml |
| Task 의존성 | sonar → :core:core-api:jacocoMergedReport |
| Workflow 조건 | dev Push, needs: build |
| 인증 변수 | SONAR_TOKEN, GITHUB_TOKEN — 값은 보관하지 않음 |

Gradle 속성과 과거의 분석/커버리지 제외 목록은
[원본 Gradle 설정](sonarcloud-gradle-original.gradle.txt)에, 제거한 Job 전체는
[원본 Workflow Job](sonarcloud-workflow-original.yml.txt)에 있다. `.txt` 파일이므로 실행 설정으로
적용되지 않는다. 이 제외 목록을 새 JaCoCo 전체 보고서나 SpotBugs에 복사하지 않는다.

기존 Workflow에는 main 테스트 생략과 AWS 배포 단계가 남아 있다. Sonar Job 제거가 새 CI/CD
구축을 의미하지 않으며 기존 Workflow를 재활성화해서는 안 된다. 신규 Workflow는 후속 단계다.

## 재연동 절차

1. 별도 변경 브랜치에서 Gradle 8.14.3/Java 17 및 당시 Sonar Scanner 요구사항에 맞는 플러그인
   버전을 검토한다. 위 버전은 원본 기록이며 새로 권장하는 최신 버전이라는 뜻은 아니다.
2. root `plugins`에 `org.sonarqube`를 추가하고 버전을 명시한다. 로컬 품질 Task는 그대로 유지한다.
   선택적 도입을 원하면 `apply false`와 명시적인 opt-in 적용을 사용해 기본 build와 분리한다.
3. 보관한 Gradle 속성과 Task 의존성을 복원한다. 기존 프로젝트/조직과 실제 Sonar 메인 브랜치를
   확인하고, 분석 범위는 별도로 검토한다. 과거 제외 목록을 자동으로 승인하지 않는다.
4. 실행 환경의 SONAR_TOKEN을 사용한다. 값 출력이나 파일 커밋 없이 존재·권한만 확인한다.
   프로젝트 Execute Analysis 권한과 조직/브랜치/플랜 조건을 확인한다.
5. 로컬 검증을 먼저 통과시킨 뒤 `:core:core-api:jacocoMergedReport`와 `sonar`를 실행한다.
   배포 차단에 사용할 경우 `-Dsonar.qualitygate.wait=true`로 서버 처리 결과까지 기다린다.
6. 신규 CI 설계에서 Sonar Job을 선택적으로 추가한다. 원본 AWS Workflow 전체를 복원하거나
   인증 실패를 성공으로 처리하지 않는다. 토큰 실패와 코드 품질 실패를 구분한다.

기존 Sonar 보고서는 신규 코드와 과거 제외 범위를 사용하므로, 전체 bytecode를 측정하는
새 JaCoCo 보고서의 전체 Line/Branch 수치와 직접 비교하지 않는다.

공식 문서: [Gradle Scanner](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/scanners/sonarscanner-for-gradle.md),
[인증 문제 점검](https://docs.sonarsource.com/sonarqube-cloud/appendices/troubleshooting).
