package com.coffiness.calfit.core.api.facade.meetingRoom;

import com.coffiness.calfit.api.v1.request.MeetingRoomReservationCreateRequest;
import com.coffiness.calfit.domain.interview.InterviewService;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationService;
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

  @Transactional
  public MeetingRoomReservation reserveMeetingRoom(
      Long userId, Long meetingRoomId, MeetingRoomReservationCreateRequest request) {
    return meetingRoomReservationService.reserve(
        userId, meetingRoomId, request.startDatetime(), request.endDatetime());
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

    return canceled;
  }
}
