package com.coffiness.calfit.domain.interview.command.model;

import java.time.LocalDateTime;

// 일정 충돌 판정용 시간 구간
public record TimeWindow(LocalDateTime start, LocalDateTime end) {

  public TimeWindow {
    if (start == null || end == null || !start.isBefore(end)) {
      throw new IllegalArgumentException("Invalid time window");
    }
  }

  // 두 시간 구간의 겹침 여부
  public boolean overlaps(TimeWindow other) {
    return start.isBefore(other.end) && other.start.isBefore(end);
  }
}
