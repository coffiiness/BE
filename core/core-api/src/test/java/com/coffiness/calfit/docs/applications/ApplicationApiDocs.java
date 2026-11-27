package com.coffiness.calfit.docs.applications;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.ApplicationStageUpdateRequest;
import com.coffiness.calfit.api.v1.response.ApplicationAnswerResponse;
import com.coffiness.calfit.api.v1.response.ApplicationDetailResponse;
import com.coffiness.calfit.api.v1.response.ApplicationFileResponse;
import com.coffiness.calfit.api.v1.response.ApplicationSummaryResponse;
import com.coffiness.calfit.core.api.controller.v1.ApplicationController;
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.application.ApplicationService;
import com.coffiness.calfit.domain.application.KanbanBoard;
import com.coffiness.calfit.domain.application.KanbanColumn;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ApplicationApiDocs extends RestDocsTest {

  private static final String WORKSPACE_ID = "workspace-123";

  private final ApplicationService applicationService = mock(ApplicationService.class);
  private final RecruitmentRepository recruitmentRepository = mock(RecruitmentRepository.class);
  private final MemberReader memberReader = mock(MemberReader.class);

  private final ApplicationController applicationController =
      new ApplicationController(applicationService, recruitmentRepository, memberReader);

  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "hr@test.com", "HR");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    when(memberReader.getMember(WORKSPACE_ID, mockSecurityUser.userId()))
        .thenReturn(new Member(1L, WORKSPACE_ID, mockSecurityUser.userId(), MemberType.HR, null));

    setUpMockMvc(
        applicationController,
        restDocumentation,
        new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(SecurityUser.class);
          }

          @Override
          public Object resolveArgument(
              MethodParameter parameter,
              ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest,
              WebDataBinderFactory binderFactory) {
            return mockSecurityUser;
          }
        });
  }

  @Test
  void 지원자_칸반_보드_조회_API_문서() throws Exception {
    when(applicationService.getKanbanBoard(anyLong())).thenReturn(mockKanbanBoard());

    performWithTenant(
            WORKSPACE_ID,
            get("/api/v1/applications/board")
                .queryParam("recruitmentId", "101")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "applications/board",
                responsePreprocessor(),
                queryParameters(parameterWithName("recruitmentId").description("채용 공고 ID")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.columns[].recruitmentProcessId").description("전형 단계 ID"),
                    fieldWithPath("data.columns[].name").description("전형 단계 이름"),
                    fieldWithPath("data.columns[].order").description("전형 단계 순서"),
                    fieldWithPath("data.columns[].stageType").description("전형 단계 타입"),
                    fieldWithPath("data.columns[].applications[].applicationId")
                        .description("지원서 ID"),
                    fieldWithPath("data.columns[].applications[].applicantId")
                        .description("지원자 ID"),
                    fieldWithPath("data.columns[].applications[].recruitmentId")
                        .description("채용 공고 ID"),
                    fieldWithPath("data.columns[].applications[].templateId")
                        .description("지원서 템플릿 ID"),
                    fieldWithPath("data.columns[].applications[].name").description("지원자 이름"),
                    fieldWithPath("data.columns[].applications[].email").description("지원자 이메일"),
                    fieldWithPath("data.columns[].applications[].phone").description("지원자 전화번호"),
                    fieldWithPath("data.columns[].applications[].createdAt").description("지원 일시"),
                    subsectionWithPath("data.columns[].applications[]")
                        .description("해당 단계의 지원서 목록")
                        .optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 지원자_단계_변경_API_문서() throws Exception {
    when(applicationService.updateStage(
            anyLong(), any(ApplicationStageUpdateRequest.class), anyLong()))
        .thenReturn(mockApplicationDetailResponse());

    performWithTenant(
            WORKSPACE_ID,
            patch("/api/v1/applications/{applicationId}/stage", 5001L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(new ApplicationStageUpdateRequest(1002L))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "applications/update-stage",
                responsePreprocessor(),
                pathParameters(parameterWithName("applicationId").description("지원서 ID")),
                requestFields(fieldWithPath("recruitmentProcessId").description("변경할 전형 단계 ID")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.applicationId").description("지원서 ID"),
                    fieldWithPath("data.applicantId").description("지원자 ID"),
                    fieldWithPath("data.recruitmentId").description("채용 공고 ID"),
                    fieldWithPath("data.recruitmentProcessId").description("현재 전형 단계 ID"),
                    fieldWithPath("data.templateId").description("지원서 템플릿 ID"),
                    fieldWithPath("data.name").description("지원자 이름"),
                    fieldWithPath("data.gender").description("성별"),
                    fieldWithPath("data.birthDate").description("생년월일"),
                    fieldWithPath("data.phone").description("전화번호"),
                    fieldWithPath("data.email").description("지원자 이메일"),
                    fieldWithPath("data.shortBio").description("자기소개 요약").optional(),
                    fieldWithPath("data.formFields").description("지원서 원본 필드"),
                    fieldWithPath("data.answers[].key").description("답변 필드 키"),
                    fieldWithPath("data.answers[].label").description("답변 항목명"),
                    fieldWithPath("data.answers[].value").description("답변 값"),
                    fieldWithPath("data.files[].fileId").description("첨부 파일 ID"),
                    fieldWithPath("data.files[].fieldKey").description("첨부 파일 필드 키"),
                    fieldWithPath("data.files[].originalFilename").description("원본 파일명"),
                    fieldWithPath("data.files[].contentType").description("파일 타입"),
                    fieldWithPath("data.files[].sizeBytes").description("파일 크기"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  private ResultActions performWithTenant(String tenantId, MockHttpServletRequestBuilder request)
      throws Exception {
    TenantContext.setTenantId(tenantId);
    try {
      return mockMvc.perform(request);
    } finally {
      TenantContext.clear();
    }
  }

  private KanbanBoard mockKanbanBoard() {
    ApplicationSummaryResponse documentApplicant =
        new ApplicationSummaryResponse(
            5001L,
            101L,
            100L,
            301L,
            "김지원",
            "applicant1@test.com",
            "010-1234-5678",
            LocalDateTime.of(2026, 3, 18, 10, 0));
    ApplicationSummaryResponse interviewApplicant =
        new ApplicationSummaryResponse(
            5002L,
            102L,
            100L,
            301L,
            "박후보",
            "applicant2@test.com",
            "010-9999-1111",
            LocalDateTime.of(2026, 3, 18, 11, 0));

    return new KanbanBoard(
        List.of(
            new KanbanColumn(
                1001L, "서류 전형", 1, RecruitmentStageType.DOCUMENT, List.of(documentApplicant)),
            new KanbanColumn(
                1002L, "면접 전형", 2, RecruitmentStageType.INTERVIEW, List.of(interviewApplicant)),
            new KanbanColumn(1003L, "최종 합격", 3, RecruitmentStageType.PASS, List.of()),
            new KanbanColumn(1004L, "불합격", 4, RecruitmentStageType.FAIL, List.of())));
  }

  private ApplicationDetailResponse mockApplicationDetailResponse() {
    return new ApplicationDetailResponse(
        5001L,
        101L,
        100L,
        1002L,
        301L,
        "김지원",
        Gender.FEMALE,
        LocalDateTime.of(1999, 1, 1, 0, 0),
        "010-1234-5678",
        "applicant1@test.com",
        "백엔드 개발 3년차입니다.",
        "{\"shortBio\":\"백엔드 개발 3년차입니다.\"}",
        List.of(new ApplicationAnswerResponse("portfolioUrl", "포트폴리오", "https://example.com")),
        List.of(
            new ApplicationFileResponse(
                9001L, "resumeFile", "resume.pdf", "application/pdf", 123456L)));
  }
}
