package com.coffiness.calfit.domain.interview.port;

import com.coffiness.calfit.domain.interview.command.InterviewCreateCommand;
import com.coffiness.calfit.domain.interview.command.model.InterviewSchedule;

// 면접 일정/배정 결과 저장 포트
public interface InterviewScheduleStorePort {
  InterviewSchedule create(String tenantId, Long userId, InterviewCreateCommand command);
}
