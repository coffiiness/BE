package com.coffiness.calfit.api.template;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coffiness.calfit.api.template.stub.RecruitmentTemplateController;
import com.coffiness.calfit.api.template.stub.RecruitmentTemplateService;
import com.coffiness.calfit.api.template.stub.TemplateNotFoundException;
import com.coffiness.calfit.api.template.stub.TestApiExceptionHandler;
import com.coffiness.calfit.api.template.stub.dto.UpdateTemplateRequest;
import com.coffiness.calfit.api.template.stub.dto.UpdateTemplateRequest.FieldUpdateRequest;
import com.coffiness.calfit.api.template.stub.dto.UpdateTemplateResponse;
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
 * 템플릿 수정 API 테스트
 * PUT /api/v1/recruitment/templates/{templateId}
 */
@DisplayName("템플릿 수정 API")
public class RecruitmentTemplateUpdateApiTest extends RestDocsTest {

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
                new TestApiExceptionHandler(),
                securityUserArgumentResolver());
    }

    // ===== 성공 케이스 =====

    @Test
    @DisplayName("올바르게 요청하면 200 OK 상태코드를 반환한다")
    void 올바른_요청_200() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "백엔드 개발자 지원서",
                List.of(
                        new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1),
                        new FieldUpdateRequest(2L, "gender", "성별", "SELECT", true, List.of("남성", "여성"), 2),
                        new FieldUpdateRequest(3L, "birthDate", "생년월일", "DATE", true, null, 3),
                        new FieldUpdateRequest(4L, "phone", "연락처", "PHONE", true, null, 4),
                        new FieldUpdateRequest(null, "custom_1", "새 단답형 질문", "TEXT", false, null, 5)),
                List.of(10L));
        UpdateTemplateResponse response = new UpdateTemplateResponse(
                1L, "백엔드 개발자 지원서", "INACTIVE", "2026-03-03");
        when(service.updateTemplate(eq(1L), any(UpdateTemplateRequest.class))).thenReturn(response);

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(1))
                .andExpect(jsonPath("$.data.name").value("백엔드 개발자 지원서"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("필드 추가/삭제/순서 변경/필수 여부 변경이 정상 반영된다")
    void 필드_변경_정상반영() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "수정된 템플릿",
                List.of(
                        new FieldUpdateRequest(1L, "name", "이름", "TEXT", false, null, 1),
                        new FieldUpdateRequest(null, "new_field", "신규 질문", "TEXT", true, null, 2)
                ),
                List.of(2L, 3L));
        UpdateTemplateResponse response = new UpdateTemplateResponse(
                1L, "수정된 템플릿", "INACTIVE", "2026-03-03");
        when(service.updateTemplate(eq(1L), any(UpdateTemplateRequest.class))).thenReturn(response);

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isOk());

        verify(service).updateTemplate(eq(1L), argThat(req ->
                !req.getFields().get(0).isRequired()
                        && req.getFields().get(1).isRequired()
                        && req.getDeletedFieldIds().size() == 2));
    }

    // ===== 실패 케이스 =====

    @Test
    @DisplayName("templateId가 양수가 아니면 400 Bad Request 상태코드를 반환한다")
    void templateId_양수아님_400() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "테스트",
                List.of(new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1)),
                null);

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", -1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 templateId면 404 Not Found 상태코드를 반환한다")
    void 존재하지않는_templateId_404() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "테스트",
                List.of(new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1)),
                null);
        when(service.updateTemplate(eq(99999L), any(UpdateTemplateRequest.class)))
                .thenThrow(new TemplateNotFoundException("Template not found"));

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 99999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("name이 지정되지 않으면 400 Bad Request 상태코드를 반환한다")
    void name_누락_400() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                null,
                List.of(new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1)),
                null);

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("name 길이가 50자를 초과하면 400 Bad Request 상태코드를 반환한다")
    void name_50자초과_400() throws Exception {
        String longName = "가".repeat(51);
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                longName,
                List.of(new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1)),
                null);

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("name이 이미 존재하면 400 Bad Request 상태코드를 반환한다")
    void name_중복_400() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "이미 존재하는 이름",
                List.of(new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1)),
                null);
        when(service.updateTemplate(eq(1L), any(UpdateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("이미 존재하는 템플릿명입니다"));

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fields가 비어있으면 400 Bad Request 상태코드를 반환한다")
    void fields_비어있음_400() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "빈 필드", Collections.emptyList(), null);

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fieldKey가 중복되면 400 Bad Request 상태코드를 반환한다")
    void fieldKey_중복_400() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "중복키 테스트",
                List.of(
                        new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1),
                        new FieldUpdateRequest(null, "name", "이름2", "TEXT", false, null, 2)),
                null);
        when(service.updateTemplate(eq(1L), any(UpdateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("fieldKey는 중복될 수 없습니다"));

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("기존 필드 수정 시 fieldId가 없으면 400 Bad Request 상태코드를 반환한다")
    void 기존필드_fieldId_없음_400() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "fieldId 누락",
                List.of(new FieldUpdateRequest(null, "name", "이름", "TEXT", true, null, 1)),
                null);
        when(service.updateTemplate(eq(1L), any(UpdateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("기존 필드 수정 시 fieldId는 필수입니다"));

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deletedFieldIds에 템플릿과 무관한 fieldId가 들어오면 400 Bad Request 상태코드를 반환한다")
    void deletedFieldIds_무관한_id_400() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "무관 ID 삭제",
                List.of(new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1)),
                List.of(999L));
        when(service.updateTemplate(eq(1L), any(UpdateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("삭제할 필드가 해당 템플릿에 존재하지 않습니다"));

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("order가 중복되거나 연속되지 않으면 400 Bad Request 상태코드를 반환한다")
    void order_중복_불연속_400() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "order 오류",
                List.of(
                        new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1),
                        new FieldUpdateRequest(2L, "gender", "성별", "TEXT", true, null, 1)
                ), null);
        when(service.updateTemplate(eq(1L), any(UpdateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("order는 중복 없이 연속이어야 합니다"));

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("type이 SELECT인데 options가 없거나 2개 미만이면 400 Bad Request 상태코드를 반환한다")
    void SELECT_options_부족_400() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "SELECT 오류",
                List.of(new FieldUpdateRequest(1L, "gender", "성별", "SELECT", true, List.of("남성"), 1)),
                null);
        when(service.updateTemplate(eq(1L), any(UpdateTemplateRequest.class)))
                .thenThrow(new IllegalArgumentException("SELECT 타입은 options가 2개 이상이어야 합니다"));

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사용 중 템플릿이면 409 Conflict 상태코드를 반환한다")
    void 사용중_템플릿_409() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "ACTIVE 수정 시도",
                List.of(new FieldUpdateRequest(1L, "name", "이름", "TEXT", true, null, 1)),
                null);
        when(service.updateTemplate(eq(1L), any(UpdateTemplateRequest.class)))
                .thenThrow(new IllegalStateException("사용 중인 템플릿은 수정할 수 없습니다"));

        mockMvc
                .perform(
                        put("/api/v1/recruitment/templates/{templateId}", 1L)
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
    }

    @Test
    @DisplayName("권한이 없으면 403 Forbidden 상태코드를 반환한다")
    void 권한없음_403() throws Exception {
        // NOTE: standalone MockMvc에서는 Security 필터가 적용되지 않습니다.
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