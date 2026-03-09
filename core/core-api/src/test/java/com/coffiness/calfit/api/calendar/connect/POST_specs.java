package com.coffiness.calfit.api.calendar.connect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.CalendarSyncReason;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.sync.policy.CalendarSyncDecision;
import com.coffiness.calfit.domain.sync.policy.CalendarSyncPolicy;
import com.coffiness.calfit.model.GoogleTokenModel;
import com.coffiness.calfit.port.GoogleOAuthPort;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("POST /api/v1/calendars/google/connect")
public class POST_specs {

  @MockitoBean private GoogleOAuthPort googleOAuthPort;

  private final CalendarSyncPolicy calendarSyncPolicy = new CalendarSyncPolicy();

  @Test
  void 올바른_인가코드로_요청하면_캘린더_연동에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    String validAuthCode = "dummy-auth-code";

    given(googleOAuthPort.exchangeToken(validAuthCode, "http://localhost:5173/auth/callback"))
        .willReturn(new GoogleTokenModel("mock-access", "mock-refresh", 3600, "test@gmail.com"));

    // Act
    ApiResponse<String> response =
        calendarFixture.connectGoogleCalendar(token, tenantId, validAuthCode);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 인가코드가_비어있으면_에러_응답을_반환한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    String emptyAuthCode = "";

    // Act
    ApiResponse<String> response =
        calendarFixture.connectGoogleCalendar(token, tenantId, emptyAuthCode);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 구글_updatedAt이_없으면_일정을_반영한다() {
    // Arrange
    LocalDateTime calfitUpdatedAt = LocalDateTime.now();
    LocalDateTime lastCalfitPushedAt = LocalDateTime.now();

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(calfitUpdatedAt, lastCalfitPushedAt, null);

    // Assert
    assertThat(decision.apply()).isTrue();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.APPLY_MISSING_GOOGLE_TIMESTAMP);
  }

  @Test
  void calfit_푸시_직후_구글_변경은_에코로_무시한다() {
    // Arrange
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime calfitUpdatedAt = now;
    LocalDateTime lastCalfitPushedAt = now;
    ZonedDateTime googleUpdatedAt = now.plusSeconds(30).atZone(ZoneId.systemDefault());

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(
            calfitUpdatedAt, lastCalfitPushedAt, googleUpdatedAt);

    // Assert
    assertThat(decision.apply()).isFalse();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.IGNORE_ECHO_FROM_CALFIT);
  }

  @Test
  void calfit_updatedAt이_없으면_구글_일정을_반영한다() {
    // Arrange
    LocalDateTime lastCalfitPushedAt = null;
    ZonedDateTime googleUpdatedAt =
        LocalDateTime.now().plusMinutes(5).atZone(ZoneId.systemDefault());

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(null, lastCalfitPushedAt, googleUpdatedAt);

    // Assert
    assertThat(decision.apply()).isTrue();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.APPLY_CALFIT_TIMESTAMP_MISSING);
  }

  @Test
  void 구글이_명확히_최신이_아니면_일정을_무시한다() {
    // Arrange
    LocalDateTime calfitUpdatedAt = LocalDateTime.now();
    LocalDateTime lastCalfitPushedAt = null;
    ZonedDateTime googleUpdatedAt = calfitUpdatedAt.plusSeconds(3).atZone(ZoneId.systemDefault());

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(
            calfitUpdatedAt, lastCalfitPushedAt, googleUpdatedAt);

    // Assert
    assertThat(decision.apply()).isFalse();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.IGNORE_GOOGLE_NOT_NEWER_THAN_CALFIT);
  }

  @Test
  void 구글이_충분히_최신이면_일정을_반영한다() {
    // Arrange
    LocalDateTime calfitUpdatedAt = LocalDateTime.now();
    LocalDateTime lastCalfitPushedAt = null;
    ZonedDateTime googleUpdatedAt = calfitUpdatedAt.plusSeconds(8).atZone(ZoneId.systemDefault());

    // Act
    CalendarSyncDecision decision =
        calendarSyncPolicy.decideGoogleToCalfit(
            calfitUpdatedAt, lastCalfitPushedAt, googleUpdatedAt);

    // Assert
    assertThat(decision.apply()).isTrue();
    assertThat(decision.reason()).isEqualTo(CalendarSyncReason.APPLY_GOOGLE_NEWER_THAN_CALFIT);
  }
}
