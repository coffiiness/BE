package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.ExternalCalendar;
import com.coffiness.calfit.domain.ExternalCalendarStore;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarEntity;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalCalendarStoreImpl implements ExternalCalendarStore {

  private final ExternalCalendarRepository externalCalendarRepository;

  @Override
  public ExternalCalendar store(ExternalCalendar externalCalendar) {
    ExternalCalendarEntity entity =
        ExternalCalendarEntity.builder()
            .userId(externalCalendar.userId())
            .calendarId(externalCalendar.calendarId())
            .accessToken(externalCalendar.accessToken())
            .refreshToken(externalCalendar.refreshToken())
            .tokenExpiresAt(externalCalendar.tokenExpiresAt())
            .syncToken(externalCalendar.syncToken())
            .isSyncEnabled(externalCalendar.isSyncEnabled())
            .build();

    ExternalCalendarEntity saved = externalCalendarRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  public ExternalCalendar updateAuthTokens(
      Long externalCalendarId,
      String accessToken,
      String refreshToken,
      LocalDateTime tokenExpiresAt) {
    ExternalCalendarEntity entity =
        externalCalendarRepository
            .findByIdAndStatus(externalCalendarId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("구글 캘린더 연동 정보를 찾을 수 없습니다."));

    entity.updateAuthTokens(accessToken, refreshToken, tokenExpiresAt);
    ExternalCalendarEntity saved = externalCalendarRepository.save(entity);

    return toDomain(saved);
  }

  @Override
  public ExternalCalendar updateSyncToken(Long externalCalendarId, String syncToken) {
    ExternalCalendarEntity entity =
        externalCalendarRepository
            .findByIdAndStatus(externalCalendarId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("구글 캘린더 연동 정보를 찾을 수 없습니다."));

    entity.updateSyncToken(syncToken);
    ExternalCalendarEntity saved = externalCalendarRepository.save(entity);

    return toDomain(saved);
  }

  private ExternalCalendar toDomain(ExternalCalendarEntity entity) {
    return new ExternalCalendar(
        entity.getId(),
        entity.getUserId(),
        entity.getCalendarId(),
        entity.getAccessToken(),
        entity.getRefreshToken(),
        entity.getTokenExpiresAt(),
        entity.getSyncToken(),
        entity.isSyncEnabled());
  }
}
