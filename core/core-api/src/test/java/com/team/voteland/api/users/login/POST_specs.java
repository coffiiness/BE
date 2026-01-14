package com.team.voteland.api.users.login;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.team.voteland.api.TestFixture;
import com.team.voteland.api.VotelandApiTest;
import com.team.voteland.domain.user.api.v1.request.LoginRequest;
import com.team.voteland.domain.user.api.v1.response.LoginResponse;

@VotelandApiTest
@DisplayName("POST /api/v1/users/login")
public class POST_specs {

    @Test
    void 올바르게_요청하면_200_OK_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String password = fixture.randomPassword();
        fixture.signUp(email, password, fixture.randomName());

        LoginRequest request = new LoginRequest(email, password);

        // Act
        ResponseEntity<LoginResponse> response = fixture.client()
            .postForEntity("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void 존재하지_않는_이메일로_로그인하면_401_Unauthorized_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent@test.com", fixture.randomPassword());

        // Act
        ResponseEntity<LoginResponse> response = fixture.client()
            .postForEntity("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void 잘못된_비밀번호로_로그인하면_401_Unauthorized_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        fixture.signUp(email, fixture.randomPassword(), fixture.randomName());

        LoginRequest request = new LoginRequest(email, "wrong-password");

        // Act
        ResponseEntity<LoginResponse> response = fixture.client()
            .postForEntity("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void 로그인_성공시_액세스_토큰을_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String password = fixture.randomPassword();
        fixture.signUp(email, password, fixture.randomName());

        LoginRequest request = new LoginRequest(email, password);

        // Act
        LoginResponse response = fixture.client().postForObject("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
    }

    @Test
    void 로그인_성공시_리프레시_토큰을_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String password = fixture.randomPassword();
        fixture.signUp(email, password, fixture.randomName());

        LoginRequest request = new LoginRequest(email, password);

        // Act
        LoginResponse response = fixture.client().postForObject("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void 로그인_성공시_사용자_정보를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String password = fixture.randomPassword();
        String name = fixture.randomName();
        fixture.signUp(email, password, name);

        LoginRequest request = new LoginRequest(email, password);

        // Act
        LoginResponse response = fixture.client().postForObject("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.user()).isNotNull();
        assertThat(response.user().email()).isEqualTo(email);
        assertThat(response.user().name()).isEqualTo(name);
    }

}
