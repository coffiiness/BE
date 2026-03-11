package com.coffiness.calfit.api.calendar.update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
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
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import com.coffiness.calfit.v1.response.ScheduleDetailResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("PUT /api/v1/schedules/{scheduleId}")
public class PUT_specs {

  @MockitoBean private GoogleOAuthPort googleOAuthPort;
  @MockitoBean private GoogleCalendarPort googleCalendarPort;

  @Test
  void 특정_일정의_정보를_수정할_수_있다(
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
            "수정용 회의",
            "수정용 회의 description",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(2),
            false,
            null,
            false,
            null);
    calendarFixture.createSchedule(token, tenantId, createRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "수정된 제목",
            "수정된 Description",
            ScheduleType.BUSINESS,
            now.plusDays(3),
            now.plusDays(3).plusHours(5),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    // Assert
    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ScheduleDetailResponse> detailResponse =
        calendarFixture.getDetailSchedule(token, tenantId, scheduleId);

    assertThat(detailResponse.getData().title()).isEqualTo("수정된 제목");
    assertThat(detailResponse.getData().description()).isEqualTo("수정된 Description");
    assertThat(detailResponse.getData().type()).isEqualTo(ScheduleType.BUSINESS);
  }

  @Test
  void 기존_회의실_예약을_취소하고_새로운_회의실을_예약할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    Long oldRoomId = meetingRoomFixture.create(token, tenantId, "기존 회의실", 1, 10).getData().id();
    Long newRoomId = meetingRoomFixture.create(token, tenantId, "새 회의실", 1, 10).getData().id();

    LocalDateTime now = LocalDateTime.now();

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "기존 회의",
            "기존 회의 description",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            oldRoomId,
            false,
            null);
    calendarFixture.createSchedule(token, tenantId, createRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "변경될 회의",
            "회의실도 변경됨",
            ScheduleType.MEETING,
            now.plusDays(1).plusHours(2),
            now.plusDays(1).plusHours(4),
            false,
            newRoomId,
            false,
            null);

    // Act
    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    // Assert
    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ScheduleDetailResponse> detailResponse =
        calendarFixture.getDetailSchedule(token, tenantId, scheduleId);

    assertThat(detailResponse.getData().title()).isEqualTo("변경될 회의");
    assertThat(detailResponse.getData().roomId()).isEqualTo(newRoomId);
  }

  @Test
  void 워크스페이스에_속하지_않은_참석자가_포함되면_일정_수정에_실패한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    LocalDateTime now = LocalDateTime.now();

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "수정 검증용 일정",
            "참석자 검증 테스트",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(2),
            false,
            null,
            false,
            null);
    calendarFixture.createSchedule(token, tenantId, createRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "수정 검증용 일정",
            "참석자 검증 테스트",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(2),
            false,
            null,
            false,
            List.of(999999L));

    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    assertThat(updateResponse.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 구글_이벤트_ID가_있는_일정을_수정하면_구글_일정도_수정된다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    String authCode = "google-auth-code-update";
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
        .willReturn(new GoogleCalendarClientResult("google-update-event-1", true));

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "수정 전 일정",
            "수정 전 설명",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            null);

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

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(3).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    given(
            googleCalendarPort.updateEvent(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(),
                any(),
                anyBoolean()))
        .willReturn(new GoogleCalendarClientResult("google-update-event-1", true));

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "수정 후 일정",
            "수정 후 설명",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(1),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                verify(googleCalendarPort, times(1))
                    .updateEvent(
                        anyString(),
                        anyString(),
                        eq("google-update-event-1"),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        anyBoolean()));

    // Assert
    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);
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
