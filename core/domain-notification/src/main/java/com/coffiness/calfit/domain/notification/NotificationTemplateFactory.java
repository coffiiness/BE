package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.NotificationTargetType;
import com.coffiness.calfit.core.enums.NotificationType;
import com.coffiness.calfit.domain.announcementBoard.event.AnnouncementCreatedEvent;
import com.coffiness.calfit.domain.interview.event.InterviewCreatedEvent;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationCreatedEvent;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationTemplateFactory {
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private final MeetingRoomRepository meetingRoomRepository;

  public NotificationMessage buildAnnouncementCreated(AnnouncementCreatedEvent event) {
    return new NotificationMessage(
        NotificationType.ANNOUNCEMENT_CREATED,
        "[공지사항] 새 공지사항이 등록되었습니다.",
        event.title(),
        NotificationTargetType.ANNOUNCEMENT,
        event.announcementId(),
        "/announcements/" + event.announcementId());
  }

  public NotificationMessage buildInterviewCreated(InterviewCreatedEvent event) {
    String roomLabel = resolveMeetingRoomLabel(event.tenantId(), event.meetingRoomId());
    return new NotificationMessage(
        NotificationType.INTERVIEW_REQUESTED,
        "[면접 일정] 새 면접 일정이 등록되었습니다.",
        DATE_TIME_FORMATTER.format(event.scheduledAt()) + " / " + roomLabel,
        NotificationTargetType.INTERVIEW_SCHEDULE,
        event.interviewScheduleId(),
        "/recruitment/jobs/" + event.recruitmentId());
  }

  public NotificationMessage buildMeetingRoomReservationCreated(
      MeetingRoomReservationCreatedEvent event) {
    String title = event.title() == null || event.title().isBlank() ? "회의실 예약" : event.title();
    String roomLabel =
        event.meetingRoomName() == null || event.meetingRoomName().isBlank()
            ? resolveMeetingRoomLabel(event.tenantId(), event.meetingRoomId())
            : event.meetingRoomName();
    return new NotificationMessage(
        NotificationType.MEETING_ROOM_RESERVED,
        "[회의실 예약] 새 예약이 등록되었습니다.",
        title + " / " + roomLabel + " / " + DATE_TIME_FORMATTER.format(event.startDatetime()),
        NotificationTargetType.MEETING_ROOM_RESERVATION,
        event.reservationId(),
        "/meeting-rooms/calendar");
  }

  private String resolveMeetingRoomLabel(String tenantId, Long meetingRoomId) {
    if (meetingRoomId == null) {
      return "회의실 미지정";
    }

    return meetingRoomRepository
        .findByTenantIdAndIdAndStatus(tenantId, meetingRoomId, EntityStatus.ACTIVE)
        .map(meetingRoom -> meetingRoom.getName())
        .filter(name -> name != null && !name.isBlank())
        .orElse("회의실 #" + meetingRoomId);
  }
}
