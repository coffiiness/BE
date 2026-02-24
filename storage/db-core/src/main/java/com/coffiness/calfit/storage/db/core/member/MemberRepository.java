package com.coffiness.calfit.storage.db.core.member;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

  List<MemberEntity> findByTenantId(String tenantId);

  MemberEntity findByTenantIdAndUserId(String tenantId, Long userId);

  boolean existsByTenantIdAndUserId(String workspaceId, Long userId);
}
