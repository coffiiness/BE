package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomReservationRepository
    extends JpaRepository<MeetingRoomReservationEntity, Long> {
  long countByTenantIdAndMeetingRoomIdAndStatusAndStartDatetimeBeforeAndEndDatetimeAfter(
      String tenantId,
      Long meetingRoomId,
      MeetingRoomStatus status,
      LocalDateTime endTime,
      LocalDateTime startTime);

  List<MeetingRoomReservationEntity>
      findAllByTenantIdAndMeetingRoomIdInAndStatusAndStartDatetimeBeforeAndEndDatetimeAfter(
          String tenantId,
          List<Long> meetingRoomIds,
          MeetingRoomStatus status,
          LocalDateTime to,
          LocalDateTime from);
}
