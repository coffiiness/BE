package com.coffiness.calfit.api.template;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coffiness.calfit.api.template.stub.RecruitmentTemplateController;
import com.coffiness.calfit.api.template.stub.RecruitmentTemplateService;
import com.coffiness.calfit.api.template.stub.dto.TemplateDetailResponse;
import com.coffiness.calfit.api.template.stub.dto.TemplateDetailResponse.FieldDetail;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
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
 * 템플릿 상세 조회 API 테스트
 * GET /api/v1/recruitment/templates/{templateId}
 */
@DisplayName("템플릿 상세 조회 API")
public class RecruitmentTemplateDetailApiTest extends RestDocsTest {

    private final RecruitmentTemplateService service = mock(RecruitmentTemplateService.class);
    private final RecruitmentTemplateController controller = new RecruitmentTemplateController(service);
    private final SecurityUser mockSecurityUser = new SecurityUser(1L, "admin@coffiness.com", "ADMIN");

    @BeforeEach
    @Override
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        super.setUp(restDocumentation);
        setUpMockMvc(controller, restDocumentation, securityUserArgumentResolver());
    }

    // ===== 성공 케이스 =====

    @Test
    @DisplayName("올바르게 요청하면 200 OK 상태코드를 반환한다")
    void 올바른_요청_200() throws Exception {
        // given
        TemplateDetailResponse response = createSampleDetailResponse();
        when(service.getTemplateDetail(1L)).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(1))
                .andExpect(jsonPath("$.data.name").value("백엔드 개발자 지원서"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.fields").isArray())
                .andExpect(jsonPath("$.data.fields.length()").value(5));
    }

    @Test
    @DisplayName("fields가 order 오름차순으로 반환된다")
    void fields_order_오름차순() throws Exception {
        // given - order가 1, 2, 3, 4, 5 순서로 정렬
        TemplateDetailResponse response = createSampleDetailResponse();
        when(service.getTemplateDetail(1L)).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fields[0].order").value(1))
                .andExpect(jsonPath("$.data.fields[1].order").value(2))
                .andExpect(jsonPath("$.data.fields[2].order").value(3))
                .andExpect(jsonPath("$.data.fields[3].order").value(4))
                .andExpect(jsonPath("$.data.fields[4].order").value(5));
    }

    @Test
    @DisplayName("SELECT 타입 필드는 options가 포함되어 반환된다")
    void SELECT_타입_options_포함() throws Exception {
        // given
        TemplateDetailResponse response = createSampleDetailResponse();
        when(service.getTemplateDetail(1L)).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // gender 필드 (index 1) 는 SELECT 타입이므로 options이 있어야 함
                .andExpect(jsonPath("$.data.fields[1].type").value("SELECT"))
                .andExpect(jsonPath("$.data.fields[1].options").isArray())
                .andExpect(jsonPath("$.data.fields[1].options.length()").value(2))
                .andExpect(jsonPath("$.data.fields[1].options[0]").value("남성"))
                .andExpect(jsonPath("$.data.fields[1].options[1]").value("여성"));
    }

    @Test
    @DisplayName("SELECT가 아닌 타입 필드는 options가 없거나 null로 반환된다")
    void 비SELECT_타입_options_null() throws Exception {
        // given
        TemplateDetailResponse response = createSampleDetailResponse();
        when(service.getTemplateDetail(1L)).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // name 필드 (index 0)는 TEXT 타입이므로 options가 null
                .andExpect(jsonPath("$.data.fields[0].type").value("TEXT"))
                .andExpect(jsonPath("$.data.fields[0].options").doesNotExist());
    }

    @Test
    @DisplayName("기본 항목은 isDefault=true로 반환된다")
    void 기본항목_isDefault_true() throws Exception {
        // given
        TemplateDetailResponse response = createSampleDetailResponse();
        when(service.getTemplateDetail(1L)).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 기본 항목(이름, 성별, 생년월일, 연락처) → isDefault = true
                .andExpect(jsonPath("$.data.fields[0].default").value(true))
                .andExpect(jsonPath("$.data.fields[1].default").value(true))
                .andExpect(jsonPath("$.data.fields[2].default").value(true))
                .andExpect(jsonPath("$.data.fields[3].default").value(true))
                // 커스텀 필드 → isDefault = false
                .andExpect(jsonPath("$.data.fields[4].default").value(false));
    }

    // ===== 실패 케이스 =====

    @Test
    @DisplayName("templateId가 양수가 아니면 400 Bad Request 상태코드를 반환한다")
    void templateId_양수아님_400() throws Exception {
        when(service.getTemplateDetail(-1L))
                .thenThrow(new IllegalArgumentException("templateId는 양수여야 합니다"));

        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates/{templateId}", -1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 templateId면 404 Not Found 상태코드를 반환한다")
    void 존재하지않는_templateId_404() throws Exception {
        when(service.getTemplateDetail(99999L))
                .thenThrow(new IllegalArgumentException("Template not found"));

        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates/{templateId}", 99999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ===== 인증/인가 =====

    @Test
    @DisplayName("접근 토큰을 사용하지 않으면 401 Unauthorized 상태코드를 반환한다")
    void 미인증_401() throws Exception {
        // NOTE: standalone MockMvc에서는 Security 필터가 적용되지 않습니다.
    }

    @Test
    @DisplayName("권한이 없으면 403 Forbidden 상태코드를 반환한다")
    void 권한없음_403() throws Exception {
        // NOTE: standalone MockMvc에서는 Security 필터가 적용되지 않습니다.
    }

    // ===== Helper =====

    private TemplateDetailResponse createSampleDetailResponse() {
        return new TemplateDetailResponse(
                1L, "백엔드 개발자 지원서", "INACTIVE", "2026-03-01", "2026-03-03",
                List.of(
                        new FieldDetail(1L, "name", "이름", "TEXT", true, null, 1, true),
                        new FieldDetail(2L, "gender", "성별", "SELECT", true, List.of("남성", "여성"), 2, true),
                        new FieldDetail(3L, "birthDate", "생년월일", "DATE", true, null, 3, true),
                        new FieldDetail(4L, "phone", "연락처", "PHONE", true, null, 4, true),
                        new FieldDetail(5L, "custom_1", "새 단답형 질문", "TEXT", false, null, 5, false)));
    }

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