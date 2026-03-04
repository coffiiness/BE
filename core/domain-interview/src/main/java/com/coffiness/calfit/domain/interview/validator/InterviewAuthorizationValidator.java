package com.coffiness.calfit.domain.interview.validator;

import com.coffiness.calfit.domain.interview.InterviewReader;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewAuthorizationValidator {

  private final InterviewReader interviewReader;

  public void validateHrMember(Long userId) {

    if (!interviewReader.isHrMember(userId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
  }
}
