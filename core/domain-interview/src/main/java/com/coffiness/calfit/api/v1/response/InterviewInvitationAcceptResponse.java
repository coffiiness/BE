package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationAcceptResult;
import java.time.LocalDateTime;

// 면접관 초대 수락 응답
public record InterviewInvitationAcceptResponse(
    Long interviewScheduleId,
    Long interviewerUserId,
    LocalDateTime respondedAt,
    int acceptedCount,
    int totalInterviewerCount,
    boolean finalized,
    InterviewStatus status) {

  public static InterviewInvitationAcceptResponse from(InterviewInvitationAcceptResult result) {
    return new InterviewInvitationAcceptResponse(
        result.interviewScheduleId(),
        result.interviewerUserId(),
        result.respondedAt(),
        result.acceptedCount(),
        result.totalInterviewerCount(),
        result.finalized(),
        result.status());
  }
}
