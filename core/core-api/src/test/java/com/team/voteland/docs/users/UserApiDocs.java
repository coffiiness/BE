package com.team.voteland.docs.users;

import com.team.voteland.core.api.controller.v1.UserController;
import com.team.voteland.docs.RestDocsTest;
import com.team.voteland.domain.user.api.v1.request.LoginRequest;
import com.team.voteland.domain.user.api.v1.request.SignUpRequest;
import com.team.voteland.domain.user.domain.User;
import com.team.voteland.domain.user.domain.UserService;
import com.team.voteland.support.security.jwt.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;

import static com.team.voteland.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserApiDocs extends RestDocsTest {

    private final UserService userService = mock(UserService.class);

    private final UserController userController = new UserController(userService);

    private final SecurityUser mockSecurityUser = new SecurityUser(1L, "test@example.com", "USER");

    @BeforeEach
    @Override
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        super.setUp(restDocumentation);
        setUpMockMvcWithSecurityUser(userController, restDocumentation);
    }

    private void setUpMockMvcWithSecurityUser(Object controller, RestDocumentationContextProvider restDocumentation) {
        setUpMockMvc(controller, restDocumentation, new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(SecurityUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockSecurityUser;
            }
        });
    }

    @Test
    void 회원가입_API_문서화() throws Exception {
        // given
        User user = new User(1L, "test@example.com", "홍길동", "USER", LocalDateTime.now());
        when(userService.signUp(anyString(), anyString(), anyString())).thenReturn(user);

        SignUpRequest request = new SignUpRequest("test@example.com", "password123", "홍길동");

        // when & then
        mockMvc
            .perform(post("/api/v1/users/signup").contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isOk())
            .andDo(document("users/signup", responsePreprocessor(),
                    requestFields(fieldWithPath("email").description("사용자 이메일"),
                            fieldWithPath("password").description("비밀번호 (4-20자)"),
                            fieldWithPath("name").description("사용자 이름 (2-50자)")),
                    responseFields(fieldWithPath("result").description("결과 타입 (SUCCESS/ERROR)"),
                            fieldWithPath("data.id").description("사용자 ID"),
                            fieldWithPath("data.email").description("사용자 이메일"),
                            fieldWithPath("data.name").description("사용자 이름"),
                            fieldWithPath("data.role").description("사용자 역할"),
                            fieldWithPath("data.createdAt").description("가입 일시"),
                            fieldWithPath("error").description("에러 정보").optional())));
    }

    @Test
    void 로그인_API_문서화() throws Exception {
        // given
        User user = new User(1L, "test@example.com", "홍길동", "USER", LocalDateTime.now());
        UserService.LoginResult loginResult = new UserService.LoginResult("access-token-example",
                "refresh-token-example", user);
        when(userService.login(anyString(), anyString())).thenReturn(loginResult);

        LoginRequest request = new LoginRequest("test@example.com", "password123");

        // when & then
        mockMvc
            .perform(post("/api/v1/users/login").contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isOk())
            .andDo(document("users/login", responsePreprocessor(),
                    requestFields(fieldWithPath("email").description("사용자 이메일"),
                            fieldWithPath("password").description("비밀번호")),
                    responseFields(fieldWithPath("result").description("결과 타입 (SUCCESS/ERROR)"),
                            fieldWithPath("data.accessToken").description("액세스 토큰"),
                            fieldWithPath("data.refreshToken").description("리프레시 토큰"),
                            fieldWithPath("data.user.id").description("사용자 ID"),
                            fieldWithPath("data.user.email").description("사용자 이메일"),
                            fieldWithPath("data.user.name").description("사용자 이름"),
                            fieldWithPath("data.user.role").description("사용자 역할"),
                            fieldWithPath("data.user.createdAt").description("가입 일시"),
                            fieldWithPath("error").description("에러 정보").optional())));
    }

    @Test
    void 내정보조회_API_문서화() throws Exception {
        // given
        User user = new User(1L, "test@example.com", "홍길동", "USER", LocalDateTime.now());
        when(userService.getUser(anyLong())).thenReturn(user);

        // when & then
        mockMvc
            .perform(get("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("users/me", responsePreprocessor(), responseFields(
                    fieldWithPath("result").description("결과 타입 (SUCCESS/ERROR)"),
                    fieldWithPath("data.id").description("사용자 ID"), fieldWithPath("data.email").description("사용자 이메일"),
                    fieldWithPath("data.name").description("사용자 이름"), fieldWithPath("data.role").description("사용자 역할"),
                    fieldWithPath("data.createdAt").description("가입 일시"),
                    fieldWithPath("error").description("에러 정보").optional())));
    }

    @Test
    void 회원탈퇴_API_문서화() throws Exception {
        // given
        doNothing().when(userService).deleteUser(anyLong());

        // when & then
        mockMvc
            .perform(delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("users/delete", responsePreprocessor(),
                    responseFields(fieldWithPath("result").description("결과 타입 (SUCCESS/ERROR)"),
                            fieldWithPath("data").description("응답 데이터").optional(),
                            fieldWithPath("error").description("에러 정보").optional())));
    }

}