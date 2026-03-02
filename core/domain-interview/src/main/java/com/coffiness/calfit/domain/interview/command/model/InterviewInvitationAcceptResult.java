package com.coffiness.calfit.domain.interview.command.model;

import com.coffiness.calfit.core.enums.InterviewStatus;
import java.time.LocalDateTime;

// 면접관 초대 수락 결과 (finalized=true 이면 최종 확정)
public record InterviewInvitationAcceptResult(
    Long interviewScheduleId,
    Long interviewerUserId,
    LocalDateTime respondedAt,
    int acceptedCount,
    int totalInterviewerCount,
    boolean finalized,
    InterviewStatus status) {}
