package com.coffiness.calfit.domain.event;

import com.coffiness.calfit.domain.CalendarConnectionLifecycleService;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CalendarWatchStopEventHandler {
  private final CalendarConnectionLifecycleService lifecycle;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CalendarWatchStopRequestedEvent event) {
    try {
      TenantContext.setTenantId(event.tenantId());
      lifecycle.stopDisabledWatch(event);
    } finally {
      TenantContext.clear();
    }
  }
}
