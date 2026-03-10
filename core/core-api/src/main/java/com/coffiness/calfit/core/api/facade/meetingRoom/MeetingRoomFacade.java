package com.coffiness.calfit.core.api.facade.meetingRoom;

import com.coffiness.calfit.api.v1.request.MeetingRoomCreateRequest;
import com.coffiness.calfit.api.v1.request.MeetingRoomUpdateRequest;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MeetingRoomFacade {

  private final MeetingRoomService meetingRoomService;

  @Transactional
  public MeetingRoom createMeetingRoom(Long userId, MeetingRoomCreateRequest request) {
    return meetingRoomService.create(
        request.name(), request.location(), request.capacity(), userId);
  }

  @Transactional(readOnly = true)
  public List<MeetingRoom> getMeetingRoomList() {
    return meetingRoomService.list();
  }

  @Transactional
  public MeetingRoom updateMeetingRoom(
      Long userId, Long meetingRoomId, MeetingRoomUpdateRequest request) {
    return meetingRoomService.update(meetingRoomId, request.name(), request.capacity(), userId);
  }

  @Transactional
  public void deleteMeetingRoom(Long userId, Long meetingRoomId) {
    meetingRoomService.delete(meetingRoomId, userId);
  }
}
