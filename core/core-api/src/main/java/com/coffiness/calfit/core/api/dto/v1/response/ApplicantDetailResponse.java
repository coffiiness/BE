package com.coffiness.calfit.core.api.dto.v1.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApplicantDetailResponse(
    Long applicationId,
    Long applicantId,
    Long recruitmentId,
    String recruitmentTitle,
    String stageName,
    String name,
    String gender,
    LocalDate birthDate,
    String phone,
    String email,
    String shortBio,
    String portfolioUrl,
    LocalDateTime appliedAt) {}
