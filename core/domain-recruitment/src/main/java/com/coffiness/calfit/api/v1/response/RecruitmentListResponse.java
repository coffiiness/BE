package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.recruitment.AssigneeSummary;
import com.coffiness.calfit.domain.recruitment.StageSummary;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentListResponse(
    Long id,
    String title,
    String leadGroupName, // 부서명
    CareerType careerType,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    LocalDateTime endDate, // D-DAY 계산용 마감일
    RecruitmentStatus status, // 상태 뱃지 표시용 상태
    List<StageSummary> stages, // 단계별 정보
    List<AssigneeSummary> assignees // 하단 담당자 프로필
    ) {}
