package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.NotificationTargetType;
import com.coffiness.calfit.core.enums.NotificationType;
import com.coffiness.calfit.domain.announcementBoard.event.AnnouncementCreatedEvent;
import com.coffiness.calfit.domain.application.event.ApplicationProcessChangedEvent;
import com.coffiness.calfit.domain.event.ScheduleAttendeeNotificationEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateFactory {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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

  // 일정 참석자 알림 생성
  public NotificationMessage buildScheduleAttendeeNotification(
      ScheduleAttendeeNotificationEvent event) {
    return switch (event.actionType()) {
      case CREATE ->
          new NotificationMessage(
              NotificationType.SCHEDULE_INVITED,
              "[일정] 새 일정이 등록되었습니다.",
              String.format("'%s' 일정이 %s에 등록되었습니다.", event.title(), formatSchedulePeriod(event)),
              NotificationTargetType.SCHEDULE,
              event.scheduleId(),
              buildScheduleActionUrl(event, true));
      case UPDATE ->
          new NotificationMessage(
              NotificationType.SCHEDULE_UPDATED,
              "[일정] 일정이 변경되었습니다.",
              String.format("'%s' 일정이 %s로 변경되었습니다.", event.title(), formatSchedulePeriod(event)),
              NotificationTargetType.SCHEDULE,
              event.scheduleId(),
              buildScheduleActionUrl(event, true));
      case DELETE ->
          new NotificationMessage(
              NotificationType.SCHEDULE_CANCELLED,
              "[일정] 일정이 취소되었습니다.",
              String.format("'%s' 일정이 취소되었습니다.", event.title()),
              NotificationTargetType.SCHEDULE,
              event.scheduleId(),
              buildScheduleActionUrl(event, false));
    };
  }

  // 일정 이동 경로 생성
  private String buildScheduleActionUrl(
      ScheduleAttendeeNotificationEvent event, boolean includeScheduleId) {
    String date = event.startTime().toLocalDate().format(DATE_FORMATTER);
    if (!includeScheduleId) {
      return "/schedule?date=" + date;
    }
    return String.format("/schedule?date=%s&scheduleId=%d", date, event.scheduleId());
  }

  // 일정 기간 문구 생성
  private String formatSchedulePeriod(ScheduleAttendeeNotificationEvent event) {
    LocalDateTime startTime = event.startTime();
    LocalDateTime endTime = event.endTime();

    if (event.isAllDay()) {
      return startTime.toLocalDate().format(DATE_FORMATTER) + " 종일";
    }

    if (startTime.toLocalDate().equals(endTime.toLocalDate())) {
      return String.format(
          "%s %s ~ %s",
          startTime.toLocalDate().format(DATE_FORMATTER),
          startTime.toLocalTime().format(TIME_FORMATTER),
          endTime.toLocalTime().format(TIME_FORMATTER));
    }

    return String.format(
        "%s ~ %s", startTime.format(DATE_TIME_FORMATTER), endTime.format(DATE_TIME_FORMATTER));
  }
}
