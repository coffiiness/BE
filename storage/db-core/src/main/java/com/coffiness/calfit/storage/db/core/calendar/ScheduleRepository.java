package com.coffiness.calfit.storage.db.core.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

    @Query("SELECT s FROM ScheduleEntity s " +
            "WHERE s.userId = :userId " +
            "AND s.startTime <= :endDate " +
            "AND s.endTime >= :startDate " +
            "AND s.status = 'ACTIVE'")
    List<ScheduleEntity> findOverlappingSchedules(
            @Param("userId") long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
