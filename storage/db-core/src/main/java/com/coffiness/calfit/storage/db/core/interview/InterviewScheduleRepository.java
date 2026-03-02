package com.coffiness.calfit.storage.db.core.interview;

import com.coffiness.calfit.core.enums.InterviewStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InterviewScheduleRepository extends JpaRepository<InterviewScheduleEntity, Long> {
  Optional<InterviewScheduleEntity> findByIdAndTenantId(Long id, String tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<InterviewScheduleEntity> findAllByTenantIdAndIdIn(String tenantId, List<Long> ids);

  List<InterviewScheduleEntity> findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
      String tenantId, List<Long> scheduleIds, InterviewStatus excludedStatus, LocalDateTime to);
}
