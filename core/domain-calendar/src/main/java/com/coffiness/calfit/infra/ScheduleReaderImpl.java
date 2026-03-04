package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.Schedule;
import com.coffiness.calfit.domain.ScheduleReader;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleReaderImpl implements ScheduleReader {

  private final ScheduleRepository scheduleRepository;
  private final ScheduleAttendeeRepository scheduleAttendeeRepository;

  @Override
  public List<Schedule> findOverlappingSchedules(
      Long userId, LocalDateTime startDate, LocalDateTime endDate) {
    return scheduleRepository.findOverlappingSchedules(userId, startDate, endDate).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Schedule read(Long scheduleId) {
    ScheduleEntity entity =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));
    return toDomain(entity);
  }

  @Override
  public List<Long> readAttendeeIds(Long scheduleId) {
    return scheduleAttendeeRepository.findByScheduleId(scheduleId).stream()
        .map(ScheduleAttendeeEntity::getAttendeeId)
        .toList();
  }

  private Schedule toDomain(ScheduleEntity entity) {
    return new Schedule(
        entity.getId(),
        entity.getUserId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getType(),
        entity.getStartTime(),
        entity.getEndTime(),
        entity.isAllDay(),
        entity.getRoomId(),
        entity.isBusy(),
        entity.getGoogleEventId());
  }
}
