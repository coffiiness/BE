package com.team.voteland.api.users.signup;

import com.team.voteland.api.VotelandApiTest;
import com.team.voteland.api.fixture.UserFixture;
import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.core.support.response.ResultType;
import com.team.voteland.domain.user.api.v1.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@VotelandApiTest
@DisplayName("POST /api/v1/users/signup")
public class POST_specs {

    @Test
    void 올바르게_요청하면_성공_응답을_반환한다(@Autowired UserFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String password = fixture.randomPassword();
        String name = fixture.randomName();

        // Act
        ApiResponse<UserResponse> response = fixture.signUp(email, password, name);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 이메일_형식이_올바르지_않으면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
        // Arrange & Act
        ApiResponse<UserResponse> response = fixture.signUp("invalid-email", fixture.randomPassword(),
                fixture.randomName());

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 비밀번호가_4자_미만이면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
        // Arrange & Act
        ApiResponse<UserResponse> response = fixture.signUp(fixture.randomEmail(), "123", fixture.randomName());

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 이름이_2자_미만이면_에러_응답을_반환한다(@Autowired UserFixture fixture) {
        // Arrange & Act
        ApiResponse<UserResponse> response = fixture.signUp(fixture.randomEmail(), fixture.randomPassword(), "A");

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 회원가입_성공시_사용자_정보를_올바르게_반환한다(@Autowired UserFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String name = fixture.randomName();

        // Act
        ApiResponse<UserResponse> response = fixture.signUp(email, fixture.randomPassword(), name);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().email()).isEqualTo(email);
        assertThat(response.getData().name()).isEqualTo(name);
    }

    @Test
    void 회원가입_성공시_사용자_ID를_반환한다(@Autowired UserFixture fixture) {
        // Arrange & Act
        ApiResponse<UserResponse> response = fixture.signUp();

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().id()).isNotNull();
    }

}
