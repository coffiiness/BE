package com.coffiness.calfit.storage.db.core.interview;

import com.coffiness.calfit.core.enums.InterviewStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewScheduleRepository extends JpaRepository<InterviewScheduleEntity, Long> {

  List<InterviewScheduleEntity> findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
      String tenantId, List<Long> ids, InterviewStatus status, LocalDateTime end);

  List<InterviewScheduleEntity>
      findAllByTenantIdAndRecruitmentIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNotOrderByScheduledAtAsc(
          String tenantId,
          Long recruitmentId,
          LocalDateTime from,
          LocalDateTime to,
          InterviewStatus status);
}
