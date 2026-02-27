package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;

public record MeetingRoomResponse(Long id, String name, Integer capacity) {

  public static MeetingRoomResponse from(MeetingRoom meetingRoom) {
    return new MeetingRoomResponse(meetingRoom.id(), meetingRoom.name(), meetingRoom.capacity());
  }
}
