package com.coffiness.calfit.core.api.facade.notification;

import com.coffiness.calfit.core.support.Page;
import com.coffiness.calfit.domain.notification.Notification;
import com.coffiness.calfit.domain.notification.NotificationCategory;
import com.coffiness.calfit.domain.notification.NotificationPage;
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
  public NotificationPage getRecentNotifications(Long userId, int page, int size) {
    return notificationService.getRecentNotifications(userId, page, size);
  }

  @Transactional(readOnly = true)
  public NotificationPage getRecentNotifications(
      Long userId, NotificationCategory category, int page, int size) {
    return notificationService.getRecentNotifications(userId, category, page, size);
  }

  @Transactional(readOnly = true)
  public NotificationPage getUnreadNotifications(Long userId, int page, int size) {
    return notificationService.getUnreadNotifications(userId, page, size);
  }

  @Transactional(readOnly = true)
  public NotificationPage getUnreadNotifications(
      Long userId, NotificationCategory category, int page, int size) {
    return notificationService.getUnreadNotifications(userId, category, page, size);
  }

  @Transactional(readOnly = true)
  public long countUnreadNotifications(Long userId) {
    return notificationService.countUnreadNotifications(userId);
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
