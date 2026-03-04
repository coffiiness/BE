package com.coffiness.calfit.domain;

import com.coffiness.calfit.core.enums.ScheduleType;
import java.time.LocalDateTime;

public record ScheduleInfo(
    Long id,
    String title,
    LocalDateTime startTime,
    LocalDateTime endTime,
    ScheduleType type,
    String description,
    Long roomId,
    boolean isBusy) {

  public static ScheduleInfo from(Schedule schedule) {
    return new ScheduleInfo(
        schedule.id(),
        schedule.title(),
        schedule.startTime(),
        schedule.endTime(),
        schedule.type(),
        schedule.description(),
        schedule.roomId(),
        schedule.isBusy());
  }
}
