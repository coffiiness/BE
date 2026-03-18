package com.coffiness.calfit.api.invitation.get;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.support.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@CalfitApiTest
@DisplayName("GET /api/v1/invitations/{token}")
class GET_specs {

  @MockBean private EmailService emailService;

  @Test
  void 유효한_토큰으로_초대_정보를_조회할_수_있다(@Autowired MemberFixture fixture) {
    MemberFixture.WorkspaceContext context = fixture.setupWorkspace();
    String inviteeEmail = fixture.userFixture().randomEmail();

    ApiResponse<InvitationResponse> created =
        fixture.createInvitation(
            context.hrToken(), context.workspaceId(), inviteeEmail, MemberType.INTERVIEWER);
    String invitationToken = created.getData().token();

    ApiResponse<InvitationResponse> response =
        fixture.base().get("/api/v1/invitations/" + invitationToken, InvitationResponse.class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().email()).isEqualTo(inviteeEmail);
    assertThat(response.getData().memberType()).isEqualTo(MemberType.INTERVIEWER);
  }

  @Test
  void 존재하지_않는_토큰으로_조회하면_에러를_반환한다(@Autowired MemberFixture fixture) {
    ApiResponse<InvitationResponse> response =
        fixture.base().get("/api/v1/invitations/invalid-token-999", InvitationResponse.class);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
