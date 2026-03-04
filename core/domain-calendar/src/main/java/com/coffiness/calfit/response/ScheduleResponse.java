package com.coffiness.calfit.response;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.domain.ScheduleInfo;
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

  public static ScheduleResponse from(ScheduleInfo info) {
    return new ScheduleResponse(
        info.id(),
        info.title(),
        info.startTime().format(DATE_FORMATTER),
        info.startTime().format(TIME_FORMATTER),
        info.endTime().format(TIME_FORMATTER),
        info.type(),
        info.description(),
        info.roomId());
  }
}
