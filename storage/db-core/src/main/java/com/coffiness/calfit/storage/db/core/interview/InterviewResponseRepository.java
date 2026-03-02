package com.coffiness.calfit.storage.db.core.interview;

import com.coffiness.calfit.core.enums.InterviewResponseStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewResponseRepository extends JpaRepository<InterviewResponseEntity, Long> {
  Optional<InterviewResponseEntity> findByTenantIdAndInterviewScheduleIdAndUserId(
      String tenantId, Long interviewScheduleId, Long userId);

  int countByTenantIdAndInterviewScheduleIdAndResponseStatus(
      String tenantId, Long interviewScheduleId, InterviewResponseStatus responseStatus);

  int countByTenantIdAndInterviewScheduleIdAndResponseStatusNot(
      String tenantId, Long interviewScheduleId, InterviewResponseStatus excludedStatus);

  void deleteByTenantIdAndInterviewScheduleId(String tenantId, Long interviewScheduleId);
}
