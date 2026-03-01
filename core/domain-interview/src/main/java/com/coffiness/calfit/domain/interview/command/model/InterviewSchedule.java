package com.coffiness.calfit.domain.interview.command.model;

import com.coffiness.calfit.core.enums.InterviewStatus;
import java.time.LocalDateTime;
import java.util.List;

// 생성/조회 시 사용하는 면접 일정 모델
public record InterviewSchedule(
    Long id,
    Long recruitmentId,
    Long recruitmentStageId,
    InterviewRound round,
    Long meetingRoomId,
    LocalDateTime scheduledAt,
    Integer durationMinutes,
    String memo,
    InterviewStatus status,
    List<Long> interviewerIds,
    List<Long> applicantIds) {

  // 면접 종료 시간 계산
  public LocalDateTime endAt() {
    return scheduledAt.plusMinutes(durationMinutes);
  }
}
