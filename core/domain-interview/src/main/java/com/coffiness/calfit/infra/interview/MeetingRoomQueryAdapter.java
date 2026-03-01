package com.coffiness.calfit.infra.interview;

import com.coffiness.calfit.domain.interview.command.model.MeetingRoomCandidate;
import com.coffiness.calfit.domain.interview.port.MeetingRoomQueryPort;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingRoomQueryAdapter implements MeetingRoomQueryPort {

  private final MeetingRoomRepository meetingRoomRepository;

  @Override
  public List<MeetingRoomCandidate> findReservableRooms(
      String tenantId, Integer preferredLocation, Integer requiredCapacity) {
    return meetingRoomRepository.findAllByOrderByNameAsc().stream()
        .filter(room -> preferredLocation == null || preferredLocation.equals(room.getLocation()))
        .filter(room -> requiredCapacity == null || room.getCapacity() >= requiredCapacity)
        .map(this::toCandidate)
        .toList();
  }

  private MeetingRoomCandidate toCandidate(MeetingRoomEntity entity) {
    return new MeetingRoomCandidate(
        entity.getId(), entity.getName(), entity.getLocation(), entity.getCapacity());
  }
}
