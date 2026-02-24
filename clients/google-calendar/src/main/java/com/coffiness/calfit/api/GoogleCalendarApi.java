package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleCalendarSyncResponseDto;
import com.coffiness.calfit.dto.GoogleEventRequestDto;
import com.coffiness.calfit.dto.GoogleEventResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    value = "google-calendar-api",
    url = "https://www.googleapis.com/calendar/v3/calendars/primary/events")
interface GoogleCalendarApi {

  // 구글 캘린더 일정 생성
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  GoogleEventResponseDto createEvent(
      @RequestHeader("Authorization") String bearerToken,
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

  // TODO: DTO 일정 수정 및 리스트 조회 기능 추가 (02.24)
}
