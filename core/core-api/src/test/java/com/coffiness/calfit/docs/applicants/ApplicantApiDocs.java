package com.coffiness.calfit.docs.applicants;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.ApplicantLoginRequest;
import com.coffiness.calfit.api.v1.request.ApplicantSignUpRequest;
import com.coffiness.calfit.core.api.controller.v1.ApplicantPortalController;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.applicant.Applicant;
import com.coffiness.calfit.domain.applicant.ApplicantPortalService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;

public class ApplicantApiDocs extends RestDocsTest {

  private final ApplicantPortalService applicantPortalService = mock(ApplicantPortalService.class);

  private final ApplicantPortalController applicantPortalController =
      new ApplicantPortalController(applicantPortalService);

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(applicantPortalController, restDocumentation);
  }

  @Test
  void 지원자_회원가입_API_문서화() throws Exception {
    Applicant applicant =
        new Applicant(1L, "workspace-123", "applicant@test.com", "지원자", LocalDateTime.now());
    when(applicantPortalService.signUp(anyString(), anyString(), anyString()))
        .thenReturn(applicant);

    ApplicantSignUpRequest request =
        new ApplicantSignUpRequest("applicant@test.com", "pass1234", "지원자");

    mockMvc
        .perform(
            post("/api/public/v1/applicants/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "applicants/signup",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("email").description("지원자 이메일"),
                    fieldWithPath("password").description("지원자 비밀번호"),
                    fieldWithPath("name").description("지원자 이름")),
                responseFields(
                    fieldWithPath("result").description("결과 상태(SUCCESS/ERROR)"),
                    fieldWithPath("data.id").description("지원자 ID"),
                    fieldWithPath("data.workspaceId").description("워크스페이스 ID"),
                    fieldWithPath("data.email").description("지원자 이메일"),
                    fieldWithPath("data.name").description("지원자 이름"),
                    fieldWithPath("data.createdAt").description("생성 일시"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 지원자_로그인_API_문서화() throws Exception {
    Applicant applicant =
        new Applicant(1L, "workspace-123", "applicant@test.com", "지원자", LocalDateTime.now());
    ApplicantPortalService.ApplicantLoginResult result =
        new ApplicantPortalService.ApplicantLoginResult("access-token-example", applicant);
    when(applicantPortalService.login(anyString(), anyString())).thenReturn(result);

    ApplicantLoginRequest request = new ApplicantLoginRequest("applicant@test.com", "pass1234");

    mockMvc
        .perform(
            post("/api/public/v1/applicants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "applicants/login",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("email").description("지원자 이메일"),
                    fieldWithPath("password").description("지원자 비밀번호")),
                responseFields(
                    fieldWithPath("result").description("결과 상태(SUCCESS/ERROR)"),
                    fieldWithPath("data.accessToken").description("액세스 토큰"),
                    fieldWithPath("data.applicant.id").description("지원자 ID"),
                    fieldWithPath("data.applicant.workspaceId").description("워크스페이스 ID"),
                    fieldWithPath("data.applicant.email").description("지원자 이메일"),
                    fieldWithPath("data.applicant.name").description("지원자 이름"),
                    fieldWithPath("data.applicant.createdAt").description("생성 일시"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}
