package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.recruitment.InterviewScheduleCalendarItem;
import java.time.LocalDateTime;

public record InterviewScheduleResponse(
    Long id,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Long interviewerMemberId,
    String interviewerName,
    String title,
    String applicantName,
    String description) {

  public static InterviewScheduleResponse from(InterviewScheduleCalendarItem item) {
    return new InterviewScheduleResponse(
        item.id(),
        item.startAt(),
        item.endAt(),
        item.interviewerMemberId(),
        item.interviewerName(),
        item.title(),
        item.applicantName(),
        item.description());
  }
}
