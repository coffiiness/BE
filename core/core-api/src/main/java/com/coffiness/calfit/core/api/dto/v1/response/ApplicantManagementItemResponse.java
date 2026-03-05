package com.coffiness.calfit.core.api.dto.v1.response;

import java.time.LocalDateTime;

public record ApplicantManagementItemResponse(
    Long applicationId,
    Long applicantId,
    String applicantName,
    String applicantEmail,
    Long recruitmentId,
    String recruitmentTitle,
    String stageName,
    LocalDateTime nextInterviewAt,
    LocalDateTime appliedAt) {}
