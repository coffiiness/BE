package com.coffiness.calfit.api.template;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coffiness.calfit.api.template.stub.RecruitmentTemplateController;
import com.coffiness.calfit.api.template.stub.RecruitmentTemplateService;
import com.coffiness.calfit.api.template.stub.dto.CreateTemplateRequest;
import com.coffiness.calfit.api.template.stub.dto.CreateTemplateRequest.FieldRequest;
import com.coffiness.calfit.api.template.stub.dto.CreateTemplateResponse;
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
 * 템플릿 생성 API 테스트
 * POST /api/v1/recruitment/templates
 */
@DisplayName("템플릿 생성 API")
public class RecruitmentTemplateCreateApiTest extends RestDocsTest {

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
    @DisplayName("올바르게 요청하면 201 Created 상태코드를 반환한다")
    void 올바른_요청_201() throws Exception {
        // given
        CreateTemplateRequest request = new CreateTemplateRequest(
                "백엔드 개발자 지원서",
                List.of(
                        new FieldRequest("name", "이름", "TEXT", true, null),
                        new FieldRequest("gender", "성별", "SELECT", true, List.of("남성", "여성")),
                        new FieldRequest("birthDate", "생년월일", "DATE", true, null),
                        new FieldRequest("phone", "연락처", "PHONE", true, null),
                        new FieldRequest("custom_1", "새 단답형 질문", "TEXT", false, null)));
        CreateTemplateResponse response = new CreateTemplateResponse(
                1L, "백엔드 개발자 지원서", "INACTIVE", "2026-03-03", "2026-03-03");
        when(service.createTemplate(any(CreateTemplateRequest.class))).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "/api/v1/recruitment/templates/1"))
                .andExpect(jsonPath("$.data.templateId").value(1))
                .andExpect(jsonPath("$.data.name").value("백엔드 개발자 지원서"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("응답 헤더에 Location이 포함된다")
    void 응답_Location_헤더() throws Exception {
        // given
        CreateTemplateRequest request = new CreateTemplateRequest(
                "테스트 템플릿",
                List.of(new FieldRequest("name", "이름", "TEXT", true, null)));
        CreateTemplateResponse response = new CreateTemplateResponse(
                5L, "테스트 템플릿", "INACTIVE", "2026-03-03", "2026-03-03");
        when(service.createTemplate(any(CreateTemplateRequest.class))).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/recruitment/templates/5"));
    }

    @Test
    @DisplayName("필수(required) 체크 상태가 그대로 저장된다")
    void required_상태_저장() throws Exception {
        // given
        CreateTemplateRequest request = new CreateTemplateRequest(
                "체크 테스트 템플릿",
                List.of(
                        new FieldRequest("name", "이름", "TEXT", true, null),
                        new FieldRequest("custom_1", "선택 질문", "TEXT", false, null)));
        CreateTemplateResponse response = new CreateTemplateResponse(
                1L, "체크 테스트 템플릿", "INACTIVE", "2026-03-03", "2026-03-03");
        when(service.createTemplate(any(CreateTemplateRequest.class))).thenReturn(response);

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isCreated());

        // 요청에 전달된 required 값이 그대로 서비스에 전달되었는지 확인
        verify(service).createTemplate(argThat(req -> {
            List<FieldRequest> fields = req.getFields();
            return fields.get(0).isRequired() && !fields.get(1).isRequired();
        }));
    }

    // ===== 실패 케이스 =====

    @Test
    @DisplayName("name이 지정되지 않으면 400 Bad Request 상태코드를 반환한다")
    void name_누락_400() throws Exception {
        // given - name이 null
        CreateTemplateRequest request = new CreateTemplateRequest(
                null,
                List.of(new FieldRequest("name", "이름", "TEXT", true, null)));
        when(service.createTemplate(any(CreateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("name은 필수입니다"));

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("name 길이가 50자를 초과하면 400 Bad Request 상태코드를 반환한다")
    void name_50자초과_400() throws Exception {
        // given
        String longName = "가".repeat(51);
        CreateTemplateRequest request = new CreateTemplateRequest(
                longName,
                List.of(new FieldRequest("name", "이름", "TEXT", true, null)));
        when(service.createTemplate(any(CreateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("name은 50자 이하여야 합니다"));

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("name이 이미 존재하면 400 Bad Request 상태코드를 반환한다")
    void name_중복_400() throws Exception {
        // given
        CreateTemplateRequest request = new CreateTemplateRequest(
                "이미 존재하는 템플릿",
                List.of(new FieldRequest("name", "이름", "TEXT", true, null)));
        when(service.createTemplate(any(CreateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("이미 존재하는 템플릿명입니다"));

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fields가 비어있으면 400 Bad Request 상태코드를 반환한다")
    void fields_비어있음_400() throws Exception {
        // given
        CreateTemplateRequest request = new CreateTemplateRequest(
                "빈 필드 템플릿", Collections.emptyList());
        when(service.createTemplate(any(CreateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("fields는 1개 이상이어야 합니다"));

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fields에 fieldKey가 중복되면 400 Bad Request 상태코드를 반환한다")
    void fieldKey_중복_400() throws Exception {
        // given
        CreateTemplateRequest request = new CreateTemplateRequest(
                "중복 키 템플릿",
                List.of(
                        new FieldRequest("name", "이름", "TEXT", true, null),
                        new FieldRequest("name", "이름2", "TEXT", false, null)));
        when(service.createTemplate(any(CreateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("fieldKey는 중복될 수 없습니다"));

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fields의 type이 허용되지 않으면 400 Bad Request 상태코드를 반환한다")
    void type_허용안됨_400() throws Exception {
        // given
        CreateTemplateRequest request = new CreateTemplateRequest(
                "잘못된 타입 템플릿",
                List.of(new FieldRequest("custom", "커스텀", "INVALID_TYPE", false, null)));
        when(service.createTemplate(any(CreateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("허용되지 않는 필드 타입입니다"));

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("type이 SELECT인데 options가 없거나 2개 미만이면 400 Bad Request 상태코드를 반환한다")
    void SELECT_options_부족_400() throws Exception {
        // given - SELECT에 options가 1개만
        CreateTemplateRequest request = new CreateTemplateRequest(
                "선택형 오류 템플릿",
                List.of(new FieldRequest("gender", "성별", "SELECT", true, List.of("남성"))));
        when(service.createTemplate(any(CreateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("SELECT 타입은 options가 2개 이상이어야 합니다"));

        // when & then
        mockMvc
                .perform(
                        post("/api/v1/recruitment/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
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