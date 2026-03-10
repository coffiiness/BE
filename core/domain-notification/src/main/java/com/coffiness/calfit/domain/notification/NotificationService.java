package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationReader notificationReader;
  private final NotificationStore notificationStore;

  @Transactional(readOnly = true)
  public List<Notification> getRecentNotifications(Long recipientUserId, int limit) {
    return notificationReader.getRecentNotifications(currentTenantId(), recipientUserId, sanitizeLimit(limit));
  }

  @Transactional(readOnly = true)
  public List<Notification> getUnreadNotifications(Long recipientUserId) {
    return notificationReader.getUnreadNotifications(currentTenantId(), recipientUserId);
  }

  @Transactional
  public Notification create(NotificationCreateCommand command) {
    return notificationStore.create(currentTenantId(), command);
  }

  @Transactional
  public List<Notification> createAll(List<NotificationCreateCommand> commands) {
    return notificationStore.createAll(currentTenantId(), commands);
  }

  @Transactional
  public Notification markAsRead(Long recipientUserId, Long notificationId) {
    return notificationStore.markAsRead(currentTenantId(), recipientUserId, notificationId);
  }

  @Transactional
  public void markAllAsRead(Long recipientUserId) {
    notificationStore.markAllAsRead(currentTenantId(), recipientUserId);
  }

  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return tenantId;
  }

  private int sanitizeLimit(int limit) {
    if (limit <= 0) {
      return 20;
    }
    return Math.min(limit, 100);
  }
}
