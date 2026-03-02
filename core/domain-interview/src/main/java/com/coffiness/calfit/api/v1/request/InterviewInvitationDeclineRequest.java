package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotBlank;

// 면접 초대 거절 요청
public record InterviewInvitationDeclineRequest(
    @NotBlank(message = "거절 사유는 필수입니다.") String reason) {}
