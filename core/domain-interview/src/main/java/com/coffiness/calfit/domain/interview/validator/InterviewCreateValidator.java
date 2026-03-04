package com.coffiness.calfit.domain.interview.validator;

import com.coffiness.calfit.domain.interview.command.InterviewCreateCommand;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class InterviewCreateValidator {

  public void validate(InterviewCreateCommand command) {

    if (command == null
        || command.recruitmentId() == null
        || command.round() == null
        || command.meetingRoomId() == null
        || command.interviewerIds() == null
        || command.interviewerIds().isEmpty()
        || command.applicantIds() == null
        || command.applicantIds().isEmpty()
        || command.scheduledAt() == null
        || command.durationMinutes() == null
        || command.durationMinutes() <= 0) {

      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }
}
