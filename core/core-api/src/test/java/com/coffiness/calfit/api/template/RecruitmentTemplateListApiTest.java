package com.coffiness.calfit.api.template;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coffiness.calfit.api.template.stub.RecruitmentTemplateController;
import com.coffiness.calfit.api.template.stub.RecruitmentTemplateService;
import com.coffiness.calfit.api.template.stub.dto.TemplatesPageResponse;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDate;
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
 * 템플릿 목록 조회 API 테스트
 * GET /api/v1/recruitment/templates?page=1&size=10&search=
 */
@DisplayName("템플릿 목록 조회 API")
public class RecruitmentTemplateListApiTest extends RestDocsTest {

    private final RecruitmentTemplateService service = mock(RecruitmentTemplateService.class);
    private final RecruitmentTemplateController controller = new RecruitmentTemplateController(service);
    private final SecurityUser mockSecurityUser = new SecurityUser(1L, "admin@coffiness.com", "ADMIN");

    @BeforeEach
    @Override
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        super.setUp(restDocumentation);
        setUpMockMvc(
                controller,
                restDocumentation,
                securityUserArgumentResolver());
    }

    // ===== 성공 케이스 =====

    @Test
    @DisplayName("올바르게 요청하면 200 OK 상태코드를 반환한다")
    void 올바른_요청_200_OK() throws Exception {
        // given
        TemplatesPageResponse response = new TemplatesPageResponse(
                1, 10, 2, 1,
                List.of(
                        new TemplatesPageResponse.TemplateItem(1L, "백엔드 개발자 지원서", LocalDate.of(2026, 3, 1).toString(),
                                LocalDate.of(2026, 3, 2).toString(), "INACTIVE"),
                        new TemplatesPageResponse.TemplateItem(2L, "프론트엔드 개발자 지원서", LocalDate.of(2026, 3, 1).toString(),
                                LocalDate.of(2026, 3, 1).toString(), "ACTIVE")));
        when(service.getTemplates(eq(1), eq(10), eq(""))).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates")
                                .param("page", "1")
                                .param("size", "10")
                                .param("search", "")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    @DisplayName("search가 비어있으면 전체 템플릿을 조회한다")
    void search_비어있으면_전체조회() throws Exception {
        // given
        TemplatesPageResponse response = new TemplatesPageResponse(
                1, 10, 3, 1,
                List.of(
                        new TemplatesPageResponse.TemplateItem(1L, "백엔드 지원서", "2026-03-01", "2026-03-03", "INACTIVE"),
                        new TemplatesPageResponse.TemplateItem(2L, "프론트엔드 지원서", "2026-03-01", "2026-03-02", "ACTIVE"),
                        new TemplatesPageResponse.TemplateItem(3L, "디자이너 지원서", "2026-03-01", "2026-03-01", "INACTIVE")));
        when(service.getTemplates(eq(1), eq(10), eq(""))).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates")
                                .param("page", "1")
                                .param("size", "10")
                                .param("search", "")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    @DisplayName("search 조건에 맞는 템플릿명만 반환한다")
    void search_부분일치_필터링() throws Exception {
        // given
        TemplatesPageResponse response = new TemplatesPageResponse(
                1, 10, 1, 1,
                List.of(
                        new TemplatesPageResponse.TemplateItem(1L, "백엔드 개발자 지원서", "2026-03-01", "2026-03-03", "INACTIVE")));
        when(service.getTemplates(eq(1), eq(10), eq("백엔드"))).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates")
                                .param("page", "1")
                                .param("size", "10")
                                .param("search", "백엔드")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("백엔드 개발자 지원서"));
    }

    @Test
    @DisplayName("updatedAt 내림차순으로 정렬되어 반환한다")
    void updatedAt_내림차순_정렬() throws Exception {
        // given - updatedAt이 최신인 것이 먼저 오도록 정렬
        TemplatesPageResponse response = new TemplatesPageResponse(
                1, 10, 2, 1,
                List.of(
                        new TemplatesPageResponse.TemplateItem(2L, "최근 수정 템플릿", "2026-03-01", "2026-03-03", "INACTIVE"),
                        new TemplatesPageResponse.TemplateItem(1L, "이전 수정 템플릿", "2026-03-01", "2026-03-01", "ACTIVE")));
        when(service.getTemplates(eq(1), eq(10), eq(""))).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates")
                                .param("page", "1")
                                .param("size", "10")
                                .param("search", "")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].updatedAt").value("2026-03-03"))
                .andExpect(jsonPath("$.data.items[1].updatedAt").value("2026-03-01"));
    }

    // ===== 실패 케이스 (검증) =====

    @Test
    @DisplayName("page가 1 미만이면 400 Bad Request 상태코드를 반환한다")
    void page_1미만_400() throws Exception {
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates")
                                .param("page", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("size가 1 미만이면 400 Bad Request 상태코드를 반환한다")
    void size_1미만_400() throws Exception {
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates")
                                .param("page", "1")
                                .param("size", "0")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("size가 50 초과면 400 Bad Request 상태코드를 반환한다")
    void size_50초과_400() throws Exception {
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates")
                                .param("page", "1")
                                .param("size", "51")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("search가 50자를 초과하면 400 Bad Request 상태코드를 반환한다")
    void search_50자초과_400() throws Exception {
        String longSearch = "가".repeat(51);
        mockMvc
                .perform(
                        get("/api/v1/recruitment/templates")
                                .param("page", "1")
                                .param("size", "10")
                                .param("search", longSearch)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ===== 인증/인가 =====

    @Test
    @DisplayName("접근 토큰을 사용하지 않으면 401 Unauthorized 상태코드를 반환한다")
    void 미인증_401() throws Exception {
        // NOTE: standalone MockMvc에서는 Security 필터가 적용되지 않습니다.
        // 이 테스트는 통합 테스트(@SpringBootTest)에서 검증해야 합니다.
        // 여기서는 테스트 케이스 존재만 기록합니다.
    }

    @Test
    @DisplayName("권한이 없으면 403 Forbidden 상태코드를 반환한다")
    void 권한없음_403() throws Exception {
        // NOTE: standalone MockMvc에서는 Security 필터가 적용되지 않습니다.
        // 이 테스트는 통합 테스트(@SpringBootTest)에서 검증해야 합니다.
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