package com.coffiness.calfit.storage.db.core.calendar;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoogleCalendarOAuthStateRepository
    extends JpaRepository<GoogleCalendarOAuthStateEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from GoogleCalendarOAuthStateEntity s where s.stateHash = :hash")
  Optional<GoogleCalendarOAuthStateEntity> findForConsumption(@Param("hash") String hash);

  @Modifying
  @Query(
      "update GoogleCalendarOAuthStateEntity s set s.usedAt = :now where s.userId = :userId and s.workspaceId = :workspaceId and s.usedAt is null")
  int invalidatePending(
      @Param("userId") Long userId,
      @Param("workspaceId") String workspaceId,
      @Param("now") LocalDateTime now);
}
