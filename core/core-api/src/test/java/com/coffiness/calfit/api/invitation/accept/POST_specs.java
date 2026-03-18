package com.coffiness.calfit.api.invitation.accept;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.support.email.EmailService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@CalfitApiTest
@DisplayName("POST /api/v1/invitations/{token}/accept")
class POST_specs {

  @MockBean private EmailService emailService;

  @Test
  @SuppressWarnings("unchecked")
  void 초대를_수락하면_워크스페이스에_멤버로_등록된다(@Autowired MemberFixture fixture) {
    MemberFixture.WorkspaceContext context = fixture.setupWorkspace();
    String inviteeEmail = fixture.userFixture().randomEmail();
    String inviteePassword = fixture.userFixture().randomPassword();
    fixture.userFixture().signUp(inviteeEmail, inviteePassword, fixture.userFixture().randomName());

    ApiResponse<InvitationResponse> created =
        fixture.createInvitation(
            context.hrToken(), context.workspaceId(), inviteeEmail, MemberType.INTERVIEWER);
    String invitationToken = created.getData().token();

    String inviteeToken =
        fixture.userFixture().login(inviteeEmail, inviteePassword).getData().accessToken();

    ApiResponse<Map> response =
        fixture
            .base()
            .post(
                "/api/v1/invitations/" + invitationToken + "/accept",
                null,
                inviteeToken,
                Map.class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).containsKey("workspaceId");
    assertThat(response.getData().get("workspaceId")).isEqualTo(context.workspaceId());
  }
}
