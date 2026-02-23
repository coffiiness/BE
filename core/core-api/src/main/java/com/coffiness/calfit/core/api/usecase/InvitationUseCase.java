package com.coffiness.calfit.core.api.usecase;

import com.coffiness.calfit.domain.user.User;
import com.coffiness.calfit.domain.user.UserService;
import com.coffiness.calfit.domain.workspace.invitation.Invitation;
import com.coffiness.calfit.domain.workspace.invitation.InvitationService;
import com.coffiness.calfit.domain.workspace.member.WorkspaceMemberService;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InvitationUseCase {

  private final InvitationService invitationService;
  private final UserService userService;
  private final WorkspaceMemberService workspaceMemberService;

  public InvitationUseCase(
      InvitationService invitationService,
      UserService userService,
      WorkspaceMemberService workspaceMemberService) {
    this.invitationService = invitationService;
    this.userService = userService;
    this.workspaceMemberService = workspaceMemberService;
  }

  @Transactional
  public void accept(String token) {
    // 1. 토큰 검증
    Invitation invitation = invitationService.getByToken(token);

    // 2. 해당 워크스페이스 테넌트 컨텍스트 설정
    TenantContext.setTenantId(invitation.workspaceId());

    // 3. 초대 이메일로 기존 사용자 조회
    User user = userService.getUserByEmail(invitation.email());

    // 4. 초대 상태 ACCEPTED로 변경
    invitationService.acceptInvitation(token);

    // 5. 워크스페이스 멤버 등록
    workspaceMemberService.registerMember(
        invitation.workspaceId(), user.id(), invitation.memberType());
  }
}
