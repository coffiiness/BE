package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.CreateInvitationRequest;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.user.User;
import com.coffiness.calfit.domain.user.UserService;
import com.coffiness.calfit.domain.workspace.invitation.Invitation;
import com.coffiness.calfit.domain.workspace.invitation.InvitationService;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InvitationController {
  private final InvitationService invitationService;
  private final UserService userService;
  private final MemberService memberService;

  @PostMapping("/api/v1/workspaces/{workspaceId}/invitations")
  public ApiResponse<InvitationResponse> createInvitation(
      @PathVariable String workspaceId,
      @Valid @RequestBody CreateInvitationRequest request,
      @AuthenticationPrincipal SecurityUser securityUser) {
    Invitation invitation =
        invitationService.createInvitation(
            workspaceId, request.email(), request.memberType(), securityUser.userId());
    return ApiResponse.success(InvitationResponse.from(invitation));
  }

  @GetMapping("/api/v1/invitations/{token}")
  public ApiResponse<InvitationResponse> getInvitation(@PathVariable String token) {
    Invitation invitation = invitationService.getByToken(token);
    return ApiResponse.success(InvitationResponse.from(invitation));
  }

  @PostMapping("/api/v1/invitations/{token}/accept")
  public ApiResponse<?> acceptInvitation(@PathVariable String token) {

    Invitation invitation = invitationService.getByToken(token);

    TenantContext.setTenantId(invitation.workspaceId());

    User user = userService.getUserByEmail(invitation.email());

    invitationService.acceptInvitation(token);

    memberService.registerMember(user.id(), invitation.memberType());

    return ApiResponse.success();
  }
}
