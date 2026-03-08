package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.domain.interview.InterviewReader;
import com.coffiness.calfit.domain.interview.InterviewValidator;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewValidatorImpl implements InterviewValidator {

  private final InterviewReader interviewReader;

  @Override
  public void validateHrMember(Long userId) {
    if (!interviewReader.isHrMember(userId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
  }

  @Override
  public void validateCreateInput(
      Long recruitmentId,
      InterviewRound round,
      Long meetingRoomId,
      List<Long> interviewerIds,
      List<Long> applicantIds,
      LocalDateTime scheduledAt,
      Integer durationMinutes) {
    if (recruitmentId == null
        || round == null
        || meetingRoomId == null
        || interviewerIds == null
        || interviewerIds.isEmpty()
        || applicantIds == null
        || applicantIds.isEmpty()
        || scheduledAt == null
        || durationMinutes == null
        || durationMinutes <= 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public void validateInterviewTime(LocalDateTime scheduledAt, Integer durationMinutes) {
    if (scheduledAt.isBefore(LocalDateTime.now())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (scheduledAt.getMinute() != 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    // Allow a single interview to span multiple contiguous 1-hour slots.
    if (durationMinutes < 60 || durationMinutes % 60 != 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public void validateCapacity(Long meetingRoomId, int totalParticipants) {
    int capacity = interviewReader.getMeetingRoomCapacity(meetingRoomId);
    if (capacity < totalParticipants) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public void validateFromDate(LocalDateTime from) {
    if (from == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (from.isBefore(LocalDateTime.now())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }
}
