package com.coffiness.calfit.infra.interview;

import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.domain.interview.command.model.TimeWindow;
import com.coffiness.calfit.domain.interview.port.InterviewAvailabilityPort;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewAvailabilityAdapter implements InterviewAvailabilityPort {

  private final ScheduleRepository scheduleRepository;
  private final InterviewScheduleInterviewerRepository interviewerRepository;
  private final InterviewScheduleApplicantRepository applicantRepository;
  private final InterviewScheduleRepository interviewScheduleRepository;
  private final MeetingRoomReservationRepository meetingRoomReservationRepository;

  @Override
  public List<TimeWindow> getInterviewerBusyWindows(
      String tenantId, List<Long> interviewerIds, LocalDateTime from, LocalDateTime to) {
    if (interviewerIds == null || interviewerIds.isEmpty()) {
      return List.of();
    }

    List<TimeWindow> result = new ArrayList<>();

    List<ScheduleEntity> schedules =
        scheduleRepository.findAllByTenantIdAndUserIdInAndStartTimeBeforeAndEndTimeAfter(
            tenantId, interviewerIds, to, from);
    for (ScheduleEntity schedule : schedules) {
      result.add(new TimeWindow(schedule.getStartTime(), schedule.getEndTime()));
    }

    List<InterviewScheduleInterviewerEntity> mappings =
        interviewerRepository.findAllByTenantIdAndUserIdIn(tenantId, interviewerIds);
    List<Long> scheduleIds =
        mappings.stream().map(InterviewScheduleInterviewerEntity::getInterviewScheduleId).toList();
    appendInterviewWindows(result, tenantId, scheduleIds, from, to);
    return result;
  }

  @Override
  public List<TimeWindow> getApplicantBusyWindows(
      String tenantId, List<Long> applicantIds, LocalDateTime from, LocalDateTime to) {
    if (applicantIds == null || applicantIds.isEmpty()) {
      return List.of();
    }

    List<TimeWindow> result = new ArrayList<>();
    List<InterviewScheduleApplicantEntity> mappings =
        applicantRepository.findAllByTenantIdAndApplicantIdIn(tenantId, applicantIds);
    List<Long> scheduleIds =
        mappings.stream().map(InterviewScheduleApplicantEntity::getInterviewScheduleId).toList();
    appendInterviewWindows(result, tenantId, scheduleIds, from, to);
    return result;
  }

  @Override
  public Map<Long, List<TimeWindow>> getMeetingRoomBusyWindows(
      String tenantId, List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to) {
    if (meetingRoomIds == null || meetingRoomIds.isEmpty()) {
      return Map.of();
    }

    List<MeetingRoomReservationEntity> reservations =
        meetingRoomReservationRepository
            .findAllByTenantIdAndMeetingRoomIdInAndStatusAndStartDatetimeBeforeAndEndDatetimeAfter(
                tenantId, meetingRoomIds, MeetingRoomStatus.RESERVED, to, from);

    Map<Long, List<TimeWindow>> result = new HashMap<>();
    for (MeetingRoomReservationEntity reservation : reservations) {
      Long roomId = reservation.getMeetingRoomId();
      LocalDateTime start = reservation.getStartDatetime();
      LocalDateTime end = reservation.getEndDatetime();
      result.computeIfAbsent(roomId, key -> new ArrayList<>()).add(new TimeWindow(start, end));
    }

    return result;
  }

  private void appendInterviewWindows(
      List<TimeWindow> target,
      String tenantId,
      List<Long> scheduleIds,
      LocalDateTime from,
      LocalDateTime to) {
    if (scheduleIds.isEmpty()) {
      return;
    }

    List<InterviewScheduleEntity> schedules =
        interviewScheduleRepository.findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
            tenantId, scheduleIds, InterviewStatus.CANCELLED, to);

    for (InterviewScheduleEntity schedule : schedules) {
      LocalDateTime start = schedule.getScheduledAt();
      LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes());
      if (end.isAfter(from)) {
        target.add(new TimeWindow(start, end));
      }
    }
  }
}
