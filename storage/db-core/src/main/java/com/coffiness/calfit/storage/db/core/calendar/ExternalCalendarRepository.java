package com.coffiness.calfit.storage.db.core.calendar;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalCalendarRepository extends JpaRepository<ExternalCalendarEntity, Long> {

  // 특정 유저의 모든 외부 캘린더 연동 정보 조회
  List<ExternalCalendarEntity> findAllByUserId(Long userId);

  // 특정 유저의 특정 외부 캘린더 단건 조회
  Optional<ExternalCalendarEntity> findByUserIdAndCalendarId(Long userId, String calendarId);
}
