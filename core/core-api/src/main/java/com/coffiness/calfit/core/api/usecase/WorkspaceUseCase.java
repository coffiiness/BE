package com.coffiness.calfit.core.api.usecase;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceService;
import com.coffiness.calfit.domain.workspace.member.WorkspaceMemberService;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkspaceUseCase {

  private final WorkspaceService workspaceService;
  private final WorkspaceMemberService workspaceMemberService;

  public WorkspaceUseCase(
      WorkspaceService workspaceService, WorkspaceMemberService workspaceMemberService) {
    this.workspaceService = workspaceService;
    this.workspaceMemberService = workspaceMemberService;
  }

  @Transactional
  public Workspace createWorkspace(
      String name, String contactPhone, String employeeScale, Long creatorId) {
    // 1. 워크스페이스 생성 (UUID workspaceId 발급)
    Workspace workspace = workspaceService.createWorkspace(name, contactPhone, employeeScale);

    // 2. TenantContext를 새 workspaceId로 설정
    TenantContext.setTenantId(workspace.workspaceId());

    // 3. 생성자를 HR 멤버로 등록
    workspaceMemberService.registerMember(workspace.workspaceId(), creatorId, MemberType.HR);

    return workspace;
  }
}
