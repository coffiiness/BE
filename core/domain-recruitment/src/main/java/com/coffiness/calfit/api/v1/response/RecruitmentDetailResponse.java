package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.recruitment.InterviewerInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentDetailInfo;
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
    String shareUrl,
    RecruitmentStatus recruitmentStatus,
    List<InterviewerInfo> interviewers) {

  public static RecruitmentDetailResponse from(RecruitmentDetailInfo info) {
    return new RecruitmentDetailResponse(
        info.id(),
        info.title(),
        info.leadGroupName(),
        info.careerType(),
        info.minExperienceYears(),
        info.maxExperienceYears(),
        info.startDate(),
        info.endDate(),
        info.totalApplicants(),
        info.processingInterview(),
        info.dDay(),
        info.shareUrl(),
        info.recruitmentStatus(),
        info.interviewers().stream()
            .map(i -> new InterviewerInfo(i.memberId(), i.name(), i.enabled()))
            .toList());
  }
}
