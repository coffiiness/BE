package com.coffiness.calfit.storage.db.core.interview;

import com.coffiness.calfit.core.enums.InterviewEventType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewScheduleHistoryRepository
    extends JpaRepository<InterviewScheduleHistoryEntity, Long> {

  List<InterviewScheduleHistoryEntity>
      findAllByTenantIdAndInterviewScheduleIdInAndEventTypeOrderByCreatedAtAsc(
          String tenantId, List<Long> interviewScheduleIds, InterviewEventType eventType);
}
