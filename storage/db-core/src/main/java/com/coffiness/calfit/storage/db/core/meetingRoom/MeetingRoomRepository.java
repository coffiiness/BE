package com.coffiness.calfit.storage.db.core.meetingRoom;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoomEntity, Long> {


  boolean existsByName(String name);

  Optional<MeetingRoomEntity> findByName(String name);

  List<MeetingRoomEntity> findAllByOrderByNameAsc();
}
