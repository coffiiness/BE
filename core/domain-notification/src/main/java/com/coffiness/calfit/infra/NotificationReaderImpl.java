package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.notification.Notification;
import com.coffiness.calfit.domain.notification.NotificationReader;
import com.coffiness.calfit.storage.db.core.notification.NotificationEntity;
import com.coffiness.calfit.storage.db.core.notification.NotificationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationReaderImpl implements NotificationReader {

  private final NotificationRepository notificationRepository;

  @Override
  public List<Notification> getRecentNotifications(String tenantId, Long recipientUserId, int limit) {
    return notificationRepository
        .findByTenantIdAndRecipientUserIdAndStatusOrderByCreatedAtDesc(
            tenantId, recipientUserId, EntityStatus.ACTIVE, PageRequest.of(0, limit))
        .stream()
        .map(this::toNotification)
        .toList();
  }

  @Override
  public Optional<Notification> getNotification(
      String tenantId, Long recipientUserId, Long notificationId) {
    return notificationRepository
        .findByTenantIdAndRecipientUserIdAndIdAndStatus(
            tenantId, recipientUserId, notificationId, EntityStatus.ACTIVE)
        .map(this::toNotification);
  }

  @Override
  public List<Notification> getUnreadNotifications(String tenantId, Long recipientUserId) {
    return notificationRepository
        .findByTenantIdAndRecipientUserIdAndStatusAndIsReadFalseOrderByCreatedAtDesc(
            tenantId, recipientUserId, EntityStatus.ACTIVE)
        .stream()
        .map(this::toNotification)
        .toList();
  }

  private Notification toNotification(NotificationEntity entity) {
    return new Notification(
        entity.getId(),
        entity.getRecipientUserId(),
        entity.getType(),
        entity.getTitle(),
        entity.getContent(),
        entity.getTargetType(),
        entity.getTargetId(),
        entity.getActionUrl(),
        entity.isRead(),
        entity.getReadAt(),
        entity.getCreatedAt());
  }
}
