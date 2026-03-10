package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.core.enums.AutomationActionType;
import com.coffiness.calfit.core.enums.AutomationEventStatus;
import com.coffiness.calfit.storage.db.core.automation.AutomationEventEntity;
import com.coffiness.calfit.storage.db.core.automation.AutomationEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutomationEventExecutor {

  private final AutomationEventRepository automationEventRepository;

  @Transactional
  public void executePendingForApplication(Long applicationId) {
    if (applicationId == null) {
      return;
    }
    List<AutomationEventEntity> events =
        automationEventRepository.findByApplicationIdAndEventStatus(
            applicationId, AutomationEventStatus.PENDING);
    for (AutomationEventEntity event : events) {
      handleEvent(event);
    }
  }

  private void handleEvent(AutomationEventEntity event) {
    AutomationActionType actionType = event.getActionType();
    switch (actionType) {
      case EMAIL -> event.markFailed("EMAIL_NOT_IMPLEMENTED");
      case SLACK -> event.markFailed("SLACK_NOT_ENABLED");
      default -> event.markFailed("UNKNOWN_ACTION");
    }
  }
}
