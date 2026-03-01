package com.coffiness.calfit.storage.db.core.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleAttendeeRepository extends JpaRepository<ScheduleAttendeeEntity, Long> {

    // 특정 일정에 참석된 참석자 데이터 가져오기
    List<ScheduleAttendeeEntity> findByScheduleId(Long scheduleId);

    // 일정이 삭제될 때 참석자 데이터도 삭제
    void deleteByScheduleId(Long scheduleId);
}
