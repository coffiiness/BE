package com.coffiness.calfit.api.recruitment.weekly;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.*;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.api.v1.response.WeeklyInterviewScheduleResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/*
 * 이번 주 면접 일정 조회 API 동작을 검증
 * */
@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/recruitments/weekly-interview-schedules")
class GET_specs {

  // HR 사용자는 워크스페이스의 이번 주 면접 일정을 모두 확인
  @Test
  void HR은_이번주_면접_일정을_조회할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewerContext interviewer =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 A");

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "A회의실", 3, 6).getData().id();

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "상반기 백엔드 채용",
            List.of(interviewer.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));

    ApiResponse<?> firstInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(101L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 10, 0),
            60,
            "1차 실무 면접");
    assertThat(firstInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<?> nextWeekInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewer.userId()),
            List.of(202L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 18, 14, 0),
            60,
            "다음 주 면접");
    assertThat(nextWeekInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<WeeklyInterviewScheduleResponse>> response =
        recruitmentFixture.getWeeklyInterviewSchedules(
            hrToken, tenantId, LocalDate.of(2030, 3, 13));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).hasSize(1);
    assertThat(response.getData().get(0).title()).isEqualTo("상반기 백엔드 채용 · 실무 면접");
    assertThat(response.getData().get(0).interviewerName()).isEqualTo("면접관 A");
    assertThat(response.getData().get(0).applicantName()).isEqualTo("지원자#101");
    assertThat(response.getData().get(0).location()).isEqualTo("A회의실 (3층)");
    assertThat(response.getData().get(0).description()).isEqualTo("1차 실무 면접");
  }

  @Test
  void 면접관은_본인에게_배정된_이번주_면접만_조회한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewerContext interviewerA =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 A");
    InterviewerContext interviewerB =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "면접관 B");

    Long meetingRoomId = meetingRoomFixture.create(hrToken, tenantId, "B회의실", 5, 8).getData().id();

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "상반기 프론트 채용",
            List.of(interviewerA.userId(), interviewerB.userId()),
            LocalDateTime.of(2030, 3, 1, 9, 0),
            LocalDateTime.of(2030, 3, 31, 18, 0));

    ApiResponse<?> interviewerAInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewerA.userId()),
            List.of(301L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 13, 9, 0),
            60,
            "A 면접");
    assertThat(interviewerAInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<?> interviewerBInterview =
        interviewFixture.create(
            hrToken,
            tenantId,
            recruitment.recruitmentId(),
            recruitment.stageId(),
            InterviewRound.FIRST,
            List.of(interviewerB.userId()),
            List.of(302L),
            meetingRoomId,
            LocalDateTime.of(2030, 3, 14, 11, 0),
            60,
            "B 면접");
    assertThat(interviewerBInterview.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<List<WeeklyInterviewScheduleResponse>> response =
        recruitmentFixture.getWeeklyInterviewSchedules(
            interviewerA.token(), tenantId, LocalDate.of(2030, 3, 13));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).hasSize(1);
    assertThat(response.getData().get(0).interviewerName()).isEqualTo("면접관 A");
    assertThat(response.getData().get(0).description()).isEqualTo("A 면접");
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

  // 면접 일정 조회에 사용할 채용 공고와 면접 단계를 생성
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

    Long stageId = recruitment.stages().get(0).id();
    return new RecruitmentContext(recruitment.id(), stageId);
  }

  private record InterviewerContext(Long userId, String token) {}

  private record RecruitmentContext(Long recruitmentId, Long stageId) {}
}
