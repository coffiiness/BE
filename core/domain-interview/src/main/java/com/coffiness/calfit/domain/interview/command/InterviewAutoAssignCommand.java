package com.coffiness.calfit.domain.interview.command;

import com.coffiness.calfit.domain.interview.command.model.InterviewRound;
import java.time.LocalDateTime;
import java.util.List;

// 면접 일정 자동 배정용 커맨드
public record InterviewAutoAssignCommand(
    Long recruitmentId,
    Long recruitmentStageId,
    InterviewRound round,
    List<Long> interviewerIds,
    List<Long> applicantIds,
    Integer durationMinutes,
    String memo,
    LocalDateTime searchStart,
    LocalDateTime searchEnd,
    Integer slotMinutes,
    Integer preferredLocation,
    Integer requiredCapacity) {

  // 자동 배정 결과를 수동 생성 커맨드 형태로 변환
  public InterviewCreateCommand toCreateCommand(Long meetingRoomId, LocalDateTime scheduledAt) {
    return new InterviewCreateCommand(
        recruitmentId,
        recruitmentStageId,
        round,
        interviewerIds,
        applicantIds,
        meetingRoomId,
        scheduledAt,
        durationMinutes,
        memo);
  }
}
