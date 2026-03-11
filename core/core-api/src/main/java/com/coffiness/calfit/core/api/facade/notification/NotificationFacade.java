package com.coffiness.calfit.core.api.facade.notification;

import com.coffiness.calfit.domain.notification.Notification;
import com.coffiness.calfit.domain.notification.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationFacade {

  private final NotificationService notificationService;

  @Transactional(readOnly = true)
  public List<Notification> getRecentNotifications(Long userId, int limit) {
    return notificationService.getRecentNotifications(userId, limit);
  }

  @Transactional
  public Notification readNotification(Long userId, Long notificationId) {
    return notificationService.markAsRead(userId, notificationId);
  }

  @Transactional
  public void readAllNotifications(Long userId) {
    notificationService.markAllAsRead(userId);
  }
}
