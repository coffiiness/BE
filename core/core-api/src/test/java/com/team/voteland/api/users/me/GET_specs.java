package com.team.voteland.api.users.me;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.team.voteland.api.TestFixture;
import com.team.voteland.api.VotelandApiTest;
import com.team.voteland.domain.user.api.v1.response.UserResponse;

@VotelandApiTest
@DisplayName("GET /api/v1/users/me")
public class GET_specs {

    @Test
    void 올바르게_요청하면_200_OK_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        fixture.createUserThenSetAsDefault();

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .exchange("/api/v1/users/me", HttpMethod.GET, fixture.withAuth(), UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void 인증_토큰_없이_요청하면_401_Unauthorized_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        fixture.clearAuth();

        // Act
        ResponseEntity<UserResponse> response = fixture.client().getForEntity("/api/v1/users/me", UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void 잘못된_토큰으로_요청하면_401_Unauthorized_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        fixture.clearAuth();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth("invalid-token");

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .exchange("/api/v1/users/me", HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers),
                    UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void 내_정보를_올바르게_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String password = fixture.randomPassword();
        String name = fixture.randomName();
        fixture.signUp(email, password, name);
        fixture.login(email, password);

        fixture.createUserThenSetAsDefault();

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .exchange("/api/v1/users/me", HttpMethod.GET, fixture.withAuth(), UserResponse.class);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
    }

}