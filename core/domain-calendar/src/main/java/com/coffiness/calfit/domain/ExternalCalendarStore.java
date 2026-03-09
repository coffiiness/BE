package com.coffiness.calfit.domain;

import java.time.LocalDateTime;

public interface ExternalCalendarStore {

  ExternalCalendar store(ExternalCalendar externalCalendar);

  ExternalCalendar updateAuthTokens(
      Long externalCalendarId,
      String accessToken,
      String refreshToken,
      LocalDateTime tokenExpiresAt);
}
