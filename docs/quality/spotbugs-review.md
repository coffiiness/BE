# SpotBugs 기존 경고 코드 검토

2026-11-27, 고정한 Temurin 17.0.21.1+2로 재분석한 255건을 기존 15-5 결과와
개별 위치·유형·priority/rank까지 대조했다. 비즈니스 로직과 기존 테스트는 수정하지 않았다.
High가 없어도 경고를 무조건 오탐으로 보지 않으며, 개별 기술 부채 수용 사유를 Baseline에 남겼다.

## 검토와 수용 기록

전체 identity와 `reviewId`, 검토자, 수용 주체, 날짜, 판단, 소스·멤버 근거 및 후속 작업은
`gradle/quality/spotbugs-baseline.json`에 있다. 검토·수용 주체는 이 요청에 따라 작업한 Codex다.
사용자나 다른 사람의 개별 승인을 받은 것으로 표기하지 않았다. 향후 Baseline 변경은 별도
코드 리뷰가 필요하다. 일반 빌드나 Gate 실행은 이 파일을 갱신하지 않는다.

[255건 개별 등록부](spotbugs-review-register.md)는 Baseline identity와 코드 위치를 연결한다.
아래는 공통 원인과 우선순위 판단이다. 유형·패키지 전체를 제외하거나 분석 대상을 줄이지 않았다.

## PnlService dead store 두 건

`PnlService.getReport`의 `allCosts`(69줄)와 `costByMonthCategory`(99줄)는 사용되지 않는다.
그러나 반환되는 비용과 영업이익은 다음 별도 경로에서 계산된다.

1. 각 월의 `CostReader.getSummaryByCategory(year, month)`를 조회한다.
2. LABOR/INFRASTRUCTURE/MARKETING/OTHER의 배열을 채운다.
3. 카테고리 배열을 합산하여 `costTotal`을 구한다.
4. `revenueTotal - costTotal`을 `operatingProfit`으로 반환한다.

`CostReaderImpl`은 실제 `sumByCategoryAndMonth`의 금액을 사용한다.
월별 추이는 SQL의 `GROUP BY costYear, costMonth`로 월당 한 행을 반환한다.
미사용 맵의 LABOR→0 매핑은 실제 반환 계산에 들어가지 않는다.
따라서 이 두 경고에서 **계산 결과 누락은 확인되지 않았다**. 중복 추이 조회와 미사용
수집 작업, 향후 개발자가 그 맵을 잘못 사용할 위험은 남는다.
기존 Pnl API 테스트에도 비용 카테고리 합계와 영업이익 항등식 검증이 있다.

판정은 기존 기술 부채 수용이다. 정리 시 조회/수집만 제거하고 비영(非零) 비용을 가진 보고서
회귀 검증을 함께 수행해야 하며, 이번 단계에서 계산 코드를 변경하지 않는다.

추가로 실제 컴파일된 PnlService를 운영 DB 없이 직접 실행했다. 매출 10,000, 카테고리별 비용
100/200/300/400, 미사용 추이 조회값 999,999를 주었을 때 비용 합계 1,000과 영업이익 9,000을
반환했다. 미사용 추이 값이 반환 비용을 덮어쓰거나 비용 계산에서 누락되는 현상은 재현되지 않았다.
이 검증은 격리된 임시 호출 클래스에서 수행했고 기존 소스와 테스트는 유지했다.

## JwtTokenProvider 생성자

`Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(UTF_8))`는 유효하지 않거나 짧은
키에 대해 예외를 발생시킨다. 잘못된 키로 애플리케이션을 기동하지 않는 fail-fast 계약은 유지해야 한다.

`CT_CONSTRUCTOR_THROW`는 non-final 클래스가 예외로 생성에 실패할 때 미완성 객체를
공격자가 만든 하위 클래스 finalizer에서 접근할 수 있는 일반 패턴을 지적한다.
현재 저장소에는 JwtTokenProvider의 하위 클래스나 finalize 구현이 없고, Spring이 관리하는
빈 생성 경로를 사용한다. 현재 경로에서 악용이나 즉시 발생하는 운영 장애는 확인하지 못했다.
경고를 없애려고 키 검증 예외를 삼키면 보안 동작이 나빠진다.

