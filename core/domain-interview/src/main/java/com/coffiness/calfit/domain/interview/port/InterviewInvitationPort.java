package com.coffiness.calfit.domain.interview.port;

import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationAcceptResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationDeclineResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationResult;

// 면접 초대장 발송 포트
public interface InterviewInvitationPort {
  InterviewInvitationResult sendInvitations(
      String tenantId, Long userId, Long interviewScheduleId, String message);

  InterviewInvitationAcceptResult acceptInvitation(
      String tenantId, Long interviewerUserId, Long interviewScheduleId);

  InterviewInvitationDeclineResult declineInvitation(
      String tenantId, Long interviewerUserId, Long interviewScheduleId, String declineReason);
}
