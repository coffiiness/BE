package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomStore;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingRoomStoreImpl implements MeetingRoomStore {

  private final MeetingRoomRepository meetingRoomRepository;
  private final MeetingRoomReservationRepository reservationRepository;

  @Override
  public MeetingRoom create(String name, Integer location, Integer capacity) {
    requireTenantId();
    MeetingRoomEntity entity =
        MeetingRoomEntity.builder().name(name).location(location).capacity(capacity).build();
    try {
      MeetingRoomEntity saved = meetingRoomRepository.save(entity);
      return new MeetingRoom(saved.getId(), saved.getName(), saved.getCapacity());
    } catch (DataIntegrityViolationException e) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public MeetingRoom update(Long meetingRoomId, String name, Integer capacity) {
    MeetingRoomEntity entity =
        meetingRoomRepository
            .findByIdAndStatus(meetingRoomId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.update(name, capacity);
    return new MeetingRoom(entity.getId(), entity.getName(), entity.getCapacity());
  }

  @Override
  public void delete(Long meetingRoomId) {
    MeetingRoomEntity entity =
        meetingRoomRepository
            .findByIdAndStatus(meetingRoomId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.deleted();
  }

  @Override
  public MeetingRoomReservation reserve(
      Long meetingRoomId, Long userId, LocalDateTime startDatetime, LocalDateTime endDatetime) {
    requireTenantId();
    MeetingRoomReservationEntity saved =
        reservationRepository.save(
            MeetingRoomReservationEntity.builder()
                .meetingRoomId(meetingRoomId)
                .userId(userId)
                .interviewScheduleId(null)
                .startDatetime(startDatetime)
                .endDatetime(endDatetime)
                .status(MeetingRoomStatus.RESERVED)
                .build());
    return toReservation(saved);
  }

  @Override
  public MeetingRoomReservation cancelReservation(
      Long meetingRoomId, Long reservationId, Long userId) {
    String tenantId = requireTenantId();
    MeetingRoomReservationEntity entity =
        reservationRepository
            .findByTenantIdAndIdAndMeetingRoomIdAndStatus(
                tenantId, reservationId, meetingRoomId, MeetingRoomStatus.RESERVED)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.cancel();
    return toReservation(entity);
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
