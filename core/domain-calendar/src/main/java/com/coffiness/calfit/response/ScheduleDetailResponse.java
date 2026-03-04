package com.coffiness.calfit.response;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.domain.Schedule;
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
      Schedule schedule,
      String location,
      List<Long> attendeeIds,
      List<String> attendees,
      String applicantName) {
    return new ScheduleDetailResponse(
        schedule.id(),
        schedule.title(),
        schedule.startTime().format(DATE_FORMATTER),
        schedule.startTime().format(TIME_FORMATTER),
        schedule.endTime().format(TIME_FORMATTER),
        schedule.type(),
        schedule.description(),
        schedule.roomId(),
        location,
        attendeeIds,
        attendees,
        applicantName);
  }
}
