package com.coffiness.calfit.port;

import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import com.coffiness.calfit.model.GoogleCalendarWatchResult;
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

  // 구글 이벤트 변경 알림 수신을 위한 watch 채널을 생성
  GoogleCalendarWatchResult watchEvents(
      String accessToken, String callbackUrl, String channelToken, Long ttlSeconds);
}