기존 기술 부채로 기록하고, final 클래스/생성자 경계 개선 여부는 Spring 프록시와 빈 계약까지
검토하는 별도 보안 변경으로 남긴다. 실제 JWT Secret 값은 읽거나 기록하지 않았다.

## core-api 테스트의 rank 13 null 경고 여덟 건

| 위치 | 건수 | 실제 코드와 판정 |
| --- | ---: | --- |
| applications/detail/GET_specs 119/120/127 | 3 | null guard 후 같은 ResponseEntity.getBody를 반복 호출한다. 분석기가 반복 호출 결과의 동일성을 모두 추론하지 못한다. 지역 변수로 보관하는 개선 여지 |
| applications/excel/GET_specs 106/190 | 2 | AssertJ isNotNull 뒤 재호출한다. assertion 및 반환 객체 불변성을 명시하면 분석·가독성을 개선할 수 있음 |
| applications/excel/GET_specs 251/309 | 2 | 2xx status만 확인하고 body를 ByteArrayInputStream에 전달한다. 2xx 자체가 non-null body를 보장하지 않아 테스트 단언 보강 필요 |
| EndpointSecurityRegressionTest 113 | 1 | asyncStarted를 확인하지만 finally의 getAsyncContext().complete는 nullable context를 직접 역참조한다. 실패 시 정리 예외가 최초 오류를 가릴 수 있음 |

모두 테스트 코드다. 운영 API의 null 취약점 8개로 보고하지 않는다.
현재 테스트는 성공하지만 모든 nullable 경로가 안전하다고 확정하는 근거로 쓰지 않는다.
테스트 보강 작업으로 기록하며 테스트 삭제·비활성화나 패키지 제외는 하지 않았다.

## EI_EXPOSE_REP 및 EI_EXPOSE_REP2 분류

`EI_EXPOSE_REP` 105건, `EI_EXPOSE_REP2` 107건으로 합계 212건이다.

| 실제 발생 원인 | 건수 | 검토 근거 |
| --- | ---: | --- |
| DTO record | 100 | 요청/응답/Google DTO에서 List·Map·array를 생성자에 보관하거나 접근자로 반환 |
| 도메인·이벤트·조회 projection record | 94 | Kanban, 일정, 면접, 채용, PnlReport, repository projection 등이 컬렉션/배열을 공유 |
| ConfigurationProperties record | 4 | 두 CorsProperties의 allowedOrigins List. 시작 시 설정 소유지만 깊은 불변성은 없음 |
| 테스트 record | 10 | 공유 TestRestTemplate/ObjectMapper 8건은 의도한 fixture 협력 객체, 나머지 응답 List 2건은 테스트 DTO aliasing |
| JPA JSON 이력 Entity | 2 | InterviewScheduleHistoryEntity.eventData, RecruitmentHistoryEntity.changeLog를 참조 그대로 저장 |
| Spring DI | 1 | SecurityConfig가 관리 중인 JwtAuthenticationFilter를 보관. 협력 객체 복제는 적절한 해결이 아님 |
| enum 공유 List | 1 | AutomationTemplateCode의 모든 상수는 List.of를 전달하고 setter가 없어 현재 목록은 변경 불가능 |

record는 필드의 재할당을 막을 뿐 List/Map/array 내부의 변경을 막지 않는다.
DTO·도메인 record를 일괄 오탐으로 승인하지 않았다. collection/array 소유권, null 처리,
Jackson 매핑을 고려한 방어 복사 여부를 후속 변경에서 검토해야 한다.

특히 JPA 이력 Map 두 곳은 데이터 무결성 관점에서 먼저 검토할 가치가 있다.
면접 writer는 새 이벤트 Map을 만든 뒤 save하고, 채용 writer도 전달받은 변경 로그를 save한다.
현재 호출 경로에서 save 이후 같은 Map을 바꿔 이력을 훼손한 증거는 확인하지 못했지만,
중첩 컬렉션까지 포함한 aliasing 위험은 남는다. 단순 Map.copyOf는 깊은 복사가 아니고,
Hibernate JSON/dirty checking과도 맞춰야 하므로 임의 수정하지 않았다.

