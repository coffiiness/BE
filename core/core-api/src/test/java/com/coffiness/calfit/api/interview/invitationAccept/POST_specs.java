package com.coffiness.calfit.api.interview.invitationAccept;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationAcceptResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("POST /api/v1/interviews/{interviewScheduleId}/invitations/accept")
class POST_specs {

  @MockitoBean private InterviewScheduleService interviewScheduleService;

  @Test
  void 정상_요청시_수락에_성공한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewInvitationAcceptResult mocked =
        new InterviewInvitationAcceptResult(
            55L, 11L, LocalDateTime.now(), 2, 2, true, InterviewStatus.CONFIRMED);

    given(interviewScheduleService.acceptInvitation(eq(tenantId), anyLong(), eq(55L)))
        .willReturn(mocked);

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewInvitationAcceptResponse> response =
        interviewFixture.acceptInvitation(token, tenantId, 55L);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().finalized()).isTrue();
    assertThat(response.getData().status()).isEqualTo(InterviewStatus.CONFIRMED);
  }
}
