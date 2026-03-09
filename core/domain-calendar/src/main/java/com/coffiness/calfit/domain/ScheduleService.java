package com.coffiness.calfit.domain;

import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
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

  public void createSchedule(Long memberId, Long reservationId, ScheduleCreateRequest request) {
    Schedule newSchedule =
        new Schedule(
            null,
            memberId,
            request.title(),
            request.description(),
            request.type(),
            request.startTime(),
            request.endTime(),
            request.isAllDay() != null ? request.isAllDay() : false,
            request.roomId(),
            reservationId,
            request.isBusy() != null ? request.isBusy() : true,
            null);

    scheduleStore.store(newSchedule, request.attendeeIds());
  }

  @Transactional(readOnly = true)
  public List<ScheduleInfo> getSchedules(
      long memberId, LocalDateTime startDate, LocalDateTime endDate) {

    List<Schedule> schedules =
        scheduleReader.findOverlappingSchedules(memberId, startDate, endDate);

    return schedules.stream().map(ScheduleInfo::from).toList();
  }

  @Transactional(readOnly = true)
  public ScheduleDetailInfo getDetailSchedule(long memberId, Long scheduleId) {

    Schedule schedule = scheduleReader.read(scheduleId);
    List<Long> attendeeIds = scheduleReader.readAttendeeIds(scheduleId);

    boolean isOwner = schedule.memberId().equals(memberId);
    boolean isAttendee = attendeeIds.contains(memberId);

    if (!isOwner && !isAttendee) {
      throw new IllegalArgumentException("해당 일정을 조회할 권한이 없습니다.");
    }

    return scheduleReader.readDetail(scheduleId);
  }

  public ScheduleDetailInfo updateSchedule(
      long memberId, Long scheduleId, Long reservationId, @Valid ScheduleUpdateRequest request) {

    Schedule schedule = scheduleReader.read(scheduleId);

    if (!schedule.memberId().equals(memberId)) {
      throw new IllegalArgumentException("해당 일정을 수정할 권한이 없습니다.");
    }

    Schedule updatedSchedule =
        new Schedule(
            schedule.id(),
            schedule.memberId(),
            request.title() != null ? request.title() : schedule.title(),
            request.description() != null ? request.description() : schedule.description(),
            request.type() != null ? request.type() : schedule.type(),
            request.startTime() != null ? request.startTime() : schedule.startTime(),
            request.endTime() != null ? request.endTime() : schedule.endTime(),
            request.isAllDay() != null ? request.isAllDay() : schedule.isAllDay(),
            request.roomId() != null ? request.roomId() : schedule.roomId(),
            reservationId,
            request.isBusy() != null ? request.isBusy() : schedule.isBusy(),
            schedule.googleEventId());

    List<Long> updatedAttendeeIds =
        request.attendeeIds() != null
            ? request.attendeeIds()
            : scheduleReader.readAttendeeIds(scheduleId);

    scheduleStore.update(updatedSchedule, updatedAttendeeIds);

    return getDetailSchedule(memberId, scheduleId);
  }

  public void deleteSchedule(long memberId, Long scheduleId) {

    Schedule schedule = scheduleReader.read(scheduleId);

    if (!schedule.memberId().equals(memberId)) {
      throw new IllegalArgumentException("해당 일정을 삭제할 권한이 없습니다.");
    }

    // 참석자 삭제
    // TODO : 일정 내에 있는 참석자는 Hard vs Soft? Soft라면 Repository에 'DELETED' 검증이 되어야 할 것
    scheduleStore.delete(schedule);
  }

  public Long upsertScheduleByGoogleEventId(long memberId, ScheduleSyncRequest request) {

    return 0L;
  }
}
