package com.coffiness.calfit.storage.db.core.member;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

  List<MemberEntity> findByTenantIdAndStatus(String tenantId, EntityStatus status);

  MemberEntity findByTenantIdAndUserId(String tenantId, Long userId);

  boolean existsByTenantIdAndUserId(String workspaceId, Long userId);
}
