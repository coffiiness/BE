package com.coffiness.calfit.domain.interview;

import com.coffiness.calfit.domain.interview.command.InterviewAutoAssignCommand;
import com.coffiness.calfit.domain.interview.command.InterviewCreateCommand;
import com.coffiness.calfit.domain.interview.command.model.InterviewSchedule;
import com.coffiness.calfit.domain.interview.command.model.MeetingRoomCandidate;
import com.coffiness.calfit.domain.interview.command.model.TimeWindow;
import com.coffiness.calfit.domain.interview.port.InterviewAvailabilityPort;
import com.coffiness.calfit.domain.interview.port.InterviewScheduleStorePort;
import com.coffiness.calfit.domain.interview.port.MeetingRoomQueryPort;
import com.coffiness.calfit.domain.interview.port.MemberAuthorizationPort;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewScheduleService {

  // HR 권한 확인 포트
  private final MemberAuthorizationPort memberAuthorizationPort;
  // 면접관/지원자/회의실 바쁜 시간 조회 포트
  private final InterviewAvailabilityPort interviewAvailabilityPort;
  // 회의실 후보 조회 포트
  private final MeetingRoomQueryPort meetingRoomQueryPort;
  // 면접 일정 저장 포트
  private final InterviewScheduleStorePort interviewScheduleStorePort;

  // 지정된 시간/회의실로 면접 일정 생성
  @Transactional
  public InterviewSchedule createInterviewSchedule(
      String tenantId, Long userId, InterviewCreateCommand command) {
    assertHrMember(tenantId, userId);
    validateCreateCommand(command);

    TimeWindow candidate = buildCandidateWindow(command.scheduledAt(), command.durationMinutes());
    assertNoParticipantConflict(
        tenantId, command.interviewerIds(), command.applicantIds(), candidate);
    assertNoRoomConflict(tenantId, command.meetingRoomId(), candidate);

    return interviewScheduleStorePort.create(tenantId, userId, command);
  }

  // 가능한 가장 빠른 슬롯으로 면접 자동 배정
  @Transactional
  public InterviewSchedule autoAssignInterviewSchedule(
      String tenantId, Long userId, InterviewAutoAssignCommand command) {
    assertHrMember(tenantId, userId);
    validateAutoAssignCommand(command);

    List<MeetingRoomCandidate> rooms =
        meetingRoomQueryPort.findReservableRooms(
            tenantId, command.preferredLocation(), command.requiredCapacity());
    if (rooms.isEmpty()) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }

    List<TimeWindow> interviewerBusy =
        interviewAvailabilityPort.getInterviewerBusyWindows(
            tenantId, command.interviewerIds(), command.searchStart(), command.searchEnd());
    List<TimeWindow> applicantBusy =
        interviewAvailabilityPort.getApplicantBusyWindows(
            tenantId, command.applicantIds(), command.searchStart(), command.searchEnd());

    List<Long> roomIds = rooms.stream().map(MeetingRoomCandidate::id).toList();
    Map<Long, List<TimeWindow>> roomBusyMap =
        interviewAvailabilityPort.getMeetingRoomBusyWindows(
            tenantId, roomIds, command.searchStart(), command.searchEnd());

    rooms = rooms.stream().sorted(Comparator.comparing(MeetingRoomCandidate::id)).toList();
    Integer slotMinutes = command.slotMinutes() == null ? 30 : command.slotMinutes();

    LocalDateTime latestStart = command.searchEnd().minusMinutes(command.durationMinutes());
    for (LocalDateTime cursor = command.searchStart();
        !cursor.isAfter(latestStart);
        cursor = cursor.plusMinutes(slotMinutes)) {
      TimeWindow candidate = buildCandidateWindow(cursor, command.durationMinutes());

      if (hasOverlap(interviewerBusy, candidate) || hasOverlap(applicantBusy, candidate)) {
        continue;
      }

      for (MeetingRoomCandidate room : rooms) {
        List<TimeWindow> roomBusy = roomBusyMap.getOrDefault(room.id(), List.of());
        if (hasOverlap(roomBusy, candidate)) {
          continue;
        }

        InterviewCreateCommand createCommand = command.toCreateCommand(room.id(), cursor);
        return interviewScheduleStorePort.create(tenantId, userId, createCommand);
      }
    }

    throw new CoreException(ErrorType.NOT_FOUND);
  }

  // 참가자 일정 겹침 검증
  private void assertNoParticipantConflict(
      String tenantId, List<Long> interviewerIds, List<Long> applicantIds, TimeWindow candidate) {
    List<TimeWindow> interviewerBusy =
        interviewAvailabilityPort.getInterviewerBusyWindows(
            tenantId, interviewerIds, candidate.start(), candidate.end());
    List<TimeWindow> applicantBusy =
        interviewAvailabilityPort.getApplicantBusyWindows(
            tenantId, applicantIds, candidate.start(), candidate.end());

    if (hasOverlap(interviewerBusy, candidate) || hasOverlap(applicantBusy, candidate)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  // 회의실 예약 겹침 검증
  private void assertNoRoomConflict(String tenantId, Long meetingRoomId, TimeWindow candidate) {
    Map<Long, List<TimeWindow>> roomBusyMap =
        interviewAvailabilityPort.getMeetingRoomBusyWindows(
            tenantId, List.of(meetingRoomId), candidate.start(), candidate.end());

    List<TimeWindow> roomBusy = roomBusyMap.getOrDefault(meetingRoomId, List.of());
    if (hasOverlap(roomBusy, candidate)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  // 후보 시간 구간 생성
  private TimeWindow buildCandidateWindow(LocalDateTime start, Integer durationMinutes) {
    if (start == null || durationMinutes == null || durationMinutes <= 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return new TimeWindow(start, start.plusMinutes(durationMinutes));
  }

  // 시간 겹침 여부
  private boolean hasOverlap(List<TimeWindow> windows, TimeWindow candidate) {
    return windows.stream().anyMatch(candidate::overlaps);
  }

  // HR 멤버 권한 체크
  private void assertHrMember(String tenantId, Long userId) {
    if (tenantId == null || tenantId.isBlank() || userId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (!memberAuthorizationPort.isHrMember(tenantId, userId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
  }

  // 수동 생성 입력값 검증
  private void validateCreateCommand(InterviewCreateCommand command) {
    if (command == null
        || command.recruitmentId() == null
        || command.round() == null
        || command.meetingRoomId() == null
        || command.interviewerIds() == null
        || command.interviewerIds().isEmpty()
        || command.applicantIds() == null
        || command.applicantIds().isEmpty()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  // 자동 배정 입력값 검증
  private void validateAutoAssignCommand(InterviewAutoAssignCommand command) {
    if (command == null
        || command.recruitmentId() == null
        || command.round() == null
        || command.durationMinutes() == null
        || command.durationMinutes() <= 0
        || command.searchStart() == null
        || command.searchEnd() == null
        || !command.searchStart().isBefore(command.searchEnd())
        || command.interviewerIds() == null
        || command.interviewerIds().isEmpty()
        || command.applicantIds() == null
        || command.applicantIds().isEmpty()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (command.slotMinutes() != null && command.slotMinutes() <= 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }
}
