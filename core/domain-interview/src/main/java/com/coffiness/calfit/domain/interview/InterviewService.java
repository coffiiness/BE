package com.coffiness.calfit.domain.interview;

import com.coffiness.calfit.core.enums.InterviewRound;
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
  private final InterviewValidator interviewValidator;

  @Transactional
  public Interview create(
      Long userId,
      Long recruitmentId,
      Long recruitmentStageId,
      InterviewRound round,
      List<Long> interviewerIds,
      List<Long> applicantIds,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo) {

    interviewValidator.validateHrMember(userId);
    interviewValidator.validateCreateInput(
        recruitmentId,
        round,
        meetingRoomId,
        interviewerIds,
        applicantIds,
        scheduledAt,
        durationMinutes);
    interviewValidator.validateInterviewTime(scheduledAt, durationMinutes);

    int totalParticipants = interviewerIds.size() + applicantIds.size();
    interviewValidator.validateCapacity(meetingRoomId, totalParticipants);

    Long scheduleId =
        interviewStore.createConfirmedSchedule(
            userId,
            recruitmentId,
            recruitmentStageId,
            meetingRoomId,
            scheduledAt,
            durationMinutes,
            memo,
            interviewerIds,
            applicantIds);

    return Interview.confirmed(
        scheduleId,
        recruitmentId,
        recruitmentStageId,
        round,
        meetingRoomId,
        scheduledAt,
        durationMinutes,
        memo,
        interviewerIds,
        applicantIds);
  }

  @Transactional(readOnly = true)
  public InterviewAvailability getAvailability(
      Long userId, LocalDateTime from, List<Long> meetingRoomIds, List<Long> interviewerIds) {

    interviewValidator.validateHrMember(userId);
    interviewValidator.validateFromDate(from);

    LocalDateTime to = from.plusMonths(1);

    meetingRoomIds = meetingRoomIds == null ? List.of() : meetingRoomIds;
    interviewerIds = interviewerIds == null ? List.of() : interviewerIds;

    List<InterviewAvailability.MeetingRoomBusySlot> meetingRoomBusySlots =
        interviewReader.findMeetingRoomBusySlots(meetingRoomIds, from, to);

    List<InterviewAvailability.InterviewerBusySlot> interviewerBusySlots =
        interviewReader.findInterviewerBusySlots(interviewerIds, from, to);

    return new InterviewAvailability(meetingRoomBusySlots, interviewerBusySlots);
  }
}
