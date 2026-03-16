package com.coffiness.calfit.api.users.signup.resend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.email.EmailVerificationTokenEntity;
import com.coffiness.calfit.storage.db.core.email.EmailVerificationTokenRepository;
import com.coffiness.calfit.support.email.EmailService;
import com.coffiness.calfit.support.email.WorkspaceVerificationEmailMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@CalfitApiTest
@DisplayName("POST /api/v1/users/signup/resend-verification")
public class POST_specs {

  @MockBean private EmailService emailService;

  @Test
  void resend_verification_rotates_token_and_sends_new_email(
      @Autowired UserFixture userFixture,
      @Autowired EmailVerificationTokenRepository tokenRepository) {
    // Arrange
    String email = userFixture.randomEmail();
    String password = userFixture.randomPassword();
    String name = userFixture.randomName();
    userFixture.signUp(email, password, name);

    String firstToken = tokenRepository.findByUserEmail(email).orElseThrow().getToken();
    ArgumentCaptor<WorkspaceVerificationEmailMessage> messageCaptor =
        ArgumentCaptor.forClass(WorkspaceVerificationEmailMessage.class);

    // Act
    ApiResponse<Void> response = userFixture.resendVerificationEmail(email);
    EmailVerificationTokenEntity refreshedToken =
        tokenRepository.findByUserEmail(email).orElseThrow();

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(tokenRepository.findByToken(firstToken)).isEmpty();
    assertThat(refreshedToken.getToken()).isNotEqualTo(firstToken);

    verify(emailService, times(2))
        .sendWorkspaceVerificationEmail(anyString(), messageCaptor.capture());
    WorkspaceVerificationEmailMessage resentMessage = messageCaptor.getAllValues().get(1);

    assertThat(resentMessage.getUserEmail()).isEqualTo(email);
    assertThat(resentMessage.getVerificationToken()).isEqualTo(refreshedToken.getToken());
  }
}
