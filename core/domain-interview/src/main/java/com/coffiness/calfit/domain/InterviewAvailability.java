package com.coffiness.calfit.domain;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewAvailability(
    List<MeetingRoomBusySlot> meetingRoomBusySlots,
    List<InterviewerBusySlot> interviewerBusySlots) {

  public record MeetingRoomBusySlot(
      Long meetingRoomId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  public record InterviewerBusySlot(
      Long interviewerId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}
}
