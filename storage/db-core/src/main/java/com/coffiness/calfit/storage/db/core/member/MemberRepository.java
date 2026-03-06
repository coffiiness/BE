package com.coffiness.calfit.storage.db.core.member;

import com.coffiness.calfit.core.enums.EntityStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

  List<MemberEntity> findByStatus(EntityStatus status);

  @Query("SELECT m.tenantId FROM MemberEntity m WHERE m.userId = :userId")
  List<String> findTenantIdsByUserId(@Param("userId") Long userId);

  default Optional<String> findTenantIdByUserId(Long userId) {
    return findTenantIdsByUserId(userId).stream().findFirst();
  }

  MemberEntity findByTenantIdAndUserId(String tenantId, Long userId);

  MemberEntity findByTenantIdAndUserIdAndStatus(String tenantId, Long userId, EntityStatus status);

  boolean existsByTenantIdAndUserId(String workspaceId, Long userId);

  List<MemberEntity> findByGroupIdAndStatus(Long groupId, EntityStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<MemberEntity> findAllByTenantIdAndUserIdIn(String tenantId, List<Long> userIds);

  @Query(
      "SELECT m.groupId, COUNT(m) FROM MemberEntity m"
          + " WHERE m.groupId IN :groupIds AND m.status = :status GROUP BY m.groupId")
  List<Object[]> countByGroupIdsAndStatus(
      @Param("groupIds") List<Long> groupIds, @Param("status") EntityStatus status);
}
