package com.coffiness.calfit.storage.db.core.interview;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository {

  record MeetingRoomBusySlotRow(
      Long meetingRoomId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  record InterviewerBusySlotRow(
      Long interviewerId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  boolean isHrMember(Long userId);

  int getMeetingRoomCapacity(Long meetingRoomId);

  List<MeetingRoomBusySlotRow> findMeetingRoomBusySlots(
      List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to);

  List<InterviewerBusySlotRow> findInterviewerBusySlots(
      List<Long> interviewerIds, LocalDateTime from, LocalDateTime to);

  Long createConfirmedSchedule(
      Long userId,
      Long recruitmentId,
      Long recruitmentStageId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo,
      List<Long> interviewerIds,
      List<Long> applicantIds);
}
