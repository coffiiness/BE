package com.coffiness.calfit.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleReader {

  List<Schedule> findOverlappingSchedules(
      Long userId, LocalDateTime startDate, LocalDateTime endDate);

  Schedule read(Long scheduleId);

  Schedule readByGoogleEventId(String googleEventId);

  List<Schedule> findByReservationId(Long reservationId);

  List<Long> readAttendeeIds(Long scheduleId);

  ScheduleDetailInfo readDetail(Long scheduleId);
}
