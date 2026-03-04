package com.coffiness.calfit.domain.interview;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewReader {

  boolean isHrMember(Long userId);

  int getMeetingRoomCapacity(Long meetingRoomId);

  List<InterviewAvailability.MeetingRoomBusySlot> findMeetingRoomBusySlots(
      List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to);

  List<InterviewAvailability.InterviewerBusySlot> findInterviewerBusySlots(
      List<Long> interviewerIds, LocalDateTime from, LocalDateTime to);

  List<InterviewScheduleCalendarItem> getSchedulesByRecruitmentId(
      Long recruitmentId, LocalDateTime from, LocalDateTime to);
}
