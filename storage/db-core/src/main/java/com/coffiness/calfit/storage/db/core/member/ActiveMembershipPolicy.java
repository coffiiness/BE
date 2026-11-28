package com.coffiness.calfit.storage.db.core.member;

import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ActiveMembershipPolicy {
  private final MemberRepository memberRepository;
  private final UserRepository userRepository;
  private final WorkspaceRepository workspaceRepository;

  @Transactional(propagation = Propagation.MANDATORY)
  public void lockUser(Long userId) {
    var users = userRepository.findAllByIdInOrderByIdAsc(List.of(userId));
    if (users.isEmpty() || !users.get(0).isActive()) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void lockAndRequireUnassigned(Long userId) {
    lockUser(userId);
    List<String> tenants = memberRepository.lockActiveTenantIdsByUserId(userId);
    if (tenants.size() > 1) {
      throw new CoreException(
          ErrorType.CONFLICT,
          "MULTIPLE_ACTIVE_MEMBERSHIPS",
          "활성 워크스페이스 소속이 중복되어 있습니다. 관리자에게 문의해 주세요.");
    }
    if (!tenants.isEmpty()) {
      String code =
          Objects.equals(TenantContext.getTenantId(), tenants.get(0))
              ? "WORKSPACE_ALREADY_JOINED"
              : "ACTIVE_WORKSPACE_EXISTS";
      throw new CoreException(
          ErrorType.CONFLICT, code, "이미 워크스페이스에 소속되어 있습니다. 기존 소속 해제 후 가입해 주세요.");
    }
  }

  public String requireCurrentTenant(Long userId) {
    String tenant =
        memberRepository
            .findTenantIdByUserId(userId)
            .orElseThrow(
                () ->
                    new CoreException(
                        ErrorType.FORBIDDEN, "ACTIVE_MEMBERSHIP_REQUIRED", "워크스페이스 소속이 필요합니다."));
    if (!Objects.equals(tenant, TenantContext.getTenantId())) {
      throw new CoreException(
          ErrorType.FORBIDDEN, "TENANT_MISMATCH", "요청한 워크스페이스와 현재 소속이 일치하지 않습니다.");
    }
    return tenant;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void lockCurrentWorkspace() {
    var workspace =
        workspaceRepository
            .findByWorkspaceIdForUpdate(TenantContext.getTenantId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    if (!workspace.isActive()) {
      throw new CoreException(ErrorType.FORBIDDEN, "WORKSPACE_INACTIVE");
    }
  }

  public void requireRegistrationWorkspace() {
    String tenant = TenantContext.getTenantId();
    if (tenant == null || "default".equals(tenant))
      throw new CoreException(ErrorType.FORBIDDEN, "ACTIVE_MEMBERSHIP_REQUIRED");
    var workspace =
        workspaceRepository
            .findByWorkspaceId(tenant)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    if (!workspace.isActive()) throw new CoreException(ErrorType.FORBIDDEN, "WORKSPACE_INACTIVE");
  }
}
