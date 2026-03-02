package com.coffiness.calfit.storage.db.core.meetingRoom;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoomEntity, Long> {

  boolean existsByName(String name);

  Optional<MeetingRoomEntity> findByName(String name);

  List<MeetingRoomEntity> findAllByOrderByNameAsc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<MeetingRoomEntity> findByTenantIdAndId(String tenantId, Long id);
}
