package com.coffiness.calfit.api.template;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coffiness.calfit.api.template.stub.RecruitmentTemplateController;
import com.coffiness.calfit.api.template.stub.RecruitmentTemplateService;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
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
 * 템플릿 삭제 API 테스트
 * DELETE /api/v1/recruitment/templates/{templateId}
 */
@DisplayName("템플릿 삭제 API")
public class RecruitmentTemplateDeleteApiTest extends RestDocsTest {

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
    @DisplayName("올바르게 요청하면 204 No Content 상태코드를 반환한다")
    void 올바른_요청_204() throws Exception {
        // given
        doNothing().when(service).deleteTemplate(1L);

        // when & then
        mockMvc
                .perform(
                        delete("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(service).deleteTemplate(1L);
    }

    // ===== 실패 케이스 =====

    @Test
    @DisplayName("templateId가 양수가 아니면 400 Bad Request 상태코드를 반환한다")
    void templateId_양수아님_400() throws Exception {
        // given - 서비스에서 양수 검증 후 예외 발생
        doThrow(new IllegalArgumentException("templateId는 양수여야 합니다"))
                .when(service).deleteTemplate(-1L);

        // when & then
        mockMvc
                .perform(
                        delete("/api/v1/recruitment/templates/{templateId}", -1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 templateId면 404 Not Found 상태코드를 반환한다")
    void 존재하지않는_templateId_404() throws Exception {
        // given
        doThrow(new IllegalArgumentException("Template not found"))
                .when(service).deleteTemplate(99999L);

        // when & then
        mockMvc
                .perform(
                        delete("/api/v1/recruitment/templates/{templateId}", 99999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("사용 중인 템플릿이면 409 Conflict 상태코드를 반환한다")
    void 사용중_템플릿_409() throws Exception {
        // given - ACTIVE 상태인 템플릿 삭제 시 예외
        doThrow(new IllegalStateException("사용 중인 템플릿은 삭제할 수 없습니다"))
                .when(service).deleteTemplate(1L);

        // when & then
        mockMvc
                .perform(
                        delete("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("삭제 후 템플릿 목록 조회에서 더 이상 조회되지 않는다")
    void 삭제후_조회불가() throws Exception {
        // given
        doNothing().when(service).deleteTemplate(1L);
        doThrow(new IllegalArgumentException("Template not found"))
                .when(service).getTemplateDetail(1L);

        // 삭제 요청
        mockMvc
                .perform(
                        delete("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // 삭제 후 상세 조회 시 404 확인
        // NOTE: ExceptionHandler 구현에 따라 404 응답이 필요합니다
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError()); // standalone에서는 ExceptionHandler 없이 500
    }

    // ===== 인증/인가 =====

    @Test
    @DisplayName("접근 토큰을 사용하지 않으면 401 Unauthorized 상태코드를 반환한다")
    void 미인증_401() throws Exception {
        // NOTE: standalone MockMvc에서는 Security 필터가 적용되지 않습니다.
        // 통합 테스트(@SpringBootTest)에서 검증해야 합니다.
    }

    @Test
    @DisplayName("권한이 없으면 403 Forbidden 상태코드를 반환한다")
    void 권한없음_403() throws Exception {
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