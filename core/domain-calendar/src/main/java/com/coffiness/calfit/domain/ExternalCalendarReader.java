package com.coffiness.calfit.domain;

public interface ExternalCalendarReader {

  ExternalCalendar read(Long externalCalendarId);

  ExternalCalendar readByUserIdAndCalendarId(Long userId, String calendarId);

  ExternalCalendar readSyncEnabledByUserId(Long userId);
}
