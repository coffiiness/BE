package com.coffiness.calfit.api.calendar.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.LoginResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.response.ScheduleAvailabilityResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/schedules/availability")
public class GET_specs {

  @Test
  void 선택한_참석자의_당일_바쁜_일정_목록을_조회한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired MemberFixture memberFixture,
      @Autowired CalendarFixture calendarFixture) {

    String ownerToken = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(ownerToken).getData();
    String tenantId = workspace.workspaceId();

    String attendeeEmail = userFixture.randomEmail();
    String attendeePassword = userFixture.randomPassword();
    String attendeeName = userFixture.randomName();
    userFixture.signUp(attendeeEmail, attendeePassword, attendeeName);

    ApiResponse<LoginResponse> attendeeLoginResponse =
        userFixture.login(attendeeEmail, attendeePassword);
    String attendeeToken = attendeeLoginResponse.getData().accessToken();
    Long attendeeUserId = attendeeLoginResponse.getData().user().id();

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(ownerToken, tenantId, attendeeEmail, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), attendeeToken);

    LocalDate targetDate = LocalDate.now().plusDays(1);
    LocalDateTime startTime = targetDate.atTime(10, 0);
    LocalDateTime endTime = targetDate.atTime(11, 0);

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "참석자 기존 일정",
            "참석자가 이미 가지고 있는 바쁜 일정",
            ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            null,
            true,
            List.of());

    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(attendeeToken, tenantId, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ScheduleAvailabilityResponse> response =
        calendarFixture.getScheduleAvailability(
            ownerToken, tenantId, targetDate.toString(), List.of(attendeeUserId));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().attendeeAvailabilities()).hasSize(1);

    ScheduleAvailabilityResponse.AttendeeAvailabilityResponse attendeeAvailability =
        response.getData().attendeeAvailabilities().get(0);

    assertThat(attendeeAvailability.attendeeId()).isEqualTo(attendeeUserId);
    assertThat(attendeeAvailability.attendeeName()).isEqualTo(attendeeName);
    assertThat(attendeeAvailability.busySchedules()).hasSize(1);
    assertThat(attendeeAvailability.busySchedules().get(0).title()).isEqualTo("참석자 기존 일정");
    assertThat(attendeeAvailability.busySchedules().get(0).isAllDay()).isFalse();
  }
}
