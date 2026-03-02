package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitee;
import java.time.LocalDateTime;
import java.util.List;

// 면접 초대장 발송 응답
public record InterviewInvitationResponse(
    Long interviewScheduleId,
    String message,
    LocalDateTime sentAt,
    List<InviteeResponse> invitees) {

  public static InterviewInvitationResponse from(InterviewInvitationResult result) {
    return new InterviewInvitationResponse(
        result.interviewScheduleId(),
        result.message(),
        result.sentAt(),
        result.invitees().stream().map(InviteeResponse::from).toList());
  }

  public record InviteeResponse(String targetType, Long targetId, String name, String email) {
    public static InviteeResponse from(InterviewInvitee invitee) {
      return new InviteeResponse(
          invitee.targetType(), invitee.targetId(), invitee.name(), invitee.email());
    }
  }
}
