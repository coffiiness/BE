package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.interview.InterviewScheduleCalendarItem;
import java.time.LocalDateTime;

public record InterviewScheduleResponse(
    Long id,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Long meetingRoomId,
    Long interviewerUserId,
    String interviewerName,
    String creatorName,
    String title,
    String applicantName,
    String description,
    String location) {

  public static InterviewScheduleResponse from(InterviewScheduleCalendarItem item) {
    return new InterviewScheduleResponse(
        item.id(),
        item.startAt(),
        item.endAt(),
        item.meetingRoomId(),
        item.interviewerUserId(),
        item.interviewerName(),
        item.creatorName(),
        item.title(),
        item.applicantName(),
        item.description(),
        item.location());
  }
}
