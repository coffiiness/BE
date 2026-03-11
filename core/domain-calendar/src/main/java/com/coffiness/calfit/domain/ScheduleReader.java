package com.coffiness.calfit.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleReader {

  List<Schedule> findOverlappingSchedules(
      Long userId, LocalDateTime startDate, LocalDateTime endDate);

  // 바쁜 일정 충돌 여부를 조회
  boolean existsBusyOverlappingScheduleConflict(
      List<Long> userIds, LocalDateTime startDate, LocalDateTime endDate);

  // 특정 일정을 제외하고 바쁜 일정 충돌 여부를 조회
  boolean existsBusyOverlappingScheduleConflictExcludingSchedule(
      Long excludedScheduleId, List<Long> userIds, LocalDateTime startDate, LocalDateTime endDate);

  Schedule read(Long scheduleId);

  Schedule readByGoogleEventId(String googleEventId);

  List<Schedule> findByReservationId(Long reservationId);

  List<Long> readAttendeeIds(Long scheduleId);

  ScheduleDetailInfo readDetail(Long scheduleId);
}
