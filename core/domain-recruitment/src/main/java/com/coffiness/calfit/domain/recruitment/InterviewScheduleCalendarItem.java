package com.coffiness.calfit.domain.recruitment;

import java.time.LocalDateTime;

/*
 * 채용 공고 상세 캘린더
 * */
public record InterviewScheduleCalendarItem(
    Long id,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Long interviewerMemberId,
    String interviewerName,
    String title,
    String applicantName,
    String description) {}
