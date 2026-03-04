package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentDetailInfo(
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
    String shareUrl,
    RecruitmentStatus recruitmentStatus,
    List<InterviewerInfo> interviewers) {}
