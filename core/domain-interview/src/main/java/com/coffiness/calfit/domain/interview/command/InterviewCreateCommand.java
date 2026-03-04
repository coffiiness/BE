package com.coffiness.calfit.domain.interview.command;

import java.time.LocalDateTime;
import java.util.List;

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
