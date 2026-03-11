package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.NotificationTargetType;
import com.coffiness.calfit.core.enums.NotificationType;
import com.coffiness.calfit.domain.announcementBoard.event.AnnouncementCreatedEvent;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateFactory {

  public NotificationMessage buildAnnouncementCreated(AnnouncementCreatedEvent event) {
    return new NotificationMessage(
        NotificationType.ANNOUNCEMENT_CREATED,
        "[공지사항] 새 공지사항이 등록되었습니다.",
        event.title(),
        NotificationTargetType.ANNOUNCEMENT,
        event.announcementId(),
        "/announcements/" + event.announcementId());
  }
}