## 나머지 유형 검토

| 유형 | 건수 | 근거와 후속 작업 |
| --- | ---: | --- |
| DM_CONVERT_CASE | 8 | 기본 Locale에 의존하는 enum/문자열 매칭. Turkish locale 등에서 차이가 가능하므로 Locale.ROOT 검토 |
| REC_CATCH_EXCEPTION | 5 | 기존 JSON fallback/HTTP 오류/서명 오류 변환을 위한 넓은 catch. 예외 범위를 좁히는 변경은 기존 오류 계약 검토 필요 |
| DLS_DEAD_LOCAL_STORE | 4 | Pnl 2건 외 테스트 fixture 반환값 미사용 2건. 대입을 지우더라도 상태 생성 호출은 유지해야 함 |
| THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION | 7 | 테스트·Jackson helper·의도적 서버 오류 endpoint의 넓은 throws |
| NP_LOAD_OF_KNOWN_NULL_VALUE | 3 | Google 동기화 테스트에서 timestamp 부재 분기를 의도적으로 검증 |
| THROWS_METHOD_THROWS_RUNTIMEEXCEPTION | 2 | 면접 lock 재시도 소진과 email throwOnFailure의 기존 전파 계약 |
| NM_CLASS_NAMING_CONVENTION | 2 | 테스트용 한글 private record 이름 |
| BX_UNBOXING_IMMEDIATELY_REBOXED | 1 | null guard가 있는 Long 조건식의 불필요 boxing |
| UPM_UNCALLED_PRIVATE_METHOD | 1 | ExternalEvent의 사용되지 않는 private helper |
| SIC_INNER_SHOULD_BE_STATIC_ANON | 1 | 즉시 Jackson 역직렬화에 전달되는 TypeReference 할당. 지속적인 외부 참조 누수는 확인하지 못함 |
| CT_CONSTRUCTOR_THROW | 1 | 위 JWT fail-fast/상속 경계 검토 |

## priority와 rank

priority는 1 High, 2 Normal, 3 Low로 관리한다. 현재 High 0, Normal 228, Low 27이다.
rank는 별도 숫자이며 작을수록 우선 검토 대상이다. 현재 rank 13: 8, 16: 1, 17: 6,
18: 213, 19: 14, 20: 13이다. rank 13을 priority High로 바꾸어 세거나 반대로 무시하지 않는다.
Gate는 전체 High를 차단하고, 기존 항목의 priority 상승이나 rank 숫자가 작아지는 심각도 상승도
새 검토가 필요한 악화로 차단한다.

## 비동기 테스트 실행 완료 조건

구독 상세 테스트 `api/admin/subscription/detail/GET_specs.java:22`는 300ms sleep 후
구독 배열이 비어 있으면 return 한다. 실제 구독 생성 확인 없이 성공할 수 있으므로 우선 보강할
테스트 신뢰성 문제다. 목록 테스트에도 고정 sleep이 있다. 일정/알림 테스트 일부는 이미
Awaitility로 상태를 기다리지만 구독 테스트의 완료 보장은 부족하다.

이 동작은 이번 요청의 검토 사항으로 보고하며 기존 테스트를 임의 변경하지 않았다.
후속 작업에서는 구독이 만들어질 때까지 제한 시간 내 poll하고 빈 결과를 명시적으로 실패시킨다.
정확한 JDK 고정은 바이트코드 차이는 줄이지만 비동기 완료 시점의 변동 자체를 해결하지 않는다.

## 우선 후속 작업

현재 255건에서 즉시 운영 로직을 바꿔야 하는 확정 계산 누락이나 현재 악용되는 High 결함은
확인되지 않았다. 우선순위는 비동기 테스트의 조건부 조기 성공 제거, JPA JSON 이력의 불변
snapshot 계약, Locale 독립 파싱, null 관련 테스트 단언/정리 보강 순으로 제안한다.
모두 별도 변경과 회귀 검증이 필요하며, 기존 경고의 현재 수용이 새 경고나 악화를 허용하지 않는다.

