package com.coffiness.calfit.dto;

import com.coffiness.calfit.model.GoogleCalendarSyncResult;

import java.util.List;

/*
* 전체 동기화용 레코드
* */
public record GoogleCalendarSyncResponseDto(
        List<Item> items,
        String nextSyncToken) {

    public record Item(
            String id,
            String status,
            String summary,
            String description,
            EventDateTime start,
            EventDateTime end
    ) {}

    public record EventDateTime(
            String dateTime,    // 일반 일정
            String date,        // 종일 일정
            String timeZone
    ) {}

    GoogleCalendarSyncResult toResult() {
        return new GoogleCalendarSyncResult(
                this.items(),
                this.nextSyncToken()
        );
    }
}
