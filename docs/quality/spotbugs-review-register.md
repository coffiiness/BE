# SpotBugs 개별 검토 등록부

검토일 2026-11-27. 주체는 사용자 요청에 따라 위임 검토한 Codex이며, 사람의 개별 승인으로 표기하지 않는다. 전체 식별자와 상세 근거는 gradle/quality/spotbugs-baseline.json에 보관한다.

| 검토 ID | 유형 | Priority / rank | 분류 | 소스 위치 | 판단 |
| --- | --- | --- | --- | --- | --- |
| SB-0e424a6cdff5 | DM_CONVERT_CASE | 3 / 20 | Locale-sensitive conversion | :clients:google-calendar Main com/coffiness/calfit/api/GoogleOAuthClient.java:92 | accepted-debt |
| SB-f463b9474eaf | EI_EXPOSE_REP | 2 / 18 | DTO record | :clients:google-calendar Main com/coffiness/calfit/dto/GoogleCalendarListResponseDto.java:8 | accepted-debt |
| SB-0f7e0a05d519 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :clients:google-calendar Main com/coffiness/calfit/dto/GoogleCalendarListResponseDto.java:8 | accepted-debt |
| SB-5bceb37ff659 | EI_EXPOSE_REP | 2 / 18 | DTO record | :clients:google-calendar Main com/coffiness/calfit/dto/GoogleCalendarSyncResponseDto.java:12 | accepted-debt |
| SB-2144d59c41a7 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :clients:google-calendar Main com/coffiness/calfit/dto/GoogleCalendarSyncResponseDto.java:12 | accepted-debt |
| SB-106f9cc6ec84 | EI_EXPOSE_REP | 2 / 18 | DTO record | :clients:google-calendar Main com/coffiness/calfit/dto/GoogleCalendarWatchRequestDto.java:8 | accepted-debt |
| SB-d7337fdcd799 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :clients:google-calendar Main com/coffiness/calfit/dto/GoogleCalendarWatchRequestDto.java:8 | accepted-debt |
| SB-97706a56976e | EI_EXPOSE_REP | 2 / 18 | Configuration record | :core:core-api Main com/coffiness/calfit/core/api/config/CorsProperties.java:6 | accepted-debt |
| SB-81624fdb5884 | EI_EXPOSE_REP2 | 2 / 18 | Configuration record | :core:core-api Main com/coffiness/calfit/core/api/config/CorsProperties.java:7 | accepted-debt |
| SB-6fe533e23aee | DM_CONVERT_CASE | 3 / 20 | Locale-sensitive conversion | :core:core-api Main com/coffiness/calfit/core/api/controller/v1/AdminCostController.java:96 | accepted-debt |
| SB-d886d42db985 | DM_CONVERT_CASE | 3 / 20 | Locale-sensitive conversion | :core:core-api Main com/coffiness/calfit/core/api/controller/v1/AdminInvoiceController.java:57 | accepted-debt |
| SB-54a07cecaeb7 | REC_CATCH_EXCEPTION | 2 / 17 | Broad recovery catch | :core:core-api Main com/coffiness/calfit/core/api/controller/v1/AdminPnlController.java:107 | accepted-debt |
| SB-50733eb0a739 | DM_CONVERT_CASE | 3 / 20 | Locale-sensitive conversion | :core:core-api Main com/coffiness/calfit/core/api/controller/v1/AdminSubscriptionController.java:38 | accepted-debt |
| SB-55b8c7e3144a | REC_CATCH_EXCEPTION | 3 / 20 | Broad recovery catch | :core:core-api Main com/coffiness/calfit/core/api/controller/v1/ApplicationExcelController.java:157 | accepted-debt |
| SB-799dc86b61fc | BX_UNBOXING_IMMEDIATELY_REBOXED | 2 / 18 | Boxing performance | :core:core-api Main com/coffiness/calfit/core/api/controller/v1/ApplicationTemplateController.java:247 | accepted-debt |
| SB-ca71d0e7fbcc | DM_CONVERT_CASE | 3 / 20 | Locale-sensitive conversion | :core:core-api Main com/coffiness/calfit/core/api/controller/v1/NotificationController.java:119 | accepted-debt |
| SB-8ece53d90666 | DM_CONVERT_CASE | 3 / 20 | Locale-sensitive conversion | :core:core-api Main com/coffiness/calfit/core/api/facade/recruitment/CareerFacade.java:48 | accepted-debt |
| SB-89c3236d619f | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:367 | accepted-debt |
| SB-70d9a0f0f610 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:367 | accepted-debt |
| SB-7e0669948dac | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:367 | accepted-debt |
| SB-fa0e07e290db | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:367 | accepted-debt |
| SB-0385083cf97b | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:354 | accepted-debt |
| SB-21460ffc852a | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:354 | accepted-debt |
| SB-51b1ee1addef | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:354 | accepted-debt |
| SB-f834f9ad86d8 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:354 | accepted-debt |
| SB-33ee83cee4e1 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:337 | accepted-debt |
| SB-007a2bb99174 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:337 | accepted-debt |
| SB-e017d969efc7 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:337 | accepted-debt |
| SB-4b66f208abc1 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/api/facade/report/ReportFacade.java:337 | accepted-debt |
| SB-3a8d3ac4a8ab | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/support/Page.java:5 | accepted-debt |
| SB-ade6420b48d1 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:core-api Main com/coffiness/calfit/core/support/Page.java:5 | accepted-debt |
| SB-4f218dc09bd7 | NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | 2 / 13 | Test nullable response/async cleanup | :core:core-api Test com/coffiness/calfit/api/applications/detail/GET_specs.java:119 | accepted-debt |
| SB-3caf6b3496f4 | NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | 2 / 13 | Test nullable response/async cleanup | :core:core-api Test com/coffiness/calfit/api/applications/detail/GET_specs.java:120 | accepted-debt |
| SB-32b2b9f430d2 | NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | 2 / 13 | Test nullable response/async cleanup | :core:core-api Test com/coffiness/calfit/api/applications/detail/GET_specs.java:127 | accepted-debt |
| SB-030439e62fcc | NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | 2 / 13 | Test nullable response/async cleanup | :core:core-api Test com/coffiness/calfit/api/applications/excel/GET_specs.java:106 | accepted-debt |
| SB-1a620e11ceb4 | NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | 2 / 13 | Test nullable response/async cleanup | :core:core-api Test com/coffiness/calfit/api/applications/excel/GET_specs.java:190 | accepted-debt |
| SB-1ad87f272128 | NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | 2 / 13 | Test nullable response/async cleanup | :core:core-api Test com/coffiness/calfit/api/applications/excel/GET_specs.java:251 | accepted-debt |
| SB-e38d48df932d | NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | 2 / 13 | Test nullable response/async cleanup | :core:core-api Test com/coffiness/calfit/api/applications/excel/GET_specs.java:309 | accepted-debt |
| SB-0055ab7d805d | THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION | 3 / 19 | Test throws contract | :core:core-api Test com/coffiness/calfit/api/applications/excel/GET_specs.java:59 | accepted-debt |
| SB-7d8e206c9b19 | THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION | 3 / 19 | Test throws contract | :core:core-api Test com/coffiness/calfit/api/applications/excel/GET_specs.java:145 | accepted-debt |
| SB-0551e2297a3c | THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION | 3 / 19 | Test throws contract | :core:core-api Test com/coffiness/calfit/api/applications/excel/GET_specs.java:212 | accepted-debt |
| SB-9d6a1c6941bc | THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION | 3 / 19 | Test throws contract | :core:core-api Test com/coffiness/calfit/api/applications/excel/GET_specs.java:270 | accepted-debt |
| SB-6b0185563e2b | NP_LOAD_OF_KNOWN_NULL_VALUE | 3 / 19 | Intentional test null | :core:core-api Test com/coffiness/calfit/api/calendar/connect/POST_specs.java:200 | not-a-defect |
| SB-dd3b637f5d30 | NP_LOAD_OF_KNOWN_NULL_VALUE | 3 / 19 | Intentional test null | :core:core-api Test com/coffiness/calfit/api/calendar/connect/POST_specs.java:216 | not-a-defect |
| SB-b3b119887b72 | NP_LOAD_OF_KNOWN_NULL_VALUE | 3 / 19 | Intentional test null | :core:core-api Test com/coffiness/calfit/api/calendar/connect/POST_specs.java:233 | not-a-defect |
| SB-e5277605237c | EI_EXPOSE_REP | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/ApplicationFileFixture.java:19 | not-a-defect |
| SB-1acca4bcc2a6 | EI_EXPOSE_REP | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/ApplicationFileFixture.java:19 | not-a-defect |
| SB-cf820e6ad0dd | EI_EXPOSE_REP2 | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/ApplicationFileFixture.java:19 | not-a-defect |
| SB-e6caa9911cb4 | EI_EXPOSE_REP2 | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/ApplicationFileFixture.java:19 | not-a-defect |
| SB-8d6be0a22a33 | EI_EXPOSE_REP | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/BaseFixture.java:12 | not-a-defect |
| SB-9bc7a2cf6418 | EI_EXPOSE_REP | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/BaseFixture.java:12 | not-a-defect |
| SB-9f8cc6d8650c | EI_EXPOSE_REP2 | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/BaseFixture.java:12 | not-a-defect |
| SB-b718f38ccada | EI_EXPOSE_REP2 | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/BaseFixture.java:12 | not-a-defect |
| SB-f984ebc49b08 | EI_EXPOSE_REP | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/NotificationFixture.java:133 | accepted-debt |
| SB-2608f79de2da | EI_EXPOSE_REP2 | 2 / 18 | Test record | :core:core-api Test com/coffiness/calfit/api/fixture/NotificationFixture.java:133 | accepted-debt |
| SB-c0951cec2bc8 | NM_CLASS_NAMING_CONVENTION | 3 / 19 | Korean test record name | :core:core-api Test com/coffiness/calfit/api/interview/create/POST_specs.java:436 | not-a-defect |
| SB-d060151ed3ff | NM_CLASS_NAMING_CONVENTION | 3 / 19 | Korean test record name | :core:core-api Test com/coffiness/calfit/api/interview/create/POST_specs.java:438 | not-a-defect |
| SB-a27642df8f25 | DLS_DEAD_LOCAL_STORE | 2 / 17 | Test fixture dead store | :core:core-api Test com/coffiness/calfit/api/meetingRoom/reserve/POST_specs.java:159 | accepted-debt |
| SB-82e22b9a9399 | DLS_DEAD_LOCAL_STORE | 2 / 17 | Test fixture dead store | :core:core-api Test com/coffiness/calfit/api/recruitment/weekly/GET_specs.java:296 | accepted-debt |
| SB-d476c29faa63 | NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | 2 / 13 | Test nullable response/async cleanup | :core:core-api Test com/coffiness/calfit/api/regression/EndpointSecurityRegressionTest.java:113 | accepted-debt |
| SB-38a564294af1 | THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION | 3 / 19 | Test throws contract | :core:core-api Test com/coffiness/calfit/api/regression/EndpointSecurityRegressionTest.java:125 | accepted-debt |
| SB-926480dfc304 | THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION | 3 / 19 | Test throws contract | :core:core-api Test com/coffiness/calfit/core/api/controller/ApiControllerAdviceTest.java:146 | accepted-debt |
| SB-b6638acbc8b7 | THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION | 3 / 19 | Test throws contract | :core:core-api Test com/coffiness/calfit/docs/RestDocsTest.java:88 | accepted-debt |
| SB-5d57cec74408 | EI_EXPOSE_REP | 2 / 18 | Enum immutable List.of | :core:core-enum Main com/coffiness/calfit/core/enums/AutomationTemplateCode.java:38 | not-a-defect |
| SB-acf1b183331d | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-application Main com/coffiness/calfit/api/v1/response/ApplicationDetailResponse.java:8 | accepted-debt |
| SB-9df9a5b1c039 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-application Main com/coffiness/calfit/api/v1/response/ApplicationDetailResponse.java:8 | accepted-debt |
| SB-e63485858dd0 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-application Main com/coffiness/calfit/api/v1/response/ApplicationDetailResponse.java:8 | accepted-debt |
| SB-c71a5ed229c1 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-application Main com/coffiness/calfit/api/v1/response/ApplicationDetailResponse.java:8 | accepted-debt |
| SB-322013137643 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-application Main com/coffiness/calfit/api/v1/response/AutomationTemplateResponse.java:7 | accepted-debt |
| SB-94911b49d804 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-application Main com/coffiness/calfit/api/v1/response/AutomationTemplateResponse.java:7 | accepted-debt |
| SB-fa3578c90efd | REC_CATCH_EXCEPTION | 2 / 17 | Broad recovery catch | :core:domain-application Main com/coffiness/calfit/domain/application/ApplicationService.java:353 | accepted-debt |
| SB-e25ef8c8a573 | REC_CATCH_EXCEPTION | 3 / 20 | Broad recovery catch | :core:domain-application Main com/coffiness/calfit/domain/application/AutomationEventExecutor.java:121 | accepted-debt |
| SB-0a1aec52b4e5 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-application Main com/coffiness/calfit/domain/application/KanbanBoard.java:5 | accepted-debt |
| SB-5d4f3a4adbc1 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-application Main com/coffiness/calfit/domain/application/KanbanBoard.java:5 | accepted-debt |
| SB-253c0a2d8fe8 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-application Main com/coffiness/calfit/domain/application/KanbanColumn.java:7 | accepted-debt |
| SB-425a7c05a792 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-application Main com/coffiness/calfit/domain/application/KanbanColumn.java:7 | accepted-debt |
| SB-e379be16cadf | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/CostSummaryResponse.java:6 | accepted-debt |
| SB-2fa9e17bd816 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/CostSummaryResponse.java:6 | accepted-debt |
| SB-3f390de1c56f | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/CostTrendResponse.java:6 | accepted-debt |
| SB-4ad3db35982e | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/CostTrendResponse.java:6 | accepted-debt |
| SB-3f85b89cf99f | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PlanDistributionResponse.java:6 | accepted-debt |
| SB-efdddcc892b7 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PlanDistributionResponse.java:6 | accepted-debt |
| SB-c5708fc922d4 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-88ba8af9073e | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-0510752ae0c8 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-10378b97dd4f | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-9b929b0a8652 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-3f0a71561e23 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-a723600190c1 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-7cde6611d438 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-f4e562d49dd6 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-61dfc7f0beb8 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-0df11f79e78c | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-fd1f3f3e8ecd | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-8a5f93847d3d | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-11e04208137d | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-9d159f2b57aa | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-708c7923b075 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/PnlReportResponse.java:6 | accepted-debt |
| SB-53b7fc5f6ddf | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/RevenueCostTrendResponse.java:6 | accepted-debt |
| SB-51cdaab1fd55 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-billing Main com/coffiness/calfit/api/v1/response/RevenueCostTrendResponse.java:6 | accepted-debt |
| SB-980c31d39377 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-7b231535dc4c | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-8dfc046e8ccd | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-3d9e9de20b94 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-44ec8fb31601 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-0b46bf80f13c | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-2d66c6050c14 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-3550135411d6 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-a9fb2a0e7b62 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-5ee2abb72174 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-c6ea1ad482ba | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-640667c13dc1 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-96ee9f1c0960 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-fcff7e28c1b2 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-3651040e3362 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-6fdb65298d28 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlReport.java:5 | accepted-debt |
| SB-8af4d140e7f4 | DLS_DEAD_LOCAL_STORE | 2 / 17 | Pnl redundant computation | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlService.java:69 | accepted-debt |
| SB-8410e2b606f0 | DLS_DEAD_LOCAL_STORE | 2 / 17 | Pnl redundant computation | :core:domain-billing Main com/coffiness/calfit/domain/billing/pnl/PnlService.java:99 | accepted-debt |
| SB-a9011bf680ca | UPM_UNCALLED_PRIVATE_METHOD | 3 / 20 | Unused helper | :core:domain-calendar Main com/coffiness/calfit/domain/ExternalEvent.java:32 | accepted-debt |
| SB-9f56209b9be4 | REC_CATCH_EXCEPTION | 3 / 20 | Broad recovery catch | :core:domain-calendar Main com/coffiness/calfit/domain/GoogleChannelTokenService.java:97 | accepted-debt |
| SB-3e7aa6c75aa1 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/ScheduleAvailability.java:10 | accepted-debt |
| SB-faa0abe71223 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/ScheduleAvailability.java:10 | accepted-debt |
| SB-10ac052e15c0 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/ScheduleAvailability.java:16 | accepted-debt |
| SB-c0e0dfa8a2de | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/ScheduleAvailability.java:16 | accepted-debt |
| SB-d9731f8d0dcc | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/ScheduleDetailInfo.java:7 | accepted-debt |
| SB-70e0ae45ac95 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/ScheduleDetailInfo.java:7 | accepted-debt |
| SB-3a2d2b0b5c1b | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/ScheduleDetailInfo.java:7 | accepted-debt |
| SB-4ec7f3fa8e02 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/ScheduleDetailInfo.java:7 | accepted-debt |
| SB-1ef173608634 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/event/ScheduleAttendeeNotificationEvent.java:11 | accepted-debt |
| SB-018a6ef4d6d4 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/domain/event/ScheduleAttendeeNotificationEvent.java:11 | accepted-debt |
| SB-c0105906b03c | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/model/GoogleCalendarSyncResult.java:6 | accepted-debt |
| SB-08c8d20d0e1d | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-calendar Main com/coffiness/calfit/model/GoogleCalendarSyncResult.java:6 | accepted-debt |
| SB-1eca9458b798 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/request/ScheduleCreateRequest.java:10 | accepted-debt |
| SB-c8d4b1bee56d | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/request/ScheduleCreateRequest.java:21 | accepted-debt |
| SB-6ed2cd7a94d1 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/request/ScheduleUpdateRequest.java:10 | accepted-debt |
| SB-9b9ae22089d6 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/request/ScheduleUpdateRequest.java:10 | accepted-debt |
| SB-937121bc86d8 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/response/ScheduleAvailabilityResponse.java:12 | accepted-debt |
| SB-54a65df6fa89 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/response/ScheduleAvailabilityResponse.java:12 | accepted-debt |
| SB-16d01dc330b2 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/response/ScheduleAvailabilityResponse.java:22 | accepted-debt |
| SB-65446e11512e | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/response/ScheduleAvailabilityResponse.java:22 | accepted-debt |
| SB-b859c1a4e6e1 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/response/ScheduleDetailResponse.java:9 | accepted-debt |
| SB-e669794492fb | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/response/ScheduleDetailResponse.java:9 | accepted-debt |
| SB-ed07b139ffbd | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/response/ScheduleDetailResponse.java:9 | accepted-debt |
| SB-00e51a492dab | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-calendar Main com/coffiness/calfit/v1/response/ScheduleDetailResponse.java:9 | accepted-debt |
| SB-4adb3bb97adf | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/request/InterviewCreateRequest.java:9 | accepted-debt |
| SB-071e1c1fa2b2 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/request/InterviewCreateRequest.java:9 | accepted-debt |
| SB-ed9515563c2a | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/request/InterviewCreateRequest.java:9 | accepted-debt |
| SB-d3a9a05959f5 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/request/InterviewCreateRequest.java:9 | accepted-debt |
| SB-e4e6f95b5ee8 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewAvailabilityResponse.java:6 | accepted-debt |
| SB-cbc752f77f93 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewAvailabilityResponse.java:6 | accepted-debt |
| SB-ac13745567b3 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewAvailabilityResponse.java:6 | accepted-debt |
| SB-05983cddfda7 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewAvailabilityResponse.java:6 | accepted-debt |
| SB-f856873dc38c | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewAvailabilityResponse.java:6 | accepted-debt |
| SB-02065cfd4bf8 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewAvailabilityResponse.java:6 | accepted-debt |
| SB-1bdd7dc6a42a | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewPendingStageResponse.java:9 | accepted-debt |
| SB-7469187e1ed9 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewPendingStageResponse.java:9 | accepted-debt |
| SB-7e346a1aeb98 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewResponse.java:9 | accepted-debt |
| SB-282bf38d41a2 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewResponse.java:9 | accepted-debt |
| SB-ea49501d0c61 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewResponse.java:9 | accepted-debt |
| SB-23e0c6aa466b | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-interview Main com/coffiness/calfit/api/v1/response/InterviewResponse.java:9 | accepted-debt |
| SB-79a7e4cc1a99 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/Interview.java:8 | accepted-debt |
| SB-6d2deea00533 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/Interview.java:8 | accepted-debt |
| SB-07cf364bb5e8 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/Interview.java:8 | accepted-debt |
| SB-406cc378de40 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/Interview.java:8 | accepted-debt |
| SB-99f8c67ec7b4 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/InterviewAvailability.java:6 | accepted-debt |
| SB-520f4d6f134a | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/InterviewAvailability.java:6 | accepted-debt |
| SB-a648eb343240 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/InterviewAvailability.java:6 | accepted-debt |
| SB-a7631b2b8065 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/InterviewAvailability.java:6 | accepted-debt |
| SB-968b8cb530b9 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/InterviewAvailability.java:6 | accepted-debt |
| SB-b69f994294d1 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/InterviewAvailability.java:6 | accepted-debt |
| SB-a27528a82e2a | THROWS_METHOD_THROWS_RUNTIMEEXCEPTION | 3 / 19 | Existing runtime propagation | :core:domain-interview Main com/coffiness/calfit/domain/interview/InterviewService.java:76 | accepted-debt |
| SB-946152e47e05 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/PendingInterviewStage.java:8 | accepted-debt |
| SB-ed766405f50c | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/PendingInterviewStage.java:8 | accepted-debt |
| SB-160c181609b2 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/event/InterviewCreatedEvent.java:7 | accepted-debt |
| SB-dd18327c4537 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/event/InterviewCreatedEvent.java:7 | accepted-debt |
| SB-19b9d76d7ad8 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/event/InterviewCreatedEvent.java:7 | accepted-debt |
| SB-cf14ed04eb07 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-interview Main com/coffiness/calfit/domain/interview/event/InterviewCreatedEvent.java:7 | accepted-debt |
| SB-5b6086654cf5 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/request/MeetingRoomCreateRequest.java:11 | accepted-debt |
| SB-7df3cb4b8010 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/request/MeetingRoomCreateRequest.java:11 | accepted-debt |
| SB-7e3ac5b6355a | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/request/MeetingRoomReservationCreateRequest.java:7 | accepted-debt |
| SB-84437c1f4844 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/request/MeetingRoomReservationCreateRequest.java:7 | accepted-debt |
| SB-a08656ef2f54 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/request/MeetingRoomUpdateRequest.java:11 | accepted-debt |
| SB-b30b68265edb | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/request/MeetingRoomUpdateRequest.java:11 | accepted-debt |
| SB-9ad1c16058c8 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/response/MeetingRoomReservationResponse.java:8 | accepted-debt |
| SB-6a92034a6f0f | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/response/MeetingRoomReservationResponse.java:8 | accepted-debt |
| SB-2bbdda2f77c5 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/response/MeetingRoomResponse.java:6 | accepted-debt |
| SB-51fe584f4bcf | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-meeting-room Main com/coffiness/calfit/api/v1/response/MeetingRoomResponse.java:6 | accepted-debt |
| SB-da76becdba5d | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-meeting-room Main com/coffiness/calfit/domain/meetingRoom/MeetingRoom.java:5 | accepted-debt |
| SB-aa54a23be314 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-meeting-room Main com/coffiness/calfit/domain/meetingRoom/MeetingRoom.java:5 | accepted-debt |
| SB-3484bf5fc259 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-meeting-room Main com/coffiness/calfit/domain/meetingRoom/MeetingRoomReservationCreatedEvent.java:7 | accepted-debt |
| SB-fa8f3fc1d7c6 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-meeting-room Main com/coffiness/calfit/domain/meetingRoom/MeetingRoomReservationCreatedEvent.java:7 | accepted-debt |
| SB-22f0536fee72 | SIC_INNER_SHOULD_BE_STATIC_ANON | 3 / 20 | Jackson TypeReference allocation | :core:domain-meeting-room Main com/coffiness/calfit/infra/MeetingRoomReaderImpl.java:136 | accepted-debt |
| SB-6006d3136648 | DM_CONVERT_CASE | 3 / 20 | Locale-sensitive conversion | :core:domain-notification Main com/coffiness/calfit/domain/notification/DashboardChatService.java:62 | accepted-debt |
| SB-e57ca651293b | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-notification Main com/coffiness/calfit/domain/notification/NotificationPage.java:5 | accepted-debt |
| SB-de02c35e5d52 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-notification Main com/coffiness/calfit/domain/notification/NotificationPage.java:5 | accepted-debt |
| SB-53f082c550c2 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentCreateRequest.java:9 | accepted-debt |
| SB-0abc9da1ceca | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentCreateRequest.java:9 | accepted-debt |
| SB-547234e919f2 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentCreateRequest.java:9 | accepted-debt |
| SB-903be3acc29d | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentCreateRequest.java:9 | accepted-debt |
| SB-8f9ee571a9a8 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentCreateRequest.java:9 | accepted-debt |
| SB-3a139eefcccd | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentCreateRequest.java:9 | accepted-debt |
| SB-4f3d9183be3b | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentInterviewersUpdateRequest.java:10 | accepted-debt |
| SB-a9bbcb4f1848 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentInterviewersUpdateRequest.java:10 | accepted-debt |
| SB-1390586a8771 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentUpdateRequest.java:12 | accepted-debt |
| SB-f93fbe6cbe81 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentUpdateRequest.java:12 | accepted-debt |
| SB-9fdcc2247b2e | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentUpdateRequest.java:12 | accepted-debt |
| SB-58fdf3d0ff53 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentUpdateRequest.java:12 | accepted-debt |
| SB-9da0c81a4bd7 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentUpdateRequest.java:12 | accepted-debt |
| SB-094632d461d2 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/request/RecruitmentUpdateRequest.java:12 | accepted-debt |
| SB-36043cded13f | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentDetailResponse.java:10 | accepted-debt |
| SB-4ca018dca571 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentDetailResponse.java:10 | accepted-debt |
| SB-2eb8829360b1 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentDetailResponse.java:10 | accepted-debt |
| SB-b7150224d170 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentDetailResponse.java:10 | accepted-debt |
| SB-13446d461eb1 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentDetailResponse.java:10 | accepted-debt |
| SB-eced28b3d969 | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentDetailResponse.java:10 | accepted-debt |
| SB-1a3b072c4bbf | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentListResponse.java:11 | accepted-debt |
| SB-89c8261e2dd5 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentListResponse.java:11 | accepted-debt |
| SB-0501de1fe777 | EI_EXPOSE_REP | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentListResponse.java:11 | accepted-debt |
| SB-4170c723177c | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentListResponse.java:11 | accepted-debt |
| SB-e15f57201e2f | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentListResponse.java:11 | accepted-debt |
| SB-3b2c85b377cc | EI_EXPOSE_REP2 | 2 / 18 | DTO record | :core:domain-recruitment Main com/coffiness/calfit/api/v1/response/RecruitmentListResponse.java:11 | accepted-debt |
| SB-45d1d500ae54 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/Recruitment.java:9 | accepted-debt |
| SB-fe8a9351bed1 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/Recruitment.java:9 | accepted-debt |
| SB-9914da125d33 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/Recruitment.java:9 | accepted-debt |
| SB-f548b750a35f | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/Recruitment.java:26 | accepted-debt |
| SB-4039f9b22b9d | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/Recruitment.java:26 | accepted-debt |
| SB-fbf419cd1ee6 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/Recruitment.java:26 | accepted-debt |
| SB-bfb2a7ab9857 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentDetailInfo.java:9 | accepted-debt |
| SB-cf177108698a | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentDetailInfo.java:9 | accepted-debt |
| SB-e30a26fc2e68 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentDetailInfo.java:9 | accepted-debt |
| SB-d6920f45ac94 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentDetailInfo.java:9 | accepted-debt |
| SB-5bc1b09251f1 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentDetailInfo.java:9 | accepted-debt |
| SB-1bc451e653cc | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentDetailInfo.java:9 | accepted-debt |
| SB-a7eb80dbd8bb | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentHistoryChangeLog.java:10 | accepted-debt |
| SB-74c73e5a6398 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentHistoryChangeLog.java:10 | accepted-debt |
| SB-9832a589c94e | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentHistoryChangeLog.java:10 | accepted-debt |
| SB-ddcd005057fd | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentHistoryChangeLog.java:10 | accepted-debt |
| SB-0cedfd391133 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentHistoryChangeLog.java:10 | accepted-debt |
| SB-b29d8be12734 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentHistoryChangeLog.java:10 | accepted-debt |
| SB-9ab3c215f3fe | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentListInfo.java:8 | accepted-debt |
| SB-9f6f62ba1942 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentListInfo.java:8 | accepted-debt |
| SB-ce9c5b161a95 | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentListInfo.java:8 | accepted-debt |
| SB-1be98b2c80ac | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentListInfo.java:8 | accepted-debt |
| SB-8f129e8d1e20 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentListInfo.java:8 | accepted-debt |
| SB-11622507c9e8 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :core:domain-recruitment Main com/coffiness/calfit/domain/recruitment/RecruitmentListInfo.java:8 | accepted-debt |
| SB-fb80f365525a | EI_EXPOSE_REP | 2 / 18 | Domain/event/projection record | :storage:db-core Main com/coffiness/calfit/storage/db/core/interview/InterviewRepository.java:33 | accepted-debt |
| SB-9cf37fc09d48 | EI_EXPOSE_REP2 | 2 / 18 | Domain/event/projection record | :storage:db-core Main com/coffiness/calfit/storage/db/core/interview/InterviewRepository.java:33 | accepted-debt |
| SB-961d9d5e7293 | EI_EXPOSE_REP2 | 2 / 18 | JPA JSON history map | :storage:db-core Main com/coffiness/calfit/storage/db/core/interview/InterviewScheduleHistoryEntity.java:46 | accepted-debt |
| SB-97abb0591dbb | EI_EXPOSE_REP2 | 2 / 18 | JPA JSON history map | :storage:db-core Main com/coffiness/calfit/storage/db/core/recruitment/RecruitmentHistoryEntity.java:61 | accepted-debt |
| SB-70572ab0a7cd | DM_CONVERT_CASE | 3 / 20 | Locale-sensitive conversion | :support:email Main com/coffiness/calfit/support/email/EmailService.java:121 | accepted-debt |
| SB-a334edcebd38 | THROWS_METHOD_THROWS_RUNTIMEEXCEPTION | 3 / 19 | Existing runtime propagation | :support:email Main com/coffiness/calfit/support/email/EmailService.java:89 | accepted-debt |
| SB-f7445a4e8405 | EI_EXPOSE_REP | 2 / 18 | Configuration record | :support:security Main com/coffiness/calfit/support/security/CorsProperties.java:6 | accepted-debt |
| SB-cb20f091a3e8 | EI_EXPOSE_REP2 | 2 / 18 | Configuration record | :support:security Main com/coffiness/calfit/support/security/CorsProperties.java:7 | accepted-debt |
| SB-3ae72736c6cb | EI_EXPOSE_REP2 | 2 / 18 | Spring DI | :support:security Main com/coffiness/calfit/support/security/SecurityConfig.java:32 | not-a-defect |
| SB-0e7b8058ed52 | CT_CONSTRUCTOR_THROW | 2 / 16 | JWT fail-fast constructor | :support:security Main com/coffiness/calfit/support/security/jwt/JwtTokenProvider.java:21 | accepted-debt |
