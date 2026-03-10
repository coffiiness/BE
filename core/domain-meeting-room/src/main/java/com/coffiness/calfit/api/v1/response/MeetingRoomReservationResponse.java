package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import java.time.LocalDateTime;

public record MeetingRoomReservationResponse(
    Long id,
    Long meetingRoomId,
    Long userId,
    Long interviewScheduleId,
    LocalDateTime startDatetime,
    LocalDateTime endDatetime,
    MeetingRoomStatus status) {

  public static MeetingRoomReservationResponse from(MeetingRoomReservation reservation) {
    return new MeetingRoomReservationResponse(
        reservation.id(),
        reservation.meetingRoomId(),
        reservation.userId(),
        reservation.interviewScheduleId(),
        reservation.startDatetime(),
        reservation.endDatetime(),
        reservation.status());
  }
}
