package com.coffiness.calfit.api.interview.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.InterviewAvailabilityResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/interviews/availability")
class GET_specs {

  @Test
  void 토큰이_없으면_조회에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String tenantId = context.workspaceId();

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availability(
            null, tenantId, LocalDateTime.now().plusDays(1), List.of(), List.of());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void from_파라미터가_없으면_조회에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availabilityWithoutFrom(token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 면접관이_생성한_일반_일정도_가용시간_조회에_포함된다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    String interviewerEmail = userFixture.randomEmail();
    String interviewerPassword = userFixture.randomPassword();
    Long interviewerUserId =
        userFixture.signUp(interviewerEmail, interviewerPassword, "면접관 사용자").getData().id();
    String interviewerToken =
        userFixture.login(interviewerEmail, interviewerPassword).getData().accessToken();

    String invitationToken =
        memberFixture
            .createInvitation(hrToken, tenantId, interviewerEmail, MemberType.INTERVIEWER)
            .getData()
            .token();
    memberFixture.acceptInvitation(invitationToken, interviewerToken);

    LocalDateTime start =
        LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime end = start.plusHours(1);

    ApiResponse<Void> createScheduleResponse =
        calendarFixture.createSchedule(
            interviewerToken,
            tenantId,
            new ScheduleCreateRequest(
                "면접관 개인 일정",
                "면접 준비 시간",
                ScheduleType.MEETING,
                start,
                end,
                false,
                null,
                true,
                List.of()));

    assertThat(createScheduleResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availability(
            hrToken, tenantId, start.minusHours(1), List.of(), List.of(interviewerUserId));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().interviewerBusySlots())
        .anySatisfy(
            slot -> {
              assertThat(slot.interviewerId()).isEqualTo(interviewerUserId);
              assertThat(slot.start()).isEqualTo(start);
              assertThat(slot.end()).isEqualTo(end);
            });
  }

  @Test
  void 면접관이_공유_일정의_참석자인_경우도_가용시간_조회에_포함된다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    String interviewerEmail = userFixture.randomEmail();
    String interviewerPassword = userFixture.randomPassword();
    Long interviewerUserId =
        userFixture.signUp(interviewerEmail, interviewerPassword, "공유 일정 면접관").getData().id();
    String interviewerToken =
        userFixture.login(interviewerEmail, interviewerPassword).getData().accessToken();

    String invitationToken =
        memberFixture
            .createInvitation(hrToken, tenantId, interviewerEmail, MemberType.INTERVIEWER)
            .getData()
            .token();
    memberFixture.acceptInvitation(invitationToken, interviewerToken);

    LocalDateTime start =
        LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime end = start.plusHours(2);

    ApiResponse<Void> createScheduleResponse =
        calendarFixture.createSchedule(
            hrToken,
            tenantId,
            new ScheduleCreateRequest(
                "면접관 공유 일정",
                "면접관을 참석자로 포함한 일정",
                ScheduleType.MEETING,
                start,
                end,
                false,
                null,
                true,
                List.of(interviewerUserId)));

    assertThat(createScheduleResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availability(
            hrToken, tenantId, start.minusHours(1), List.of(), List.of(interviewerUserId));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().interviewerBusySlots())
        .anySatisfy(
            slot -> {
              assertThat(slot.interviewerId()).isEqualTo(interviewerUserId);
              assertThat(slot.start()).isEqualTo(start);
              assertThat(slot.end()).isEqualTo(end);
            });
  }
}
