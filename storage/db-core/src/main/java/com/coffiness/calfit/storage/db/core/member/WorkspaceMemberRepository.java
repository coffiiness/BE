package com.coffiness.calfit.storage.db.core.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMemberEntity, Long> {

  boolean existsByTenantIdAndUserId(String tenantId, Long userId);
}
