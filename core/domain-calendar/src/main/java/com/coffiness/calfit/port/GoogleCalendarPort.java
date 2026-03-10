package com.coffiness.calfit.port;

import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import java.time.ZonedDateTime;

public interface GoogleCalendarPort {

  GoogleCalendarClientResult createEvent(
      String accessToken,
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean allDay);

  GoogleCalendarClientResult updateEvent(
      String accessToken,
      String googleEventId,
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean allDay);

  void deleteEvent(String accessToken, String googleEventId);

  GoogleCalendarSyncResult syncEvent(String accessToken, String syncToken);
}
