package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReader;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingRoomReaderImpl implements MeetingRoomReader {

  private final MeetingRoomRepository meetingRoomRepository;
  private final MeetingRoomReservationRepository reservationRepository;

  @Override
  public boolean existsByName(String name) {
    return meetingRoomRepository.existsByNameAndStatus(name, EntityStatus.ACTIVE);
  }

  @Override
  public MeetingRoom getMeetingRoom(Long meetingRoomId) {
    MeetingRoomEntity entity =
        meetingRoomRepository
            .findByIdAndStatus(meetingRoomId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    return toMeetingRoom(entity);
  }

  @Override
  public List<MeetingRoom> getMeetingRooms() {
    return meetingRoomRepository.findAllByStatusOrderByNameAsc(EntityStatus.ACTIVE).stream()
        .map(this::toMeetingRoom)
        .toList();
  }

  @Override
  public long countOverlappingReservations(
      Long meetingRoomId, LocalDateTime startDatetime, LocalDateTime endDatetime) {
    String tenantId = requireTenantId();
    return reservationRepository
        .countByTenantIdAndMeetingRoomIdAndStatusAndStartDatetimeBeforeAndEndDatetimeAfter(
            tenantId, meetingRoomId, MeetingRoomStatus.RESERVED, endDatetime, startDatetime);
  }

  @Override
  public MeetingRoomReservation getActiveReservation(Long meetingRoomId, Long reservationId) {
    String tenantId = requireTenantId();
    MeetingRoomReservationEntity entity =
        reservationRepository
            .findByTenantIdAndIdAndMeetingRoomIdAndStatus(
                tenantId, reservationId, meetingRoomId, MeetingRoomStatus.RESERVED)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    return toReservation(entity);
  }

  private MeetingRoom toMeetingRoom(MeetingRoomEntity entity) {
    return new MeetingRoom(entity.getId(), entity.getName(), entity.getCapacity());
  }

  private MeetingRoomReservation toReservation(MeetingRoomReservationEntity entity) {
    return new MeetingRoomReservation(
        entity.getId(),
        entity.getMeetingRoomId(),
        entity.getUserId(),
        entity.getStartDatetime(),
        entity.getEndDatetime(),
        entity.getStatus());
  }

  private String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return tenantId;
  }
}
