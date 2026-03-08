package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitmentRepository extends JpaRepository<RecruitmentEntity, Long> {
  Page<RecruitmentEntity> findByRecruitmentStatus(
      RecruitmentStatus recruitmentStatus, Pageable pageable);

  Optional<RecruitmentEntity> findByIdAndStatus(Long id, EntityStatus status);

  @Query(
      value =
          "SELECT r.tenant_id, COUNT(*) as cnt"
              + " FROM recruitments r"
              + " WHERE r.status = 'ACTIVE' AND r.recruitment_status = 'OPEN'"
              + " GROUP BY r.tenant_id",
      nativeQuery = true)
  List<Object[]> countOpenRecruitmentsByTenant();

  @Query(
      value =
          "SELECT * FROM recruitments r"
              + " WHERE r.tenant_id = :tenantId"
              + " AND r.status = 'ACTIVE'"
              + " AND r.recruitment_status = 'OPEN'"
              + " ORDER BY r.start_date DESC",
      nativeQuery = true)
  List<RecruitmentEntity> findOpenRecruitmentsByTenantId(@Param("tenantId") String tenantId);

  @Query(
      value =
          "SELECT * FROM recruitments r"
              + " WHERE r.tenant_id = :tenantId"
              + " AND r.status = 'ACTIVE'"
              + " AND r.recruitment_status = 'OPEN'"
              + " AND LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))"
              + " ORDER BY r.start_date DESC",
      nativeQuery = true)
  List<RecruitmentEntity> findOpenRecruitmentsByTenantIdAndSearch(
      @Param("tenantId") String tenantId, @Param("search") String search);
}
