package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleEventRequestDto;
import com.coffiness.calfit.dto.GoogleCalendarSyncResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        value = "google-calendar-api",
        url = "https://www.googleapis.com/calendar/v3/calendars/primary/events"
)
interface GoogleCalendarApi {

    // 구글 캘린더 일정 생성
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    GoogleCalendarSyncResponseDto createEvent(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody GoogleEventRequestDto request);

    // 구글 캘린더 일정 수정
    @DeleteMapping("/{eventId}")
    void deleteEvent(
            @RequestHeader("Authorization") String bearerToken,
            @PathVariable("eventId") String eventId);

    // TODO: DTO 작성 후 일정 수정 및 리스트 조회 기능 추가 (02.24)
}
