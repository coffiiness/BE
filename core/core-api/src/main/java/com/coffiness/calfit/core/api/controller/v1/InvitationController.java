package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.CreateInvitationRequest;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.user.User;
import com.coffiness.calfit.domain.user.UserService;
import com.coffiness.calfit.domain.workspace.invitation.Invitation;
import com.coffiness.calfit.domain.workspace.invitation.InvitationService;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.email.EmailPageService;
import com.coffiness.calfit.support.email.EmailProperties;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class InvitationController {
  private final InvitationService invitationService;
  private final UserService userService;
  private final MemberService memberService;
  private final EmailProperties emailProperties;
  private final EmailPageService emailPageService;

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

  @GetMapping(value = "/api/v1/invitations/{token}/view", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> viewInvitation(@PathVariable String token) {
    try {
      Invitation invitation = invitationService.getByToken(token);
      String page =
          emailPageService.renderInvitationPage(
              invitation.email(),
              toMemberTypeLabel(invitation.memberType()),
              invitation.expiresAt(),
              "/api/v1/invitations/" + token + "/accept",
              emailProperties.getLoginUrl());
      return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    } catch (CoreException e) {
      String page = emailPageService.renderInvitationFailurePage(emailProperties.getLoginUrl());
      return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }
  }

  @PostMapping("/api/v1/invitations/{token}/accept")
  public ApiResponse<?> acceptInvitation(@PathVariable String token) {
    Invitation invitation = invitationService.getByToken(token);

    TenantContext.setTenantId(invitation.workspaceId());
    try {
      User user = userService.getUserByEmail(invitation.email());
      invitationService.acceptInvitation(token);
      memberService.registerMember(user.id(), invitation.memberType());
      return ApiResponse.success();
    } finally {
      TenantContext.clear();
    }
  }

  private String toMemberTypeLabel(MemberType memberType) {
    return switch (memberType) {
      case HR -> "인사담당자";
      case INTERVIEWER -> "면접관";
    };
  }
}
