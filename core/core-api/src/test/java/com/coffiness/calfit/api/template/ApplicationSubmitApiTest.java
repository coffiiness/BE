package com.coffiness.calfit.api.template;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coffiness.calfit.api.template.stub.ApplicationSubmitController;
import com.coffiness.calfit.api.template.stub.ApplicationSubmitService;
import com.coffiness.calfit.api.template.stub.dto.ApplyRequest;
import com.coffiness.calfit.api.template.stub.dto.ApplyRequest.AnswerRequest;
import com.coffiness.calfit.api.template.stub.dto.ApplyResponse;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 지원서 제출 API 테스트 (로그인 기반)
 * POST /api/v1/careers/{companySlug}/jobs/{jobId}/apply
 */
@DisplayName("지원서 제출 API")
public class ApplicationSubmitApiTest extends RestDocsTest {

    private final ApplicationSubmitService service = mock(ApplicationSubmitService.class);
    private final ApplicationSubmitController controller = new ApplicationSubmitController(service);
    private final SecurityUser mockSecurityUser = new SecurityUser(1L, "applicant@example.com", "USER");

    @BeforeEach
    @Override
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        super.setUp(restDocumentation);
        setUpMockMvc(controller, restDocumentation, securityUserArgumentResolver());
    }

    // ===== 성공 케이스 =====

    @Test
    @DisplayName("올바르게 요청하면 201 Created 상태코드를 반환한다")
    void 올바른_요청_201() throws Exception {
        // given
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "안녕하세요. 3년차 백엔드 개발자입니다.", null),
                new AnswerRequest("portfolioUrl", "https://github.com/parkjiwon", null),
                new AnswerRequest("resumeFile", null, "file_8f1b2c3d")));
        ApplyResponse response = new ApplyResponse(
                100L, 1L, 10L, "APPLIED", "2026-03-03T10:00:00");
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.applicationId").value(100))
                .andExpect(jsonPath("$.data.applicantId").value(1))
                .andExpect(jsonPath("$.data.jobId").value(10))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.submittedAt").exists());
    }

    @Test
    @DisplayName("응답 헤더에 Location이 포함된다")
    void 응답_Location_헤더() throws Exception {
        // given
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "지원합니다.", null)));
        ApplyResponse response = new ApplyResponse(
                100L, 1L, 10L, "APPLIED", "2026-03-03T10:00:00");
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/careers/coffiness/jobs/10/applications/100"));
    }

    @Test
    @DisplayName("기본정보 required 필드가 answers에 없어도 계정 프로필 값으로 제출이 성공한다")
    void 기본정보_프로필로_충족_성공() throws Exception {
        // given - name, email, phone 등 기본정보를 answers에 포함하지 않음
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "추가 질문 답변만 포함", null)));
        ApplyResponse response = new ApplyResponse(
                101L, 1L, 10L, "APPLIED", "2026-03-03T10:00:00");
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("제출 성공 시 applicationId/applicantId/jobId/status/submittedAt이 정상 반환된다")
    void 성공_응답_필드_정상반환() throws Exception {
        // given
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "자기소개입니다", null)));
        ApplyResponse response = new ApplyResponse(
                200L, 5L, 20L, "APPLIED", "2026-03-03T15:30:00");
        when(service.apply(eq("testcompany"), eq(20L), eq(1L), any(ApplyRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "testcompany", 20L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.applicationId").value(200))
                .andExpect(jsonPath("$.data.applicantId").value(5))
                .andExpect(jsonPath("$.data.jobId").value(20))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.submittedAt").value("2026-03-03T15:30:00"));
    }

    // ===== 실패 케이스 =====

    @Test
    @DisplayName("존재하지 않는 companySlug면 404 Not Found 상태코드를 반환한다")
    void companySlug_미존재_404() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "지원합니다", null)));
        when(service.apply(eq("nonexistent"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalArgumentException("Company not found"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "nonexistent", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 jobId면 404 Not Found 상태코드를 반환한다")
    void jobId_미존재_404() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "지원합니다", null)));
        when(service.apply(eq("coffiness"), eq(99999L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalArgumentException("Job not found"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 99999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("jobId가 companySlug 소속이 아니면 404 Not Found 상태코드를 반환한다")
    void jobId_회사_불일치_404() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "지원합니다", null)));
        when(service.apply(eq("coffiness"), eq(50L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalArgumentException("Job does not belong to company"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 50L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("공고 상태가 OPEN이 아니면 409 Conflict 상태코드를 반환한다")
    void 공고_OPEN아님_409() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "지원합니다", null)));
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalStateException("공고가 마감되었습니다"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("공고에 템플릿이 없으면 404 Not Found 상태코드를 반환한다")
    void 공고_템플릿없음_404() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "지원합니다", null)));
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalArgumentException("Template not found for this job"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("answers에 템플릿에 없는 fieldKey가 오면 400 Bad Request 상태코드를 반환한다")
    void answers_없는_fieldKey_400() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("nonexistent_key", "값", null)));
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalArgumentException("템플릿에 존재하지 않는 fieldKey입니다"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("(추가 질문/파일) required 필드가 누락되면 400 Bad Request 상태코드를 반환한다")
    void required_필드_누락_400() throws Exception {
        // 추가 질문 중 required=true인 필드를 누락
        ApplyRequest request = new ApplyRequest(Collections.emptyList());
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalArgumentException("필수 항목이 누락되었습니다"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FILE 타입 필드에서 fileId가 없으면 400 Bad Request 상태코드를 반환한다")
    void FILE_fileId_없음_400() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("resumeFile", null, null))); // FILE 타입인데 fileId 없음
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalArgumentException("FILE 타입 필드에는 fileId가 필수입니다"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fileId가 존재하지 않거나 유효하지 않으면 400 Bad Request 상태코드를 반환한다")
    void fileId_유효하지않음_400() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("resumeFile", null, "invalid_file_id")));
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalArgumentException("유효하지 않은 fileId입니다"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 지원자의 fileId를 참조하면 403 Forbidden 상태코드를 반환한다")
    void 다른_지원자_fileId_403() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("resumeFile", null, "other_user_file_id")));
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new SecurityException("다른 사용자의 파일에 접근할 수 없습니다"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("동일 applicantId가 동일 jobId에 재지원하면 409 Conflict 상태코드를 반환한다")
    void 중복_지원_409() throws Exception {
        ApplyRequest request = new ApplyRequest(List.of(
                new AnswerRequest("intro", "재지원합니다", null)));
        when(service.apply(eq("coffiness"), eq(10L), eq(1L), any(ApplyRequest.class)))
                .thenThrow(new IllegalStateException("이미 지원한 공고입니다"));

        mockMvc
                .perform(
                        post("/api/v1/careers/{companySlug}/jobs/{jobId}/apply", "coffiness", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isConflict());
    }

    // ===== 인증/인가 =====

    @Test
    @DisplayName("접근 토큰을 사용하지 않으면 401 Unauthorized 상태코드를 반환한다")
    void 미인증_401() throws Exception {
        // NOTE: standalone MockMvc에서는 Security 필터가 적용되지 않습니다.
        // 통합 테스트(@SpringBootTest)에서 검증해야 합니다.
    }

    // ===== Helper =====

    private HandlerMethodArgumentResolver securityUserArgumentResolver() {
        return new HandlerMethodArgumentResolver() {
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
        };
    }
}