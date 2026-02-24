package com.coffiness.calfit.dto;

/*
 * 일정 생성 레코드
 * */
public record GoogleEventRequestDto(
    String summary, String description, EventDateTime start, EventDateTime end) {

  public record EventDateTime(
      String dateTime, // 일반 일정
      String date, // 종일 일정
      String timeZone) {}
}
