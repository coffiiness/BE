package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

  @Query(
      "SELECT s FROM ScheduleEntity s "
          + "WHERE s.memberId = :memberId "
          + "AND s.startTime <= :endDate "
          + "AND s.endTime >= :startDate "
          + "AND s.status = 'ACTIVE'")
  List<ScheduleEntity> findOverlappingSchedules(
      @Param("memberId") long memberId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  @Query(
      "SELECT s FROM ScheduleEntity s "
          + "WHERE s.memberId IN :memberIds "
          + "AND s.startTime < :to "
          + "AND s.endTime > :from "
          + "AND s.status = :status "
          + "AND s.isBusy = true")
  List<ScheduleEntity> findBusyOverlappingSchedulesByUserIds(
      @Param("memberIds") List<Long> memberIds,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("status") EntityStatus status);

  @Query(
      "SELECT s FROM ScheduleEntity s "
          + "WHERE s.roomId IN :roomIds "
          + "AND s.startTime < :to "
          + "AND s.endTime > :from "
          + "AND s.status = :status "
          + "AND s.isBusy = true")
  List<ScheduleEntity> findBusyOverlappingSchedulesByRoomIds(
      @Param("roomIds") List<Long> roomIds,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("status") EntityStatus status);

  Optional<ScheduleEntity> findByIdAndStatus(Long id, EntityStatus status);

  Optional<ScheduleEntity> findByGoogleEventIdAndStatus(String googleEventId, EntityStatus status);

  boolean existsByGoogleEventIdAndStatus(String googleEventId, EntityStatus status);

  List<ScheduleEntity> findAllByGoogleEventIdInAndStatus(
      List<String> googleEventIds, EntityStatus status);
}
