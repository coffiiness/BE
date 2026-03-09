package com.coffiness.calfit.domain;

import java.time.LocalDateTime;

/*
 * 외부 캘린더 연동 정보 도메인 모델
 * */
public record ExternalCalendar(
    Long id,
    Long userId,
    String calendarId,
    String accessToken,
    String refreshToken,
    LocalDateTime tokenExpiresAt,
    String syncToken,
    boolean isSyncEnabled) {}
