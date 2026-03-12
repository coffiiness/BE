package com.coffiness.calfit.api.interview.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.api.v1.response.ScheduleDetailResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("POST /api/v1/interviews")
class POST_specs {

  @Test
  void 토큰이_없으면_생성에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String tenantId = context.workspaceId();

    ApiResponse<InterviewResponse> response =
        interviewFixture.create(
            null,
            tenantId,
            1L,
            1L,
            InterviewRound.FIRST,
            List.of(1L),
            List.of(1L),
            1L,
            LocalDateTime.now().plusDays(1),
            60,
            "memo");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 면접_일정을_생성하면_면접관의_내_일정에서도_조회할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture,
      @Autowired CalendarFixture calendarFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 테스트");

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "면접실 A", 3, 6).getData().id();

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "백엔드 채용 공고",
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));

    LocalDateTime scheduledAt = LocalDateTime.of(2030, 3, 13, 14, 0);
    ApiResponse<InterviewResponse> createResponse =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(101L),
            meetingRoomId,
            scheduledAt,
            60,
            "백엔드 1차 면접");

    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<com.coffiness.calfit.v1.response.ScheduleResponse>> scheduleResponse =
        calendarFixture.getSchedules(interviewer.token(), tenantId, "2030-03-13", "2030-03-13");

    assertThat(scheduleResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(scheduleResponse.getData()).hasSize(1);
    assertThat(scheduleResponse.getData().get(0).title()).isEqualTo("백엔드 채용 공고 · 실무 면접");
    assertThat(scheduleResponse.getData().get(0).type()).isEqualTo(ScheduleType.INTERVIEW);
    assertThat(scheduleResponse.getData().get(0).interviewScheduleId())
        .isEqualTo(createResponse.getData().id());

    ApiResponse<ScheduleDetailResponse> detailResponse =
        calendarFixture.getDetailSchedule(
            interviewer.token(), tenantId, scheduleResponse.getData().get(0).id());

    assertThat(detailResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(detailResponse.getData().type()).isEqualTo(ScheduleType.INTERVIEW);
    assertThat(detailResponse.getData().location()).isEqualTo("면접실 A");
    assertThat(detailResponse.getData().description()).isEqualTo("백엔드 1차 면접");
    assertThat(detailResponse.getData().applicantName()).isEqualTo("지원자#101");
    assertThat(detailResponse.getData().interviewScheduleId())
        .isEqualTo(createResponse.getData().id());
  }

  // 테스트용 면접관 사용자를 생성하고 워크스페이스에 초대
  private InterviewerContext inviteInterviewer(
      MemberFixture memberFixture,
      UserFixture userFixture,
      String hrToken,
      String tenantId,
      String name) {
    String email = userFixture.randomEmail();
    String password = userFixture.randomPassword();
    Long userId = userFixture.signUp(email, password, name).getData().id();
    String token = userFixture.login(email, password).getData().accessToken();

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(hrToken, tenantId, email, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), token);

    return new InterviewerContext(userId, token);
  }

  // 테스트용 채용 공고와 면접 단계를 생성
  private RecruitmentContext createRecruitment(
      RecruitmentFixture recruitmentFixture,
      String token,
      String tenantId,
      String title,
      List<Long> interviewerIds,
      LocalDateTime startDate,
      LocalDateTime endDate) {
    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            title,
            1,
            1L,
            "채용 공고",
            startDate,
            endDate,
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L, 2L),
            interviewerIds,
            List.of(new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 1)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> title.equals(item.title()))
            .findFirst()
            .orElseThrow();

    return new RecruitmentContext(recruitment.id(), recruitment.stages().get(0).id());
  }

  private record InterviewerContext(Long userId, String token) {}

  private record RecruitmentContext(Long recruitmentId, Long stageId) {}
}
