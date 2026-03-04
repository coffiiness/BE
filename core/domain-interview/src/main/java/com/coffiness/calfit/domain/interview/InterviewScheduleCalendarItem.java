package com.coffiness.calfit.domain.interview;

import java.time.LocalDateTime;

public record InterviewScheduleCalendarItem(
    Long id,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Long interviewerMemberId,
    String interviewerName,
    String title,
    String applicantName,
    String description) {}
