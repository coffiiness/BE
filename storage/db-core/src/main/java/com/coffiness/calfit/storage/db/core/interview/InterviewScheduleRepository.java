package com.coffiness.calfit.storage.db.core.interview;

import com.coffiness.calfit.core.enums.InterviewStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewScheduleRepository extends JpaRepository<InterviewScheduleEntity, Long> {

  @Query(
      "SELECT e FROM InterviewScheduleEntity e "
          + "WHERE e.tenantId = :tenantId "
          + "AND e.id IN :ids "
          + "AND e.interviewStatus <> :status "
          + "AND e.scheduledAt < :end")
  List<InterviewScheduleEntity> findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
      @Param("tenantId") String tenantId,
      @Param("ids") List<Long> ids,
      @Param("status") InterviewStatus status,
      @Param("end") LocalDateTime end);

  @Query(
      "SELECT e FROM InterviewScheduleEntity e "
          + "WHERE e.tenantId = :tenantId "
          + "AND e.recruitmentId = :recruitmentId "
          + "AND e.scheduledAt >= :from "
          + "AND e.scheduledAt < :to "
          + "AND e.interviewStatus <> :status "
          + "ORDER BY e.scheduledAt ASC")
  List<InterviewScheduleEntity>
      findAllByTenantIdAndRecruitmentIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNotOrderByScheduledAtAsc(
          @Param("tenantId") String tenantId,
          @Param("recruitmentId") Long recruitmentId,
          @Param("from") LocalDateTime from,
          @Param("to") LocalDateTime to,
          @Param("status") InterviewStatus status);

  Optional<InterviewScheduleEntity> findByTenantIdAndIdAndInterviewStatusNot(
      String tenantId, Long id, InterviewStatus status);
}
