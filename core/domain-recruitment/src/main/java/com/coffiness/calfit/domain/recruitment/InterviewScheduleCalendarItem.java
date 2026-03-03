package com.coffiness.calfit.domain.recruitment;

import java.time.LocalDateTime;

/*
 * 채용 공고 상세 오른쪽 카드
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
