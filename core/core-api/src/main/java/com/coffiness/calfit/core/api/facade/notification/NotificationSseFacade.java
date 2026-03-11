package com.coffiness.calfit.core.api.facade.notification;

import com.coffiness.calfit.domain.notification.NotificationSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class NotificationSseFacade {

  private final NotificationSseService notificationSseService;

  public SseEmitter subscribe(String tenantId, Long userId) {
    return notificationSseService.subscribe(tenantId, userId);
  }
}
