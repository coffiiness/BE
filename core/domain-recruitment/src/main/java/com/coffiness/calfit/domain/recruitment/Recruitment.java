package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record Recruitment(
    Long id,
    Long creatorId,
    String title,
    String contents,
    RecruitmentStatus status,
    int targetCount,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Long applicationTemplateId,
    CareerType careerType,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    Long leadGroupId,
    List<Long> referenceGroupIds,
    List<Long> interviewerIds,
    List<RecruitmentStage> stages) {
  public Recruitment {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("채용 제목은 필수입니다.");
    }
    if (startDate == null || endDate == null) {
      throw new IllegalArgumentException("채용 일정(시작/종료)은 필수입니다.");
    }
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("마감일이 시작일보다 빠를 수 없습니다.");
    }
    if (targetCount < 1) {
      throw new IllegalArgumentException("채용 목표 인원은 최소 1명 이상이어야 합니다.");
    }
    if (stages == null || stages.isEmpty()) {
      throw new IllegalArgumentException("최소 1개의 채용 단계가 필요합니다.");
    }

    if (careerType == CareerType.EXPERIENCED) {
      if (minExperienceYears == null && maxExperienceYears == null) {
        throw new IllegalArgumentException("경력직 채용의 경우 경력 연차 범위가 필요합니다.");
      }
    }
  }

  // 채용 공고 상세 수정
  public Recruitment updateDetails(
      String newTitle,
      String newContents,
      int newTargetCount,
      LocalDateTime newStartDate,
      LocalDateTime newEndDate,
      CareerType newCareerType,
      Integer newMinExperienceYears,
      Integer newMaxExperienceYears,
      Long newLeadGroupId,
      List<Long> newReferenceGroupIds,
      List<Long> newInterviewerIds,
      List<RecruitmentStage> newStages) {
    return new Recruitment(
        this.id,
        this.creatorId,
        newTitle,
        newContents,
        this.status,
        newTargetCount,
        newStartDate,
        newEndDate,
        this.applicationTemplateId,
        newCareerType,
        newMinExperienceYears,
        newMaxExperienceYears,
        newLeadGroupId,
        newReferenceGroupIds,
        newInterviewerIds,
        newStages);
  }

  // 채용 공고 상태만 변경
  public Recruitment updateStatus(RecruitmentStatus newStatus) {
    return new Recruitment(
        this.id,
        this.creatorId,
        this.title,
        this.contents,
        newStatus,
        this.targetCount,
        this.startDate,
        this.endDate,
        this.applicationTemplateId,
        this.careerType,
        this.minExperienceYears,
        this.maxExperienceYears,
        this.leadGroupId,
        this.referenceGroupIds,
        this.interviewerIds,
        this.stages);
  }
}
