package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitmentRepository extends JpaRepository<RecruitmentEntity, Long> {
  Page<RecruitmentEntity> findByRecruitmentStatus(
      RecruitmentStatus recruitmentStatus, Pageable pageable);

  Optional<RecruitmentEntity> findByIdAndStatus(Long id, EntityStatus status);

  // 시작 시간이 되면 DRAFT 상태 채용 공고를 OPEN으로 변경
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE RecruitmentEntity r
         SET r.recruitmentStatus = :toStatus
       WHERE r.status = :entityStatus
         AND r.recruitmentStatus = :fromStatus
         AND r.startDate <= :currentTime
         AND r.endDate > :currentTime
      """)
  int openScheduledRecruitments(
      @Param("currentTime") LocalDateTime currentTime,
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("fromStatus") RecruitmentStatus fromStatus,
      @Param("toStatus") RecruitmentStatus toStatus);

  // 종료된 DRAFT/OPEN 상태 채용 공고를 CLOSED로 변경
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE RecruitmentEntity r
         SET r.recruitmentStatus = :toStatus
       WHERE r.status = :entityStatus
         AND r.recruitmentStatus IN :fromStatuses
         AND r.endDate <= :currentTime
      """)
  int closeEndedRecruitments(
      @Param("currentTime") LocalDateTime currentTime,
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("fromStatuses") List<RecruitmentStatus> fromStatuses,
      @Param("toStatus") RecruitmentStatus toStatus);

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

  @Query(
      value =
          "SELECT r.tenant_id FROM recruitments r"
              + " WHERE r.id = :recruitmentId"
              + " AND r.status = 'ACTIVE'",
      nativeQuery = true)
  Optional<String> findTenantIdById(@Param("recruitmentId") Long recruitmentId);
}
