package com.coffiness.calfit.domain;

import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

  // TODO : 유저 관련 보안 예외 처리 삽입

  private final ScheduleReader scheduleReader;
  private final ScheduleStore scheduleStore;

  public void createSchedule(Long userId, ScheduleCreateRequest request) {
    Schedule newSchedule =
        new Schedule(
            null,
            userId,
            request.title(),
            request.description(),
            request.type(),
            request.startTime(),
            request.endTime(),
            request.isAllDay() != null ? request.isAllDay() : false,
            request.roomId(),
            request.isBusy() != null ? request.isBusy() : true,
            null);

    scheduleStore.store(newSchedule, request.attendeeIds());
  }

  @Transactional(readOnly = true)
  public List<ScheduleInfo> getSchedules(
      long userId, LocalDateTime startDate, LocalDateTime endDate) {

    List<Schedule> schedules = scheduleReader.findOverlappingSchedules(userId, startDate, endDate);

    return schedules.stream().map(ScheduleInfo::from).toList();
  }

  @Transactional(readOnly = true)
  public ScheduleDetailInfo getDetailSchedule(long userId, Long scheduleId) {

    // TODO : 이 멤버가 이 일정을 볼 권한이 있는지 검증
    return scheduleReader.readDetail(scheduleId);
  }

  public ScheduleDetailInfo updateSchedule(
      long userId, Long scheduleId, @Valid ScheduleUpdateRequest request) {

    Schedule schedule = scheduleReader.read(scheduleId);

    if (!schedule.userId().equals(userId)) {
      throw new IllegalArgumentException("해당 일정을 수정할 권한이 없습니다.");
    }

    Schedule updatedSchedule =
        new Schedule(
            schedule.id(),
            schedule.userId(),
            request.title() != null ? request.title() : schedule.title(),
            request.description() != null ? request.description() : schedule.description(),
            request.type() != null ? request.type() : schedule.type(),
            request.startTime() != null ? request.startTime() : schedule.startTime(),
            request.endTime() != null ? request.endTime() : schedule.endTime(),
            request.isAllDay() != null ? request.isAllDay() : schedule.isAllDay(),
            request.roomId() != null ? request.roomId() : schedule.roomId(),
            request.isBusy() != null ? request.isBusy() : schedule.isBusy(),
            schedule.googleEventId());

    List<Long> updatedAttendeeIds =
        request.attendeeIds() != null
            ? request.attendeeIds()
            : scheduleReader.readAttendeeIds(scheduleId);

    scheduleStore.update(updatedSchedule, updatedAttendeeIds);

    return getDetailSchedule(userId, scheduleId);
  }

  public void deleteSchedule(long userId, Long scheduleId) {

    Schedule schedule = scheduleReader.read(scheduleId);

    if (!schedule.userId().equals(userId)) {
      throw new IllegalArgumentException("해당 일정을 삭제할 권한이 없습니다.");
    }

    // 참석자 삭제
    // TODO : 일정 내에 있는 참석자는 Hard vs Soft? Soft라면 Repository에 'DELETED' 검증이 되어야 할 것
    scheduleStore.delete(schedule);
  }
}
