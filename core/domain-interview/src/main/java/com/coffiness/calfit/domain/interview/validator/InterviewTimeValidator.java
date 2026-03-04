package com.coffiness.calfit.domain.interview.validator;

import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class InterviewTimeValidator {

  public void validate(LocalDateTime scheduledAt, Integer durationMinutes) {

    if (scheduledAt.isBefore(LocalDateTime.now())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    if (scheduledAt.getMinute() != 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    if (durationMinutes != 60) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }
}
