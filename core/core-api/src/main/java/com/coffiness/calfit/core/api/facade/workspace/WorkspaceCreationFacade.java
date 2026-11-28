package com.coffiness.calfit.core.api.facade.workspace;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceService;
import com.coffiness.calfit.domain.workspace.member.MemberStore;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.member.ActiveMembershipPolicy;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class WorkspaceCreationFacade {
  private final WorkspaceService workspaceService;
  private final MemberStore memberStore;
  private final ActiveMembershipPolicy membershipPolicy;
  private final PlatformTransactionManager transactionManager;

  public Workspace create(Long userId, String name, String contactPhone, String employeeScale) {
    String workspaceId = UUID.randomUUID().toString();
    String previous = TenantContext.getTenantId();
    try {
      // The discriminator is fixed when Hibernate opens the transaction's Session.
      TenantContext.setTenantId(workspaceId);
      TransactionTemplate transactions = new TransactionTemplate(transactionManager);
      // The locked user row serializes this account; avoid unrelated-user gap locks.
      transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
      return transactions.execute(
          status -> {
            membershipPolicy.lockAndRequireUnassigned(userId);
            Workspace workspace =
                workspaceService.createWorkspace(workspaceId, name, contactPhone, employeeScale);
            memberStore.save(userId, MemberType.HR);
            return workspace;
          });
    } finally {
      if (previous == null) TenantContext.clear();
      else TenantContext.setTenantId(previous);
    }
  }
}
