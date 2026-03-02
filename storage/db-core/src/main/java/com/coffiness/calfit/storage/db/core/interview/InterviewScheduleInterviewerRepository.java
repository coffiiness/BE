package com.coffiness.calfit.storage.db.core.interview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewScheduleInterviewerRepository
    extends JpaRepository<InterviewScheduleInterviewerEntity, Long> {
  List<InterviewScheduleInterviewerEntity> findAllByTenantIdAndInterviewScheduleId(
      String tenantId, Long interviewScheduleId);

  List<InterviewScheduleInterviewerEntity> findAllByTenantIdAndUserIdIn(
      String tenantId, List<Long> userIds);

  boolean existsByTenantIdAndInterviewScheduleIdAndUserId(
      String tenantId, Long interviewScheduleId, Long userId);

  int countByTenantIdAndInterviewScheduleId(String tenantId, Long interviewScheduleId);
}
