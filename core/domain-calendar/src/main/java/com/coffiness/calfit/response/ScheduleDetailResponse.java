package com.coffiness.calfit.response;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ScheduleDetailResponse(
    Long id,
    String title,
    String date,
    String startTime,
    String endTime,
    ScheduleType type,
    String description,
    Long roomId,
    String location,
    List<Long> attendeeIds,
    List<String> attendees,
    String applicantName) {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public static ScheduleDetailResponse of(
      ScheduleEntity entity,
      String location,
      List<Long> attendeeIds,
      List<String> attendees,
      String applicantName) {
    return new ScheduleDetailResponse(
        entity.getId(),
        entity.getTitle(),
        entity.getStartTime().format(DATE_FORMATTER),
        entity.getStartTime().format(TIME_FORMATTER),
        entity.getEndTime().format(TIME_FORMATTER),
        entity.getType(),
        entity.getDescription(),
        entity.getRoomId(),
        location,
        attendeeIds,
        attendees,
        applicantName);
  }
}
