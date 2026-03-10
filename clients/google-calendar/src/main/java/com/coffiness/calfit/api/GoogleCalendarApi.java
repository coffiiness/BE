package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleCalendarSyncResponseDto;
import com.coffiness.calfit.dto.GoogleCalendarWatchRequestDto;
import com.coffiness.calfit.dto.GoogleCalendarWatchResponseDto;
import com.coffiness.calfit.dto.GoogleEventRequestDto;
import com.coffiness.calfit.dto.GoogleEventResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    value = "google-calendar-api",
    url = "https://www.googleapis.com/calendar/v3/calendars/primary/events")
interface GoogleCalendarApi {

  // 구글 캘린더 일정 생성
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  GoogleEventResponseDto createEvent(
      @RequestHeader("Authorization") String bearerToken,
      @RequestBody GoogleEventRequestDto request);

  // 구글 캘린더 일정 수정
  @PutMapping(value = "/{eventId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  GoogleEventResponseDto updateEvent(
      @RequestHeader("Authorization") String bearerToken,
      @PathVariable("eventId") String eventId,
      @RequestBody GoogleEventRequestDto request);

  // 구글 캘린더 일정 삭제
  @DeleteMapping("/{eventId}")
  void deleteEvent(
      @RequestHeader("Authorization") String bearerToken, @PathVariable("eventId") String eventId);

  // 구글 캘린더 동기화
  @GetMapping
  GoogleCalendarSyncResponseDto syncEvent(
      @RequestHeader("Authorization") String bearerToken,
      @RequestParam(value = "syncToken", required = false) String nextSyncToken,
      @RequestParam(value = "pageToken", required = false) String pageToken);

  // 구글 캘린더 watch 채널 생성
  @PostMapping(value = "/watch", consumes = MediaType.APPLICATION_JSON_VALUE)
  GoogleCalendarWatchResponseDto watchEvents(
      @RequestHeader("Authorization") String bearerToken,
      @RequestBody GoogleCalendarWatchRequestDto request);
}
