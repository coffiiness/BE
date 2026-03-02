package com.coffiness.calfit.domain.interview.command.model;

import com.coffiness.calfit.core.enums.InterviewStatus;
import java.time.LocalDateTime;

// 면접관 초대 거절 결과 (거절 시 재조율을 위해 PENDING 유지)
public record InterviewInvitationDeclineResult(
    Long interviewScheduleId,
    Long interviewerUserId,
    String declineReason,
    LocalDateTime respondedAt,
    int acceptedCount,
    int declinedCount,
    int totalInterviewerCount,
    InterviewStatus status) {}
