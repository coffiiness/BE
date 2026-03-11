package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.NotificationTargetType;
import com.coffiness.calfit.core.enums.NotificationType;
import com.coffiness.calfit.domain.announcementBoard.event.AnnouncementCreatedEvent;
import com.coffiness.calfit.domain.application.event.ApplicationProcessChangedEvent;
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

  public NotificationMessage buildApplicationProcessChanged(ApplicationProcessChangedEvent event) {
    return new NotificationMessage(
        NotificationType.APPLICATION_PROCESS_CHANGED,
        "[칸반] 지원자 전형 단계가 변경되었습니다.",
        String.format(
            "%s님이 %s에서 %s로 이동되었습니다.",
            event.applicantName(), event.fromStageName(), event.toStageName()),
        NotificationTargetType.APPLICATION,
        event.applicationId(),
        String.format("/applications/board?recruitmentId=%d", event.recruitmentId()));
  }
}
