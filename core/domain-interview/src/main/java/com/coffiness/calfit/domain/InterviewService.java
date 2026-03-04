package com.coffiness.calfit.domain;

import com.coffiness.calfit.domain.interview.InterviewReader;
import com.coffiness.calfit.domain.interview.InterviewStore;
import com.coffiness.calfit.domain.interview.command.InterviewCreateCommand;
import com.coffiness.calfit.domain.interview.validator.InterviewAuthorizationValidator;
import com.coffiness.calfit.domain.interview.validator.InterviewCapacityValidator;
import com.coffiness.calfit.domain.interview.validator.InterviewCreateValidator;
import com.coffiness.calfit.domain.interview.validator.InterviewTimeValidator;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewService {

  private final InterviewReader interviewReader;
  private final InterviewStore interviewStore;

  private final InterviewAuthorizationValidator authorizationValidator;
  private final InterviewCreateValidator createValidator;
  private final InterviewTimeValidator timeValidator;
  private final InterviewCapacityValidator capacityValidator;

  /* 면접 일정 생성 */
  @Transactional
  public Interview create(Long userId, InterviewCreateCommand command) {

    // 권한 검증
    authorizationValidator.validateHrMember(userId);

    // 기본 입력 검증
    createValidator.validate(command);

    // 시간 정책 검증
    timeValidator.validate(command.scheduledAt(), command.durationMinutes());

    // capacity 검증
    int totalParticipants = command.interviewerIds().size() + command.applicantIds().size();

    capacityValidator.validate(command.meetingRoomId(), totalParticipants);

    Long scheduleId =
        interviewStore.createConfirmedSchedule(
            userId,
            command.recruitmentId(),
            command.recruitmentStageId(),
            command.meetingRoomId(),
            command.scheduledAt(),
            command.durationMinutes(),
            command.memo(),
            command.interviewerIds(),
            command.applicantIds());

    return Interview.confirmed(
        scheduleId,
        command.recruitmentId(),
        command.recruitmentStageId(),
        command.round(),
        command.meetingRoomId(),
        command.scheduledAt(),
        command.durationMinutes(),
        command.memo(),
        command.interviewerIds(),
        command.applicantIds());
  }

  /* 면접관 + 회의실 시간 조회 */
  @Transactional(readOnly = true)
  public InterviewAvailability getAvailability(
      Long userId, LocalDateTime from, List<Long> meetingRoomIds, List<Long> interviewerIds) {

    authorizationValidator.validateHrMember(userId);

    validateFromDate(from);

    // 서버가 to를 계산
    LocalDateTime to = from.plusMonths(1);

    meetingRoomIds = meetingRoomIds == null ? List.of() : meetingRoomIds;
    interviewerIds = interviewerIds == null ? List.of() : interviewerIds;

    List<InterviewAvailability.MeetingRoomBusySlot> meetingRoomBusySlots =
        interviewReader.findMeetingRoomBusySlots(meetingRoomIds, from, to);

    List<InterviewAvailability.InterviewerBusySlot> interviewerBusySlots =
        interviewReader.findInterviewerBusySlots(interviewerIds, from, to);

    return new InterviewAvailability(meetingRoomBusySlots, interviewerBusySlots);
  }

  private void validateFromDate(LocalDateTime from) {

    if (from == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    // 과거 시작 금지
    if (from.isBefore(LocalDateTime.now())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }
}
