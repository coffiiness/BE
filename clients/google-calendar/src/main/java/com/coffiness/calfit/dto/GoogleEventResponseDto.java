package com.coffiness.calfit.dto;

import com.coffiness.calfit.model.GoogleCalendarClientResult;

/*
* 단일 생성용 레코드
* */
public record GoogleEventResponseDto(
        String id,
        String status
) {
    public GoogleCalendarClientResult toResult() {
        return new GoogleCalendarClientResult(this.id(), true);
    }
}
