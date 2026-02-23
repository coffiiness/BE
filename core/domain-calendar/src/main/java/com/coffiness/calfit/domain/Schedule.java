package com.coffiness.calfit.domain;

import com.coffiness.calfit.core.enums.ScheduleType;
import java.time.LocalDateTime;

public record Schedule(
    Long id,
    Long userId,
    String title,
    String description,
    ScheduleType type,
    LocalDateTime startTime,
    LocalDateTime endTime,
    boolean isAllDay,
    Long roomId,
    String googleEventId) {

  public Schedule {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("일정 제목은 필수입니다.");
    }
    if (startTime == null || endTime == null) {
      throw new IllegalArgumentException("일정의 시작 시간과 종료 시간은 필수입니다.");
    }
    if (endTime.isBefore(startTime)) {
      throw new IllegalArgumentException("종료 시간이 시작 시간보다 빠를 수 없습니다.");
    }

    if (isAllDay) {
      if (startTime.getHour() != 0 || startTime.getMinute() != 0) {
        throw new IllegalArgumentException("종일 일정의 시작 시간은 00:00 이어야 합니다.");
      }
    }
  }

  public Schedule withGoogleEventId(String newGoogleEventId) {
    return new Schedule(
        this.id,
        this.userId,
        this.title,
        this.description,
        this.type,
        this.startTime,
        this.endTime,
        this.isAllDay,
        this.roomId,
        newGoogleEventId);
  }

  public Schedule updateDetails(String newTitle, String newDescription, Long newRoomId) {
    return new Schedule(
        this.id,
        this.userId,
        newTitle,
        newDescription,
        this.type,
        this.startTime,
        this.endTime,
        this.isAllDay,
        newRoomId,
        this.googleEventId);
  }

  public Schedule updateTime(
      LocalDateTime newStartTime, LocalDateTime newEndTime, boolean newIsAllDay) {
    return new Schedule(
        this.id,
        this.userId,
        this.title,
        this.description,
        this.type,
        newStartTime,
        newEndTime,
        newIsAllDay,
        this.roomId,
        this.googleEventId);
  }
}
