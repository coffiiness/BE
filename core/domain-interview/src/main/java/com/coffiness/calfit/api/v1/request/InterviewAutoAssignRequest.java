package com.coffiness.calfit.api.v1.request;

import com.coffiness.calfit.domain.interview.command.InterviewAutoAssignCommand;
import com.coffiness.calfit.domain.interview.command.model.InterviewRound;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;

// 면접 일정 자동 배정 요청
public record InterviewAutoAssignRequest(
    @NotNull(message = "채용 공고 ID는 필수입니다.") Long recruitmentId,
    Long recruitmentStageId,
    @NotNull(message = "면접 차수는 필수입니다.") InterviewRound round,
    @NotEmpty(message = "면접관은 최소 1명 이상이어야 합니다.") List<Long> interviewerIds,
    @NotEmpty(message = "지원자는 최소 1명 이상이어야 합니다.") List<Long> applicantIds,
    @NotNull(message = "면접 시간(분)은 필수입니다.") @Positive(message = "면접 시간은 1분 이상이어야 합니다.")
        Integer durationMinutes,
    String memo,
    @NotNull(message = "검색 시작 시간은 필수입니다.") LocalDateTime searchStart,
    @NotNull(message = "검색 종료 시간은 필수입니다.") LocalDateTime searchEnd,
    @Positive(message = "슬롯 분 단위는 1 이상이어야 합니다.") Integer slotMinutes,
    Integer preferredLocation,
    Integer requiredCapacity) {

  // 요청 DTO -> 도메인 커맨드 변환
  public InterviewAutoAssignCommand toCommand() {
    return new InterviewAutoAssignCommand(
        recruitmentId,
        recruitmentStageId,
        round,
        interviewerIds,
        applicantIds,
        durationMinutes,
        memo,
        searchStart,
        searchEnd,
        slotMinutes,
        preferredLocation,
        requiredCapacity);
  }
}
