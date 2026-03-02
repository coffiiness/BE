package com.coffiness.calfit.domain.interview.command.model;

import java.time.LocalDateTime;
import java.util.List;

// 면접관 확정 요청 초대장 발송 결과
public record InterviewInvitationResult(
    Long interviewScheduleId,
    String message,
    LocalDateTime sentAt,
    List<InterviewInvitee> invitees) {}
