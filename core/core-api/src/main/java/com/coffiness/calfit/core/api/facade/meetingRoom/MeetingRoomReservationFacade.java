package com.coffiness.calfit.core.api.facade.meetingRoom;

import com.coffiness.calfit.api.v1.request.MeetingRoomReservationCreateRequest;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.domain.interview.InterviewService;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReader;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationCreatedEvent;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationService;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MeetingRoomReservationFacade {

  private final MeetingRoomReservationService meetingRoomReservationService;
  private final InterviewService interviewService;
  private final ScheduleService scheduleService;
  private final MeetingRoomReader meetingRoomReader;
  private final DomainEventPublisher domainEventPublisher;

  @Transactional
  public MeetingRoomReservation reserveMeetingRoom(
      Long userId, Long meetingRoomId, MeetingRoomReservationCreateRequest request) {
    MeetingRoomReservation reservation =
        meetingRoomReservationService.reserve(
            userId,
            meetingRoomId,
            request.startDatetime(),
            request.endDatetime(),
            request.participantUserIds());

    scheduleService.createMeetingRoomReservationSchedule(
        userId,
        reservation.id(),
        meetingRoomId,
        request.title(),
        request.description(),
        request.startDatetime(),
        request.endDatetime(),
        request.participantUserIds());

    MeetingRoom meetingRoom = meetingRoomReader.getMeetingRoom(meetingRoomId);
    domainEventPublisher.publish(
        MeetingRoomReservationCreatedEvent.of(
            currentTenantId(),
            reservation.id(),
            userId,
            meetingRoomId,
            meetingRoom.name(),
            request.title(),
            request.startDatetime(),
            request.endDatetime(),
            request.participantUserIds()));

    return reservation;
  }

  @Transactional(readOnly = true)
  public List<MeetingRoomReservation> getActiveReservations(
      LocalDateTime fromDatetime, LocalDateTime toDatetime) {
    return meetingRoomReservationService.listActiveReservations(fromDatetime, toDatetime);
  }

  @Transactional
  public MeetingRoomReservation cancelReservation(
      Long userId, Long meetingRoomId, Long reservationId) {
    MeetingRoomReservation canceled =
        meetingRoomReservationService.cancelReservation(userId, meetingRoomId, reservationId);

    if (canceled.interviewScheduleId() != null) {
      interviewService.cancelConfirmedSchedule(userId, canceled.interviewScheduleId());
    }

    scheduleService.deleteSchedulesByReservationId(canceled.id());

    return canceled;
  }

  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return tenantId;
  }
}
