package com.coffiness.calfit.domain.notification;

import java.util.List;
import java.util.Optional;

public interface NotificationReader {

  List<Notification> getRecentNotifications(String tenantId, Long recipientUserId, int limit);

  Optional<Notification> getNotification(String tenantId, Long recipientUserId, Long notificationId);

  List<Notification> getUnreadNotifications(String tenantId, Long recipientUserId);
}
