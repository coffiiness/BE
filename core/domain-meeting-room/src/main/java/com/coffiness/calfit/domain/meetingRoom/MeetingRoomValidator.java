package com.coffiness.calfit.domain.meetingRoom;

import java.time.LocalDateTime;

public interface MeetingRoomValidator {

  void validateCreateInput(String name, Integer location, Integer capacity);

  void validateUpdateInput(Long meetingRoomId, String name, Integer capacity);

  void validateDeleteRequest(Long meetingRoomId, Long userId);

  void validateReserveRequest(
      Long userId, Long meetingRoomId, LocalDateTime startDatetime, LocalDateTime endDatetime);

  void validateCancelReservationRequest(Long userId, Long meetingRoomId, Long reservationId);
}
