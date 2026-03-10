package com.coffiness.calfit.storage.db.core.interview;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository {

  record MeetingRoomBusySlotRow(
      Long meetingRoomId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  record InterviewerBusySlotRow(
      Long interviewerId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  record ApplicantBusySlotRow(
      Long applicantId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  record InterviewScheduleCalendarRow(
      Long interviewScheduleId,
      LocalDateTime startAt,
      LocalDateTime endAt,
      Long interviewerMemberId,
      String interviewerName,
      String applicantName,
      String description) {}

  boolean isHrMember(Long userId);

  int getMeetingRoomCapacity(Long meetingRoomId);

  List<MeetingRoomBusySlotRow> findMeetingRoomBusySlots(
      List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to);

  List<InterviewerBusySlotRow> findInterviewerBusySlots(
      List<Long> interviewerIds, LocalDateTime from, LocalDateTime to);

  List<ApplicantBusySlotRow> findApplicantBusySlots(
      List<Long> applicantIds, LocalDateTime from, LocalDateTime to);

  List<InterviewScheduleCalendarRow> getSchedulesByRecruitmentId(
      Long recruitmentId, LocalDateTime from, LocalDateTime to);

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

  void cancelConfirmedSchedule(Long userId, Long interviewScheduleId);
}
