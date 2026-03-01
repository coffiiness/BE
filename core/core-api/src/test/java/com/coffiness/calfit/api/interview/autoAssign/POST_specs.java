package com.coffiness.calfit.api.interview.autoAssign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.interview.InterviewScheduleService;
import com.coffiness.calfit.domain.interview.command.InterviewAutoAssignCommand;
import com.coffiness.calfit.domain.interview.command.model.InterviewRound;
import com.coffiness.calfit.domain.interview.command.model.InterviewSchedule;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("POST /api/v1/interviews/auto-assign")
class POST_specs {

  @MockitoBean private InterviewScheduleService interviewScheduleService;

  @Test
  void 정상_요청시_자동_배정에_성공한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    LocalDateTime start =
        LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime end = start.plusDays(1);
    InterviewSchedule mocked =
        new InterviewSchedule(
            1L,
            100L,
            200L,
            InterviewRound.SECOND,
            99L,
            start.plusHours(1),
            90,
            "2차 인터뷰",
            InterviewStatus.PENDING,
            List.of(11L, 12L),
            List.of(21L, 22L));

    given(
            interviewScheduleService.autoAssignInterviewSchedule(
                eq(tenantId), anyLong(), any(InterviewAutoAssignCommand.class)))
        .willReturn(mocked);

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewResponse> response =
        interviewFixture.autoAssign(
            token,
            tenantId,
            100L,
            200L,
            InterviewRound.SECOND,
            List.of(11L, 12L),
            List.of(21L, 22L),
            90,
            "2차 인터뷰",
            start,
            end,
            30,
            8,
            6);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().meetingRoomId()).isEqualTo(99L);
  }

  @Test
  void 인증_토큰이_없으면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String tenantId = context.workspaceId();

    LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
    LocalDateTime end = start.plusDays(1);

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewResponse> response =
        interviewFixture.autoAssign(
            null,
            tenantId,
            100L,
            200L,
            InterviewRound.SECOND,
            List.of(11L),
            List.of(21L),
            60,
            "2차 인터뷰",
            start,
            end,
            30,
            8,
            4);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 검색_시간이_없으면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewResponse> response =
        interviewFixture.autoAssign(
            token,
            tenantId,
            100L,
            200L,
            InterviewRound.SECOND,
            List.of(11L),
            List.of(21L),
            60,
            "2차 인터뷰",
            null,
            null,
            30,
            8,
            4);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
