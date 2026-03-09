package com.coffiness.calfit.core.api.facade.interview;

import com.coffiness.calfit.api.v1.request.InterviewCreateRequest;
import com.coffiness.calfit.domain.interview.Interview;
import com.coffiness.calfit.domain.interview.InterviewAvailability;
import com.coffiness.calfit.domain.interview.InterviewService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InterviewFacade {

  private final InterviewService interviewService;

  @Transactional(readOnly = true)
  public InterviewAvailability getAvailability(
      Long userId, LocalDateTime from, List<Long> meetingRoomIds, List<Long> interviewerIds) {
    return interviewService.getAvailability(userId, from, meetingRoomIds, interviewerIds);
  }

  @Transactional
  public Interview createInterview(Long userId, InterviewCreateRequest request) {
    return interviewService.create(
        userId,
        request.recruitmentId(),
        request.recruitmentStageId(),
        request.round(),
        request.interviewerIds(),
        request.applicantIds(),
        request.meetingRoomId(),
        request.scheduledAt(),
        request.durationMinutes(),
        request.memo());
  }
}
