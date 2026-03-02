package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.domain.interview.command.model.InterviewRound;
import com.coffiness.calfit.domain.interview.command.model.InterviewSchedule;
import java.time.LocalDateTime;
import java.util.List;

// 면접 일정 응답 DTO (초기 생성 시 status=PENDING, 전원 수락 시 status=CONFIRMED)
public record InterviewResponse(
    Long id,
    Long recruitmentId,
    Long recruitmentStageId,
    InterviewRound round,
    Long meetingRoomId,
    LocalDateTime scheduledAt,
    LocalDateTime endAt,
    Integer durationMinutes,
    String memo,
    InterviewStatus status,
    List<Long> interviewerIds,
    List<Long> applicantIds) {

  // 도메인 모델 -> 응답 DTO 변환
  public static InterviewResponse from(InterviewSchedule schedule) {
    return new InterviewResponse(
        schedule.id(),
        schedule.recruitmentId(),
        schedule.recruitmentStageId(),
        schedule.round(),
        schedule.meetingRoomId(),
        schedule.scheduledAt(),
        schedule.endAt(),
        schedule.durationMinutes(),
        schedule.memo(),
        schedule.status(),
        schedule.interviewerIds(),
        schedule.applicantIds());
  }
}
