package com.coffiness.calfit.api.interview.invitationDecline;

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
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationDeclineResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("POST /api/v1/interviews/{interviewScheduleId}/invitations/decline")
class POST_specs {

  @MockitoBean private InterviewScheduleService interviewScheduleService;

  @Test
  void 정상_요청시_거절에_성공한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewInvitationDeclineResult mocked =
        new InterviewInvitationDeclineResult(
            55L, 11L, "기존 일정 충돌", LocalDateTime.now(), 1, 1, 2, InterviewStatus.PENDING);

    given(
            interviewScheduleService.declineInvitation(
                eq(tenantId), anyLong(), eq(55L), any(String.class)))
        .willReturn(mocked);

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewInvitationDeclineResponse> response =
        interviewFixture.declineInvitation(token, tenantId, 55L, "기존 일정 충돌");

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().status()).isEqualTo(InterviewStatus.PENDING);
    assertThat(response.getData().declinedCount()).isEqualTo(1);
  }
}
