package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.NotificationType;
import java.util.EnumSet;

public enum NotificationCategory {
  ANNOUNCEMENT(EnumSet.of(NotificationType.ANNOUNCEMENT_CREATED)),
  APPLICATION(EnumSet.of(NotificationType.APPLICATION_PROCESS_CHANGED)),
  INTERVIEW(
      EnumSet.of(
          NotificationType.INTERVIEW_REQUESTED,
          NotificationType.INTERVIEW_UPDATED,
          NotificationType.INTERVIEW_CANCELLED)),
  SYSTEM(EnumSet.noneOf(NotificationType.class));

  private final EnumSet<NotificationType> types;

  NotificationCategory(EnumSet<NotificationType> types) {
    this.types = types;
  }

  public EnumSet<NotificationType> types() {
    return types.clone();
  }
}
