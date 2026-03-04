package com.coffiness.calfit.domain.meetingRoom;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

  private final MeetingRoomReader meetingRoomReader;
  private final MeetingRoomStore meetingRoomStore;
  private final MeetingRoomValidator meetingRoomValidator;

  /* 회의실 생성 */
  @Transactional
  public MeetingRoom create(String name, Integer location, Integer capacity) {
    meetingRoomValidator.validateCreateInput(name, location, capacity);
    return meetingRoomStore.create(name, location, capacity);
  }

  /* 회의실 수정 */
  @Transactional
  public MeetingRoom update(Long id, String name, Integer capacity) {
    meetingRoomValidator.validateUpdateInput(id, name, capacity);
    return meetingRoomStore.update(id, name, capacity);
  }

  /* 회의실 삭제 */
  @Transactional
  public void delete(Long id, Long userId) {
    meetingRoomValidator.validateDeleteRequest(id, userId);
    meetingRoomStore.delete(id);
  }

  /* 회의실 리스트 조회 */
  @Transactional(readOnly = true)
  public List<MeetingRoom> list() {
    return meetingRoomReader.getMeetingRooms();
  }

  @Transactional
  public MeetingRoomReservation reserve(
      Long userId, Long meetingRoomId, LocalDateTime startDatetime, LocalDateTime endDatetime) {
    meetingRoomValidator.validateReserveRequest(userId, meetingRoomId, startDatetime, endDatetime);
    return meetingRoomStore.reserve(meetingRoomId, userId, startDatetime, endDatetime);
  }

  @Transactional
  public MeetingRoomReservation cancelReservation(
      Long userId, Long meetingRoomId, Long reservationId) {
    meetingRoomValidator.validateCancelReservationRequest(userId, meetingRoomId, reservationId);
    return meetingRoomStore.cancelReservation(meetingRoomId, reservationId, userId);
  }
}
