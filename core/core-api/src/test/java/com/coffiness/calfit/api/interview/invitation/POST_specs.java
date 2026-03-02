package com.coffiness.calfit.api.interview.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.interview.InterviewScheduleService;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitee;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CalfitApiTest
@DisplayName("POST /api/v1/interviews/{interviewScheduleId}/invitations")
class POST_specs {

  @MockitoBean private InterviewScheduleService interviewScheduleService;

  @Test
  void 정상_요청시_초대장_발송에_성공한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    InterviewInvitationResult mocked =
        new InterviewInvitationResult(
            55L,
            "면접 초대드립니다.",
            LocalDateTime.now(),
            List.of(
                new InterviewInvitee("INTERVIEWER", 11L, "면접관A", "interviewer@example.com"),
                new InterviewInvitee("APPLICANT", 21L, "지원자A", "applicant@example.com")));

    given(interviewScheduleService.sendInvitations(eq(tenantId), anyLong(), eq(55L), any()))
        .willReturn(mocked);

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewInvitationResponse> response =
        interviewFixture.sendInvitation(token, tenantId, 55L, "면접 초대드립니다.");

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().invitees()).hasSize(2);
  }

  @Test
  void 인증_토큰이_없으면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String tenantId = context.workspaceId();

    ApiResponse<com.coffiness.calfit.api.v1.response.InterviewInvitationResponse> response =
        interviewFixture.sendInvitation(null, tenantId, 55L, "면접 초대드립니다.");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
