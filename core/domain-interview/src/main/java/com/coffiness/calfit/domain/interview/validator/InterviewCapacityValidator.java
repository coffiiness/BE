package com.coffiness.calfit.domain.interview.validator;

import com.coffiness.calfit.domain.interview.InterviewReader;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewCapacityValidator {

  private final InterviewReader interviewReader;

  public void validate(Long meetingRoomId, int totalParticipants) {

    int capacity = interviewReader.getMeetingRoomCapacity(meetingRoomId);

    if (capacity < totalParticipants) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }
}
