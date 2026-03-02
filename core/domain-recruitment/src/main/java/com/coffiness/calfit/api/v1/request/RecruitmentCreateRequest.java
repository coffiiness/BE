package com.coffiness.calfit.api.v1.request;

import com.coffiness.calfit.core.enums.CareerType;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentCreateRequest(
    String title,
    int targetCount,
    Long applicationTemplateId,
    String contents,
    LocalDateTime startDate,
    LocalDateTime endDate,
    CareerType careerType,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    Long leadGroupId,
    List<Long> referenceGroupIds,
    List<Long> interviewerIds,
    List<RecruitmentStageRequest> stages) {}
