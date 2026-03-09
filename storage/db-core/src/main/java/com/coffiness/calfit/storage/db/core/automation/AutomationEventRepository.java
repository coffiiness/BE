package com.coffiness.calfit.storage.db.core.automation;

import com.coffiness.calfit.core.enums.AutomationEventStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationEventRepository extends JpaRepository<AutomationEventEntity, Long> {
  List<AutomationEventEntity> findByEventStatus(AutomationEventStatus eventStatus);

  List<AutomationEventEntity> findByApplicationIdAndEventStatus(
      Long applicationId, AutomationEventStatus eventStatus);
}
