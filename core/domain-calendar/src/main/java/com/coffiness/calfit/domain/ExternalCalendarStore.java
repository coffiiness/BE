package com.coffiness.calfit.domain;

import java.time.LocalDateTime;

public interface ExternalCalendarStore {

  ExternalCalendar store(ExternalCalendar externalCalendar);

  ExternalCalendar updateAuthTokens(
      Long externalCalendarId,
      String accessToken,
      String refreshToken,
      LocalDateTime tokenExpiresAt);

  ExternalCalendar updateSyncToken(Long externalCalendarId, String syncToken);

  // 외부 캘린더의 watch 채널 정보를 갱신
  ExternalCalendar updateWatchChannel(
      Long externalCalendarId,
      String channelId,
      String channelResourceId,
      LocalDateTime channelExpiresAt);
}
