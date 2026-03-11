package com.coffiness.calfit.api.calendar.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("POST /api/v1/schedules")
public class POST_specs {

  @MockitoBean private GoogleOAuthPort googleOAuthPort;
  @MockitoBean private GoogleCalendarPort googleCalendarPort;

  @Test
  void 필수_데이터를_입력하면_내_일정_생성에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "기획 회의",
            "프로젝트 초기 기획 회의입니다.",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> response = calendarFixture.createSchedule(token, tenantId, createRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 회의실을_포함하여_내_일정_생성에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    Long roomId = meetingRoomFixture.create(token, tenantId, "회의실1", 1, 10).getData().id();

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "중요한 임원 회의",
            "회의실 연동 테스트입니다.",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            roomId,
            false,
            null);

    // Act
    ApiResponse<Void> response = calendarFixture.createSchedule(token, tenantId, createRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    var detailInfo = calendarFixture.getDetailSchedule(token, tenantId, scheduleId).getData();

    assertThat(detailInfo).isNotNull();
    assertThat(detailInfo.title()).isEqualTo("중요한 임원 회의");
    assertThat(detailInfo.roomId()).isEqualTo(roomId);
  }

  @Test
  void 캘핏_일정_생성_후_구글_이벤트_ID로_재동기화하면_한건만_유지된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    String authCode = "google-auth-code-create";
    given(
            googleOAuthPort.exchangeAuthorizationCode(
                authCode, "http://localhost:5173/auth/callback"))
        .willReturn(
            new OAuthExchangeResult(
                "access-token", 3600L, "refresh-token", createIdToken("sync@test.com")));

    given(googleCalendarPort.resolveWorkspaceCalendarId(anyString(), anyString(), anyString()))
        .willReturn("workspace-calendar-id");

    ApiResponse<String> connectResponse =
        calendarFixture.connectGoogleCalendar(token, tenantId, authCode);
    assertThat(connectResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    given(
            googleCalendarPort.createEvent(
                anyString(), anyString(), anyString(), anyString(), any(), any(), anyBoolean()))
        .willReturn(new GoogleCalendarClientResult("google-created-event-1", true));

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "캘핏에서 생성한 일정",
            "아웃바운드 동기화 테스트",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(token, tenantId, createRequest);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                verify(googleCalendarPort, times(1))
                    .createEvent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        anyBoolean()));

    ScheduleSyncRequest syncRequest =
        new ScheduleSyncRequest(
            "google-created-event-1",
            "구글에서 재동기화된 제목",
            "구글에서 재동기화된 설명",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(1),
            false);

    ApiResponse<Void> syncResponse = calendarFixture.syncSchedule(token, tenantId, syncRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    var schedules = calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData();

    // Assert
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(syncResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(schedules).hasSize(1);
    assertThat(schedules.get(0).title()).isEqualTo("구글에서 재동기화된 제목");
  }

  private String createIdToken(String email) {
    String headerJson = "{\"alg\":\"none\"}";
    String payloadJson = "{\"email\":\"" + email + "\"}";

    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

    return header + "." + payload + ".signature";
  }
}
