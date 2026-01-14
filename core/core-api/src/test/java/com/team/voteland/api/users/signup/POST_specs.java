package com.team.voteland.api.users.signup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.team.voteland.api.TestFixture;
import com.team.voteland.api.VotelandApiTest;
import com.team.voteland.domain.user.api.v1.request.SignUpRequest;
import com.team.voteland.domain.user.api.v1.response.UserResponse;

@VotelandApiTest
@DisplayName("POST /api/v1/users/signup")
public class POST_specs {

    @Test
    void 올바르게_요청하면_201_Created_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        SignUpRequest request = new SignUpRequest(fixture.randomEmail(), fixture.randomPassword(),
                fixture.randomName());

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .postForEntity("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void 이메일_형식이_올바르지_않으면_400_Bad_Request_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        SignUpRequest request = new SignUpRequest("invalid-email", fixture.randomPassword(), fixture.randomName());

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .postForEntity("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void 비밀번호가_4자_미만이면_400_Bad_Request_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        SignUpRequest request = new SignUpRequest(fixture.randomEmail(), "123", fixture.randomName());

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .postForEntity("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void 이름이_2자_미만이면_400_Bad_Request_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        SignUpRequest request = new SignUpRequest(fixture.randomEmail(), fixture.randomPassword(), "A");

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .postForEntity("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void 회원가입_성공시_사용자_정보를_올바르게_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String name = fixture.randomName();
        SignUpRequest request = new SignUpRequest(email, fixture.randomPassword(), name);

        // Act
        UserResponse response = fixture.client().postForObject("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.name()).isEqualTo(name);
    }

    @Test
    void 회원가입_성공시_사용자_ID를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        SignUpRequest request = new SignUpRequest(fixture.randomEmail(), fixture.randomPassword(),
                fixture.randomName());

        // Act
        UserResponse response = fixture.client().postForObject("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
    }

}