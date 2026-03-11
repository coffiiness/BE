package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.NotificationType;
import java.util.EnumSet;

public enum NotificationCategory {
  ANNOUNCEMENT(EnumSet.of(NotificationType.ANNOUNCEMENT_CREATED)),
  INTERVIEW(
      EnumSet.of(
          NotificationType.INTERVIEW_REQUESTED,
          NotificationType.INTERVIEW_UPDATED,
          NotificationType.INTERVIEW_CANCELLED)),
  MEETING_ROOM(EnumSet.of(NotificationType.MEETING_ROOM_RESERVED)),
  SYSTEM(EnumSet.noneOf(NotificationType.class));

  private final EnumSet<NotificationType> types;

  NotificationCategory(EnumSet<NotificationType> types) {
    this.types = types;
  }

  public EnumSet<NotificationType> types() {
    return types.clone();
  }
}
