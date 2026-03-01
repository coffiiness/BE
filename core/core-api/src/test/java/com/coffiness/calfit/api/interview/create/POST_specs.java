package com.coffiness.calfit.api.interview.create;

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
import com.coffiness.calfit.domain.interview.command.InterviewCreateCommand;
import com.coffiness.calfit.domain.interview.command.model.InterviewRound;
import com.coffiness.calfit.domain.interview.command.model.InterviewSchedule;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("POST /api/v1/interviews")
class POST_specs {

  @MockitoBean private InterviewScheduleService interviewScheduleService;

  @Test
  void 정상_요청시_면접_일정이_생성된다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    LocalDateTime scheduledAt =
        LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);
    InterviewSchedule mocked =
        new InterviewSchedule(
            1L,
            100L,
            200L,
            InterviewRound.FIRST,
            10L,
            scheduledAt,
            60,
            "1차 인터뷰",
            InterviewStatus.PENDING,
            List.of(11L, 12L),
            List.of(21L));

    given(
            interviewScheduleService.createInterviewSchedule(
                eq(tenantId), anyLong(), any(InterviewCreateCommand.class)))
        .willReturn(mocked);

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewResponse> response =
        interviewFixture.create(
            token,
            tenantId,
            100L,
            200L,
            InterviewRound.FIRST,
            List.of(11L, 12L),
            List.of(21L),
            10L,
            scheduledAt,
            60,
            "1차 인터뷰");

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().meetingRoomId()).isEqualTo(10L);
  }

  @Test
  void 인증_토큰이_없으면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String tenantId = context.workspaceId();
    LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1);

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewResponse> response =
        interviewFixture.create(
            null,
            tenantId,
            100L,
            200L,
            InterviewRound.FIRST,
            List.of(11L),
            List.of(21L),
            10L,
            scheduledAt,
            60,
            "1차 인터뷰");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 필수값이_누락되면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewResponse> response =
        interviewFixture.create(
            token,
            tenantId,
            100L,
            200L,
            InterviewRound.FIRST,
            List.of(),
            List.of(21L),
            10L,
            null,
            60,
            "1차 인터뷰");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
