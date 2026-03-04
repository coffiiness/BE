package com.coffiness.calfit.domain.meetingRoom;

import java.time.LocalDateTime;

public interface MeetingRoomStore {

  MeetingRoom create(String name, Integer location, Integer capacity);

  MeetingRoom update(Long meetingRoomId, String name, Integer capacity);

  void delete(Long meetingRoomId);

  MeetingRoomReservation reserve(
      Long meetingRoomId, Long userId, LocalDateTime startDatetime, LocalDateTime endDatetime);

  MeetingRoomReservation cancelReservation(Long meetingRoomId, Long reservationId, Long userId);
}
