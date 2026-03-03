package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.recruitment.InterviewerInfo;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentDetailResponse(
    Long id,
    String title,
    String leadGroupName,
    CareerType careerType,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    LocalDateTime startDate,
    LocalDateTime endDate,
    int totalApplicants,
    int processingInterview,
    int dDay,
    String ShareUrl,
    RecruitmentStatus recruitmentStatus,
    List<InterviewerInfo> interviewers) {}
