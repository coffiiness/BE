package com.coffiness.calfit.response;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.domain.Schedule;
import java.time.format.DateTimeFormatter;

public record ScheduleResponse(
    Long id,
    String title,
    String date,
    String startTime,
    String endTime,
    ScheduleType type,
    String description,
    Long roomId) {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public static ScheduleResponse from(Schedule schedule) {
    return new ScheduleResponse(
        schedule.id(),
        schedule.title(),
        schedule.startTime().format(DATE_FORMATTER),
        schedule.startTime().format(TIME_FORMATTER),
        schedule.endTime().format(TIME_FORMATTER),
        schedule.type(),
        schedule.description(),
        schedule.roomId());
  }
}
