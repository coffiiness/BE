package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationDeclineResult;
import java.time.LocalDateTime;

// 면접관 초대 거절 응답
public record InterviewInvitationDeclineResponse(
    Long interviewScheduleId,
    Long interviewerUserId,
    String declineReason,
    LocalDateTime respondedAt,
    int acceptedCount,
    int declinedCount,
    int totalInterviewerCount,
    InterviewStatus status) {

  public static InterviewInvitationDeclineResponse from(InterviewInvitationDeclineResult result) {
    return new InterviewInvitationDeclineResponse(
        result.interviewScheduleId(),
        result.interviewerUserId(),
        result.declineReason(),
        result.respondedAt(),
        result.acceptedCount(),
        result.declinedCount(),
        result.totalInterviewerCount(),
        result.status());
  }
}
