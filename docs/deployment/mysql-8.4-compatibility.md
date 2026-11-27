# MySQL 8.4.13 신규 시연 DB 호환성 검토

검토일: 2026-11-26. 운영 목적은 포트폴리오·시연이며 **운영 Compose는 mysql:8.4.13으로
고정한다.** 신규 external 볼륨은 **calfit_oracle_mysql84**이다. 기존 AWS DB 데이터는
이전하지 않고 기존 8.0 볼륨/업무 코드/로컬 H2는 변경하지 않는다.

MySQL 공식 릴리스 노트는 8.0이 2026년 4월 EOL에 도달했다고 안내한다. 앞선 8.0/8.4 비교
검증을 바탕으로 신규 DB에는 8.4.13을 선택했다. 기존 데이터의 in-place 업그레이드는 하지 않는다.
[공식 EOL 안내](https://dev.mysql.com/doc/relnotes/mysql/8.0/en/)

## 저장소에서 확인한 사항

| 영역 | 현재 구성 | 8.4 판단 |
| --- | --- | --- |
| JDBC | Gradle dependencyInsight 결과 Connector/J **9.2.0**, Java 17 | 8.4.13 접속·caching_sha2_password 인증 검증 통과. 드라이버 버전 유지 |
| JPA | Hibernate **6.6.18.Final** MySQLDialect, IDENTITY, JSON/TEXT 컬럼 | 신규 스키마 생성·validate 사전 검증 통과. 비즈니스 로직 유지 |
| 인증 | 기존 Compose는 default-authentication-plugin=caching_sha2_password | **해당 서버 옵션은 8.4에서 제거됨. 기존 Compose의 image만 8.4로 바꾸면 시작 실패 위험** |
| 신규 사용자 | 빈 8.4 DB에서 MYSQL_USER로 생성 | caching_sha2_password 사용. 기존 AWS 사용자는 이전하지 않음 |
| 문자셋 | utf8mb4 / utf8mb4_unicode_ci | 지원됨. 기존 collation을 유지해 정렬·unique 비교 변화 최소화 |
| 데이터 볼륨 | 신규 calfit_oracle_mysql84 | 기존 8.0 datadir 재사용 없음 |
| 프로필 | local H2 / prod MySQL | H2 테스트 통과만으로 8.4 호환 판정 불가 |

8.4는 caching_sha2_password를 기본 사용하며 mysql_native_password는 기본 비활성화다.
Oracle 전용 Compose에서는 불필요한 default-authentication-plugin 옵션을 생략했다.
새 8.4 사용자도 기본 caching_sha2_password를 사용한다. 기존 AWS 계정/비밀번호/데이터는
가져오지 않는다. [8.4 변경 사항](https://dev.mysql.com/doc/refman/8.4/en/mysql-nutshell.html),
[제거 옵션 목록](https://dev.mysql.com/doc/mysqld-version-reference/en/optvar-changes-8-4.html)

JDBC URL은 내부 TCP 연결에서 useSSL=false / allowPublicKeyRetrieval=true를 유지한다.
이는 caching_sha2_password의 비TLS 최초 인증에 사용된다. DB 포트는 공개하지 않으며,
서로 다른 호스트의 DB로 옮길 경우 인증서 검증 TLS로 별도 구성한다.
Connector/J의 JDBC 4.2/Java 호환성은 릴리스별로 확인해야 하며 현재 BOM의 실제 선택 버전은
9.2.0이다. 최신 Spring 3.5 문서의 관리 버전을 이 프로젝트에 그대로 적용하지 않는다.
[Connector/J 9.2 릴리스](https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-2-0.html)

현재 설정은 MySQLDialect를 명시하고 JDBC metadata 기본 사용을 끈다. 로컬 8.4 검증에서도
Hibernate 초기 로그에는 Database version: 8.0이 표시됐다. 이 로그를 실제 서버 버전으로
해석하면 안 된다. 실제 JDBC DatabaseMetaData와 SELECT VERSION()으로 8.4.13을 확인했다.
메타데이터 관련 deprecated 설정의 정리는 별도 변경으로 검토하며 이번에는 유지한다.

## SQL / 엔티티 분석 범위

native SQL은 다음 저장소에서 확인했다.

- RecruitmentRepository: tenant별 COUNT/GROUP BY, 상태 필터, LOWER/CONCAT/LIKE, ORDER BY
- MemberRepository: tenant 검색과 LIMIT 1
- ApplicationRepository / ApplicationFileRepository: ID·status 기준 tenant 검색

이 문법들은 8.4에서 사용할 수 있다. JSON은 ApplicationTemplateEntity,
ApplicationEntity, RecruitmentHistoryEntity, InterviewScheduleHistoryEntity,
MeetingRoomHistoryEntity, AutomationRuleEntity 등에서 사용한다. `shedlock.sql`은
VARCHAR PK와 TIMESTAMP(3)를 사용한다. JPQL/비관적 잠금/페이징도 실제 데이터로 확인해야 한다.

8.4의 비표준 외래키 제한, 예약어·기본값·실행 계획 변화는 실제 운영 스키마가 있어야
판정할 수 있다. 특히 비고유/복합키 일부를 참조하는 FK를 확인한다. 운영 덤프·테이블
크기·데이터 분포·사용자 plugin·sql_mode는 아직 확인하지 않았다.
[업그레이드 변경 사항](https://dev.mysql.com/doc/refman/8.4/en/upgrading-from-previous-series.html),
[서버 변수](https://dev.mysql.com/doc/refman/8.4/en/server-system-variables.html#sysvar_restrict_fk_on_non_standard_key)

## 신규 DB 초기화 절차

1. .env.prod의 JPA_DDL_AUTO는 validate로 유지하고 신규 8.4 볼륨만 명시적으로 만든다.
2. MySQL 8.4.13과 빈 업무 DB(table_count=0)를 확인한다.
3. **한 명령에만 JPA_DDL_AUTO=update**를 적용해 스키마를 생성한다.
4. 환경변수 override 없이 API를 다시 실행해 validate로 기동한다.
5. 회원가입/워크스페이스 생성 등 정상 API로 시연 데이터를 만들고 S3·메일·일정·결제를 확인한다.

명령은 [Oracle 가이드](oracle.md)에 있다. 기본 설정을 update로 남기지 않는다.

## 참고: 향후 기존 데이터 이전을 별도 요청할 때만 적용

아래 절차는 이번 신규 DB 구축 범위에 포함하지 않는다.

1. 기존 DB에서 SELECT VERSION(), @@sql_mode, @@collation_server 및 사용자 인증 plugin을
   확인한다. 덤프/복원과 업그레이드 검사는 읽기 권한 있는 관리 계정으로 수행한다.
2. MySQL Shell의 util.checkForServerUpgrade를 실제 출발 버전과 **정확한 8.4 대상 패치**로
   실행하고 보고서를 검토한다. 암호를 명령행/로그에 남기지 않는다.
3. **새 별도 볼륨**의 8.4에 애플리케이션 DB 논리 덤프를 복원한다. 기존 8.0 볼륨은 그대로
   보관하고 mysql 시스템 DB는 major 버전 사이에 단순 복원하지 않는다.
4. caching_sha2_password 앱 계정을 별도로 준비하고 기존 Connector/J로 접속한다.
5. JPA validate로 전체 매핑을 검사한다. ddl-auto=update로 호환성 오류를 가리지 않는다.
6. 회원가입/로그인, workspace tenant 격리, 채용 공개 조회, 지원서 JSON·파일 메타데이터,
   일정·타임존·회의실 동시 예약, 알림·결제·집계, ShedLock 실행을 확인한다.
7. 실행 계획/응답 시간과 재시작 후 데이터 보존, 백업 복원을 확인한다.
8. 승인 후 별도 변경으로 8.4의 검증된 패치/digest와 새 볼륨을 지정한다. 실패 시 보관한
   원본 8.0 볼륨/덤프로 복구한다. 8.4가 수정한 datadir에 8.0을 다시 연결하지 않는다.

## 검증 상태

- 소스/SQL 정적 분석 및 Connector/J 실제 버전 확인: 완료.
- 기존 AWS 운영 데이터 복원·사용자 이전: 이번 시연 운영 범위에서 제외.
- 로컬 ARM64(QEMU)에서 MySQL 8.0.46 / 8.4.13과 Spring prod 기동: 둘 다 healthy.
- 기존 Connector/J 9.2.0으로 두 버전의 caching_sha2_password 인증: 통과.
- 한글 JSON·TIMESTAMP(3)·PreparedStatement·rollback 및 native SQL 7개: 두 버전 모두 통과.
- 업무 테이블은 빈 테스트 스키마이며 실제 데이터·동시성·집계 정확성을 검증한 것은 아니다.
- DB 재시작 후 검증 데이터 보존: 두 버전 모두 통과.
- 동일 테스트 스키마에 prod + JPA validate로 API 재기동: 두 버전 모두 healthy/UP 통과.
- Oracle 네이티브 하드웨어의 실행/성능, 실제 운영 데이터 복원·업무 회귀는 미수행.

**판정: 신규 포트폴리오·시연 DB에 mysql:8.4.13을 사용한다.** 기존 데이터의 버전 업그레이드
안전성을 승인한 것은 아니며 실제 Oracle 배포/성능·외부 연동은 별도 확인한다.

## 신규 빈 DB 최종 검증

2026-11-26, 기존 볼륨과 분리한 새 external QA 볼륨에서 고정 태그 mysql:8.4.13을 실행했다.
최초 테이블 수 0개를 확인하고 일회성 update로 38개 테이블을 생성했다. 이후 기본 prod +
validate 설정으로 API를 재생성해 MySQL·API healthy 및 관리 health=UP을 확인했다.
API 재생성 후 별도 QA 테이블의 검증 행 1개도 유지됐다.

MySQL과 백엔드는 linux/arm64 이미지를 로컬 QEMU에서 실행했다. 기존 AWS 데이터의 이전,
실제 Oracle 배포, S3/Google/메일/결제 외부 연동이나 업무 전체 회귀를 수행한 결과는 아니다.
