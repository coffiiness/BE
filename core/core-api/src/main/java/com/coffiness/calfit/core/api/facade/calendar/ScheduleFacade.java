package com.coffiness.calfit.core.api.facade.calendar;

import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleInfo;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleFacade {

  private final MemberReader memberReader;
  private final ScheduleService scheduleService;
  private final MeetingRoomService meetingRoomService;

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
    Member member = validateAndGetMember(userId);
    Long reservationId = null;

    if (request.roomId() != null) {
      MeetingRoomReservation reservation =
          meetingRoomService.reserve(
              member.id(), request.roomId(), request.startTime(), request.endTime());
      reservationId = reservation.id();
    }

    scheduleService.createSchedule(member.id(), reservationId, request);
  }

  @Transactional(readOnly = true)
  public List<ScheduleInfo> getSchedules(
      long userId, LocalDateTime startDate, LocalDateTime endDate) {
    Member member = validateAndGetMember(userId);

    return scheduleService.getSchedules(member.id(), startDate, endDate);
  }

  @Transactional(readOnly = true)
  public ScheduleDetailInfo getDetailSchedule(long userId, Long scheduleId) {
    Member member = validateAndGetMember(userId);

    return scheduleService.getDetailSchedule(member.id(), scheduleId);
  }

  @Transactional
  public ScheduleDetailInfo updateSchedule(
      long userId, Long scheduleId, ScheduleUpdateRequest request) {
    Member member = validateAndGetMember(userId);
    ScheduleDetailInfo scheduleDetailInfo =
        scheduleService.getDetailSchedule(member.id(), scheduleId);

    Long newReservationId = scheduleDetailInfo.reservationId();

    boolean roomIdChanged =
        (request.roomId() != null && !request.roomId().equals(scheduleDetailInfo.roomId()))
            || (request.roomId() == null && scheduleDetailInfo.roomId() != null);
    boolean timeChanged =
        (request.startTime() != null && !request.startTime().equals(scheduleDetailInfo.startTime()))
            || (request.endTime() != null
                && !request.endTime().equals(scheduleDetailInfo.endTime()));

    if (scheduleDetailInfo.roomId() != null && scheduleDetailInfo.reservationId() != null) {
      if (roomIdChanged || timeChanged) {
        meetingRoomService.cancelReservation(
            member.id(), scheduleDetailInfo.roomId(), scheduleDetailInfo.reservationId());
        newReservationId = null; // 기존 예약은 취소됨
      }
    }

    // 새로운 회의실 점유 로직
    if (request.roomId() != null && (roomIdChanged || timeChanged)) {
      LocalDateTime startTime =
          request.startTime() != null ? request.startTime() : scheduleDetailInfo.startTime();
      LocalDateTime endTime =
          request.endTime() != null ? request.endTime() : scheduleDetailInfo.endTime();

      MeetingRoomReservation reservation =
          meetingRoomService.reserve(member.id(), request.roomId(), startTime, endTime);
      newReservationId = reservation.id();
    }

    return scheduleService.updateSchedule(member.id(), scheduleId, newReservationId, request);
  }

  @Transactional
  public void deleteSchedule(long userId, Long scheduleId) {
    Member member = validateAndGetMember(userId);

    scheduleService.deleteSchedule(member.id(), scheduleId);
  }
}
