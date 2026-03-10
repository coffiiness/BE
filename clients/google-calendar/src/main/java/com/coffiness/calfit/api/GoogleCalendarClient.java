package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleCalendarSyncResponseDto;
import com.coffiness.calfit.dto.GoogleCalendarWatchRequestDto;
import com.coffiness.calfit.dto.GoogleCalendarWatchResponseDto;
import com.coffiness.calfit.dto.GoogleEventRequestDto;
import com.coffiness.calfit.dto.GoogleEventResponseDto;
import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult.SyncEventModel;
import com.coffiness.calfit.model.GoogleCalendarWatchResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import feign.FeignException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleCalendarClient implements GoogleCalendarPort {

  private final GoogleCalendarApi googleCalendarApi;

  // 캘린더에 하나의 일정 추가
  @Override
  public GoogleCalendarClientResult createEvent(
      String accessToken,
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean isAllDay) {

    GoogleEventRequestDto requestDto =
        createEventRequest(summary, description, start, end, isAllDay);

    String bearerToken = "Bearer " + accessToken;
    GoogleEventResponseDto responseDto = googleCalendarApi.createEvent(bearerToken, requestDto);

    return responseDto.toResult();
  }

  // 캘린더에 하나의 일정 수정
  @Override
  public GoogleCalendarClientResult updateEvent(
      String accessToken,
      String googleEventId,
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean isAllDay) {

    GoogleEventRequestDto requestDto =
        createEventRequest(summary, description, start, end, isAllDay);

    String bearerToken = "Bearer " + accessToken;
    GoogleEventResponseDto responseDto =
        googleCalendarApi.updateEvent(bearerToken, googleEventId, requestDto);

    return responseDto.toResult();
  }

  // 캘린더 일정 삭제
  @Override
  public void deleteEvent(String accessToken, String googleEventId) {
    String bearerToken = "Bearer " + accessToken;
    googleCalendarApi.deleteEvent(bearerToken, googleEventId);
  }

  // 캘린더에 있는 일정 가져오기
  @Override
  public GoogleCalendarSyncResult syncEvent(String accessToken, String syncToken) {
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

        // pageToken 이 없을 때까지 모든 일정 조회
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

  // 구글 이벤트 변경 알림을 받기 위한 watch 채널 생성
  @Override
  public GoogleCalendarWatchResult watchEvents(
      String accessToken, String callbackUrl, String channelToken, Long ttlSeconds) {
    if (callbackUrl == null || callbackUrl.isBlank()) {
      throw new IllegalStateException("GOOGLE_WATCH_CALLBACK_URL_MISSING");
    }

    String bearerToken = "Bearer " + accessToken;
    GoogleCalendarWatchRequestDto requestDto =
        new GoogleCalendarWatchRequestDto(
            UUID.randomUUID().toString(),
            "web_hook",
            callbackUrl,
            channelToken,
            createWatchParams(ttlSeconds));

    try {
      GoogleCalendarWatchResponseDto responseDto =
          googleCalendarApi.watchEvents(bearerToken, requestDto);
      return responseDto.toResult();
    } catch (FeignException e) {
      if (e.status() == 400 || e.status() == 401 || e.status() == 403) {
        throw new IllegalStateException("GOOGLE_WATCH_REGISTRATION_FAILED", e);
      }
      if (e.status() >= 500) {
        throw new IllegalStateException("GOOGLE_TEMPORARY_ERROR", e);
      }
      throw new IllegalStateException("GOOGLE_OAUTH_ERROR", e);
    }
  }

  private GoogleEventRequestDto createEventRequest(
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean isAllDay) {
    GoogleEventRequestDto.EventDateTime startDto = createEventDateTime(start, isAllDay);
    GoogleEventRequestDto.EventDateTime endDto = createEventDateTime(end, isAllDay);

    return new GoogleEventRequestDto(summary, description, startDto, endDto);
  }

  // 이벤트 시간 변환 헬퍼 메소드
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

  // watch 채널 생성 요청에 전달할 TTL 파라미터를 구성
  private Map<String, String> createWatchParams(Long ttlSeconds) {
    if (ttlSeconds == null || ttlSeconds <= 0) {
      return Map.of();
    }

    return Map.of("ttl", String.valueOf(ttlSeconds));
  }
}
