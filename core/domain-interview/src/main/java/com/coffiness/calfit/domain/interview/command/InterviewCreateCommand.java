package com.coffiness.calfit.domain.interview.command;

import com.coffiness.calfit.domain.interview.command.model.InterviewRound;
import java.time.LocalDateTime;
import java.util.List;

// 면접 일정 수동 생성용 커맨드
public record InterviewCreateCommand(
    Long recruitmentId,
    Long recruitmentStageId,
    InterviewRound round,
    List<Long> interviewerIds,
    List<Long> applicantIds,
    Long meetingRoomId,
    LocalDateTime scheduledAt,
    Integer durationMinutes,
    String memo) {}
