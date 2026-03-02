package com.coffiness.calfit.domain.interview.command.model;

// 초대 대상자 정보
public record InterviewInvitee(
    String targetType, // INTERVIEWER or APPLICANT
    Long targetId,
    String name,
    String email) {}
