package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleCalendarSyncResponseDto;
import com.coffiness.calfit.dto.GoogleEventRequestDto;
import com.coffiness.calfit.dto.GoogleEventResponseDto;
import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult.SyncEventModel;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GoogleCalendarClient {

  private final GoogleCalendarApi googleCalendarApi;

  /*
   * 캘린더에 하나의 일정 추가 (POST)
   */
  public GoogleCalendarClientResult createEvent(
      String accessToken,
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean isAllDay) {

    GoogleEventRequestDto.EventDateTime startDto = createEventDateTime(start, isAllDay);
    GoogleEventRequestDto.EventDateTime endDto = createEventDateTime(end, isAllDay);

    GoogleEventRequestDto requestDto =
        new GoogleEventRequestDto(summary, description, startDto, endDto);

    String bearerToken = "Bearer " + accessToken;
    GoogleEventResponseDto responseDto = googleCalendarApi.createEvent(bearerToken, requestDto);

    return responseDto.toResult();
  }

  /*
   * 캘린더에 있는 일정 가져오기 (GET)
   */
  public GoogleCalendarSyncResult syncEvents(String accessToken, String syncToken) {
    String bearerToken = "Bearer " + accessToken;
    List<SyncEventModel> allEvents = new ArrayList<>();
    String pageToken = null;
    String finalSyncToken = null;

    try {
      do {
        GoogleCalendarSyncResponseDto responseDto =
            googleCalendarApi.syncEvent(bearerToken, syncToken, pageToken);

        GoogleCalendarSyncResult partialResult = responseDto.toResult();
        if (partialResult.items() != null) {
          allEvents.addAll(partialResult.items());
        }
        // pageToken이 없을 때까지 모든 일정 저장
        pageToken = responseDto.nextPageToken();
        if (pageToken == null) {
          finalSyncToken = responseDto.nextSyncToken();
        }
      } while (pageToken != null);

      return new GoogleCalendarSyncResult(allEvents, finalSyncToken);

    } catch (FeignException.Gone e) {
      throw new IllegalStateException("GOOGLE_SYNC_TOKEN_EXPIRED", e);
    }
  }

  /*
   * 내부 시간 변환 헬퍼 메소드
   */
  private GoogleEventRequestDto.EventDateTime createEventDateTime(
      ZonedDateTime time, boolean isAllDay) {
    String timeZoneId = time.getZone().getId();

    if (isAllDay) {
      return new GoogleEventRequestDto.EventDateTime(
          null, time.toLocalDate().toString(), timeZoneId);
    } else {
      return new GoogleEventRequestDto.EventDateTime(
          time.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), null, timeZoneId);
    }
  }
}
