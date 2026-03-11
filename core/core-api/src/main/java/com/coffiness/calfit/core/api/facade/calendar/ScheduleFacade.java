package com.coffiness.calfit.core.api.facade.calendar;

import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleInfo;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * 일정 API의 멤버 검증과 회의실 예약 연동 담당
 * */
@Component
@RequiredArgsConstructor
public class ScheduleFacade {

  private final MemberReader memberReader;
  private final ScheduleService scheduleService;
  private final MeetingRoomReservationService meetingRoomReservationService;

  private Member validateAndGetMember(long userId) {
    String currentWorkspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(currentWorkspaceId, userId);
    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버가 아닙니다.");
    }
    return member;
  }

  @Transactional
  public void createSchedule(long userId, ScheduleCreateRequest request) {
    validateAndGetMember(userId);
    Long reservationId = null;

    if (request.roomId() != null) {
      MeetingRoomReservation reservation =
          meetingRoomReservationService.reserve(
              userId, request.roomId(), request.startTime(), request.endTime(), List.of());
      reservationId = reservation.id();
    }

    scheduleService.createSchedule(userId, reservationId, request);
  }

  @Transactional
  public void syncSchedule(long userId, ScheduleSyncRequest request) {
    validateAndGetMember(userId);

    scheduleService.upsertScheduleByGoogleEventId(userId, request);
  }

  @Transactional(readOnly = true)
  public List<ScheduleInfo> getSchedules(
      long userId, LocalDateTime startDate, LocalDateTime endDate) {
    validateAndGetMember(userId);

    return scheduleService.getSchedules(userId, startDate, endDate);
  }

  @Transactional(readOnly = true)
  public ScheduleDetailInfo getDetailSchedule(long userId, Long scheduleId) {
    validateAndGetMember(userId);

    return scheduleService.getDetailSchedule(userId, scheduleId);
  }

  @Transactional
  public ScheduleDetailInfo updateSchedule(
      long userId, Long scheduleId, ScheduleUpdateRequest request) {
    validateAndGetMember(userId);
    ScheduleDetailInfo scheduleDetailInfo = scheduleService.getDetailSchedule(userId, scheduleId);

    Long targetRoomId = request.roomId() != null ? request.roomId() : scheduleDetailInfo.roomId();
    Long newReservationId = scheduleDetailInfo.reservationId();

    boolean roomIdChanged =
        request.roomId() != null && !request.roomId().equals(scheduleDetailInfo.roomId());
    boolean timeChanged =
        (request.startTime() != null && !request.startTime().equals(scheduleDetailInfo.startTime()))
            || (request.endTime() != null
                && !request.endTime().equals(scheduleDetailInfo.endTime()));

    if (scheduleDetailInfo.roomId() != null && scheduleDetailInfo.reservationId() != null) {
      if (roomIdChanged || timeChanged) {
        meetingRoomReservationService.cancelReservation(
            userId, scheduleDetailInfo.roomId(), scheduleDetailInfo.reservationId());
        newReservationId = null;
      }
    }

    // 회의실 또는 시간 정보가 변경되면 새 예약을 생성한다.
    if (targetRoomId != null && (roomIdChanged || timeChanged)) {
      LocalDateTime startTime =
          request.startTime() != null ? request.startTime() : scheduleDetailInfo.startTime();
      LocalDateTime endTime =
          request.endTime() != null ? request.endTime() : scheduleDetailInfo.endTime();

      MeetingRoomReservation reservation =
          meetingRoomReservationService.reserve(
              userId, targetRoomId, startTime, endTime, List.of());
      newReservationId = reservation.id();
    }

    return scheduleService.updateSchedule(userId, scheduleId, newReservationId, request);
  }

  @Transactional
  public void deleteSchedule(long userId, Long scheduleId) {
    validateAndGetMember(userId);

    ScheduleDetailInfo scheduleInfo = scheduleService.getDetailSchedule(userId, scheduleId);

    if (scheduleInfo.roomId() != null && scheduleInfo.reservationId() != null) {
      meetingRoomReservationService.cancelReservation(
          userId, scheduleInfo.roomId(), scheduleInfo.reservationId());
    }

    scheduleService.deleteSchedule(userId, scheduleId);
  }
}
