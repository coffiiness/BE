package com.coffiness.calfit.storage.db.core.payment;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingKeyRepository extends JpaRepository<BillingKeyEntity, Long> {

  Optional<BillingKeyEntity> findByWorkspaceIdAndActiveAndStatus(
      String workspaceId, boolean active, EntityStatus status);
}
