package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleCalendarSyncResponseDto;
import com.coffiness.calfit.dto.GoogleEventRequestDto;
import com.coffiness.calfit.dto.GoogleEventResponseDto;
import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class GoogleCalendarClient {

    private final GoogleCalendarApi googleCalendarApi;

    /*
    * 캘린더에 하나의 일정 추가 (POST)
    * */
    public GoogleCalendarClientResult createEvent(String accessToken, String summary, String description,
                                                  ZonedDateTime start, ZonedDateTime end, boolean isAllDay) {

        GoogleEventRequestDto.EventDateTime startDto = createEventDateTime(start, isAllDay);
        GoogleEventRequestDto.EventDateTime endDto = createEventDateTime(end, isAllDay);

        GoogleEventRequestDto requestDto = new GoogleEventRequestDto(summary, description, startDto, endDto);

        String bearerToken = "Bearer " + accessToken;
        GoogleEventResponseDto responseDto = googleCalendarApi.createEvent(bearerToken, requestDto);

        return responseDto.toResult();
    }

    /*
    * 캘린더에 있는 일정 가져오기 (GET)
    * */
    public GoogleCalendarSyncResult syncEvents(String accessToken, String nextSyncToken) {
        String bearerToken = "Bearer " + accessToken;
        GoogleCalendarSyncResponseDto responseDto = googleCalendarApi.syncEvent(bearerToken, nextSyncToken);

        return responseDto.toResult();
    }

    /*
    * 내부 시간 변환 헬퍼 메소드
    * */
    private GoogleEventRequestDto.EventDateTime createEventDateTime(ZonedDateTime time, boolean isAllDay) {
        String timeZoneId = time.getZone().getId();

        if (isAllDay) {
            return new GoogleEventRequestDto.EventDateTime(null, time.toLocalDate().toString(), timeZoneId);
        } else {
            return new GoogleEventRequestDto.EventDateTime(time.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), null, timeZoneId);
        }
    }
}
