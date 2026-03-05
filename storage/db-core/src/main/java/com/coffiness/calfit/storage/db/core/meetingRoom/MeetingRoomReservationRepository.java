package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomReservationRepository
    extends JpaRepository<MeetingRoomReservationEntity, Long> {
  long countByTenantIdAndMeetingRoomIdAndReservationStatusAndStartDatetimeBeforeAndEndDatetimeAfter(
      String tenantId,
      Long meetingRoomId,
      MeetingRoomStatus reservationStatus,
      LocalDateTime endTime,
      LocalDateTime startTime);

  List<MeetingRoomReservationEntity>
      findAllByTenantIdAndMeetingRoomIdInAndReservationStatusAndStartDatetimeBeforeAndEndDatetimeAfter(
          String tenantId,
          List<Long> meetingRoomIds,
          MeetingRoomStatus reservationStatus,
          LocalDateTime to,
          LocalDateTime from);

  Optional<MeetingRoomReservationEntity> findByTenantIdAndIdAndMeetingRoomIdAndReservationStatus(
      String tenantId, Long id, Long meetingRoomId, MeetingRoomStatus reservationStatus);
}
