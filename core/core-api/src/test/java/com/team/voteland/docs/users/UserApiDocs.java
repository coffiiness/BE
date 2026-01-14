package com.team.voteland.docs.users;

import static com.team.voteland.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;

import com.team.voteland.docs.RestDocsTest;
import com.team.voteland.core.api.controller.v1.UserController;
import com.team.voteland.domain.user.api.v1.request.LoginRequest;
import com.team.voteland.domain.user.api.v1.request.SignUpRequest;
import com.team.voteland.domain.user.domain.User;
import com.team.voteland.domain.user.domain.UserService;

public class UserApiDocs extends RestDocsTest {

    private final UserService userService = mock(UserService.class);

    private final UserController userController = new UserController(userService);

    @BeforeEach
    @Override
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        super.setUp(restDocumentation);
        setUpMockMvc(userController, restDocumentation);
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
            .andExpect(status().isCreated())
            .andDo(document("users/signup", responsePreprocessor(),
                    requestFields(fieldWithPath("email").description("사용자 이메일"),
                            fieldWithPath("password").description("비밀번호 (4-20자)"),
                            fieldWithPath("name").description("사용자 이름 (2-50자)")),
                    responseFields(fieldWithPath("id").description("사용자 ID"),
                            fieldWithPath("email").description("사용자 이메일"), fieldWithPath("name").description("사용자 이름"),
                            fieldWithPath("role").description("사용자 역할"),
                            fieldWithPath("createdAt").description("가입 일시"))));
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
                    responseFields(fieldWithPath("accessToken").description("액세스 토큰"),
                            fieldWithPath("refreshToken").description("리프레시 토큰"),
                            fieldWithPath("user.id").description("사용자 ID"),
                            fieldWithPath("user.email").description("사용자 이메일"),
                            fieldWithPath("user.name").description("사용자 이름"),
                            fieldWithPath("user.role").description("사용자 역할"),
                            fieldWithPath("user.createdAt").description("가입 일시"))));
    }

}