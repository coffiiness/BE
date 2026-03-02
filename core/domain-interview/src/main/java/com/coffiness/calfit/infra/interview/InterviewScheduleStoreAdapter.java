package com.coffiness.calfit.infra.interview;

import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.domain.interview.command.InterviewCreateCommand;
import com.coffiness.calfit.domain.interview.command.model.InterviewSchedule;
import com.coffiness.calfit.domain.interview.port.InterviewScheduleStorePort;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationRepository;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InterviewScheduleStoreAdapter implements InterviewScheduleStorePort {

  private final MeetingRoomRepository meetingRoomRepository;
  private final MemberRepository memberRepository;
  private final ApplicantRepository applicantRepository;
  private final InterviewScheduleRepository interviewScheduleRepository;
  private final InterviewScheduleInterviewerRepository interviewerRepository;
  private final InterviewScheduleApplicantRepository applicantMappingRepository;
  private final MeetingRoomReservationRepository meetingRoomReservationRepository;

  @Override
  @Transactional
  public InterviewSchedule create(String tenantId, Long userId, InterviewCreateCommand command) {
    // 확정 전 임시 일정(PENDING) 생성을 위한 자원 락 획득
    acquirePessimisticLocks(tenantId, command);
    // 락 구간에서 충돌 여부 재검증
    assertNoConflictInLockedSection(tenantId, command);

    InterviewScheduleEntity scheduleEntity =
        new InterviewScheduleEntity(
            command.recruitmentId(),
            command.recruitmentStageId(),
            command.meetingRoomId(),
            command.scheduledAt(),
            command.durationMinutes(),
            command.memo() == null ? "" : command.memo(),
            InterviewStatus.PENDING);

    InterviewScheduleEntity savedSchedule = interviewScheduleRepository.save(scheduleEntity);
    Long scheduleId = savedSchedule.getId();

    List<InterviewScheduleInterviewerEntity> interviewerEntities = new ArrayList<>();
    for (Long interviewerId : command.interviewerIds()) {
      interviewerEntities.add(new InterviewScheduleInterviewerEntity(scheduleId, interviewerId));
    }
    interviewerRepository.saveAll(interviewerEntities);

    List<InterviewScheduleApplicantEntity> applicantEntities = new ArrayList<>();
    for (Long applicantId : command.applicantIds()) {
      applicantEntities.add(new InterviewScheduleApplicantEntity(scheduleId, applicantId));
    }
    applicantMappingRepository.saveAll(applicantEntities);

    Long reservationUserId =
        userId != null ? userId : command.interviewerIds().stream().findFirst().orElse(0L);
    MeetingRoomReservationEntity reservation =
        MeetingRoomReservationEntity.builder()
            .meetingRoomId(command.meetingRoomId())
            .userId(reservationUserId)
            .interviewScheduleId(scheduleId)
            .startDatetime(command.scheduledAt())
            .endDatetime(command.scheduledAt().plusMinutes(command.durationMinutes()))
            .status(MeetingRoomStatus.RESERVED)
            .build();
    meetingRoomReservationRepository.save(reservation);

    return new InterviewSchedule(
        savedSchedule.getId(),
        command.recruitmentId(),
        command.recruitmentStageId(),
        command.round(),
        command.meetingRoomId(),
        command.scheduledAt(),
        command.durationMinutes(),
        command.memo(),
        InterviewStatus.PENDING,
        command.interviewerIds(),
        command.applicantIds());
  }

  private void acquirePessimisticLocks(String tenantId, InterviewCreateCommand command) {
    meetingRoomRepository
        .findByTenantIdAndId(tenantId, command.meetingRoomId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    if (command.interviewerIds() != null && !command.interviewerIds().isEmpty()) {
      Set<Long> requestedInterviewerIds = new HashSet<>(command.interviewerIds());
      Set<Long> existingInterviewerIds =
          memberRepository.findAllByTenantIdAndUserIdIn(tenantId, command.interviewerIds()).stream()
              .map(member -> member.getUserId())
              .collect(java.util.stream.Collectors.toSet());

      Set<Long> missingInterviewerIds = new HashSet<>(requestedInterviewerIds);
      missingInterviewerIds.removeAll(existingInterviewerIds);
      if (!missingInterviewerIds.isEmpty()) {
        throw new CoreException(
            ErrorType.VALIDATION_ERROR, Map.of("missingInterviewerIds", missingInterviewerIds));
      }
    }

    if (command.applicantIds() != null && !command.applicantIds().isEmpty()) {
      Set<Long> requestedApplicantIds = new HashSet<>(command.applicantIds());
      Set<Long> existingApplicantIds =
          applicantRepository.findAllByTenantIdAndIdIn(tenantId, command.applicantIds()).stream()
              .map(applicant -> applicant.getId())
              .collect(java.util.stream.Collectors.toSet());

      Set<Long> missingApplicantIds = new HashSet<>(requestedApplicantIds);
      missingApplicantIds.removeAll(existingApplicantIds);
      if (!missingApplicantIds.isEmpty()) {
        throw new CoreException(
            ErrorType.VALIDATION_ERROR, Map.of("missingApplicantIds", missingApplicantIds));
      }
    }
  }

  private void assertNoConflictInLockedSection(String tenantId, InterviewCreateCommand command) {
    LocalDateTime start = command.scheduledAt();
    LocalDateTime end = command.scheduledAt().plusMinutes(command.durationMinutes());

    // 회의실 예약 충돌 체크
    long roomConflict =
        meetingRoomReservationRepository
            .countByTenantIdAndMeetingRoomIdAndStatusAndStartDatetimeBeforeAndEndDatetimeAfter(
                tenantId, command.meetingRoomId(), MeetingRoomStatus.RESERVED, end, start);
    if (roomConflict > 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    // 면접관 일정 충돌 체크
    if (command.interviewerIds() != null && !command.interviewerIds().isEmpty()) {
      List<Long> scheduleIds =
          interviewerRepository
              .findAllByTenantIdAndUserIdIn(tenantId, command.interviewerIds())
              .stream()
              .map(InterviewScheduleInterviewerEntity::getInterviewScheduleId)
              .toList();
      if (!scheduleIds.isEmpty()) {
        List<InterviewScheduleEntity> schedules =
            interviewScheduleRepository.findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
                tenantId, scheduleIds, InterviewStatus.CANCELLED, end);

        for (InterviewScheduleEntity schedule : schedules) {
          LocalDateTime rowStart = schedule.getScheduledAt();
          LocalDateTime rowEnd = rowStart.plusMinutes(schedule.getDurationMinutes());
          if (rowStart.isBefore(end) && start.isBefore(rowEnd)) {
            throw new CoreException(ErrorType.VALIDATION_ERROR);
          }
        }
      }
    }

    // 지원자 일정 충돌 체크
    if (command.applicantIds() != null && !command.applicantIds().isEmpty()) {
      List<Long> scheduleIds =
          applicantMappingRepository
              .findAllByTenantIdAndApplicantIdIn(tenantId, command.applicantIds())
              .stream()
              .map(InterviewScheduleApplicantEntity::getInterviewScheduleId)
              .toList();
      if (!scheduleIds.isEmpty()) {
        List<InterviewScheduleEntity> schedules =
            interviewScheduleRepository.findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
                tenantId, scheduleIds, InterviewStatus.CANCELLED, end);

        for (InterviewScheduleEntity schedule : schedules) {
          LocalDateTime rowStart = schedule.getScheduledAt();
          LocalDateTime rowEnd = rowStart.plusMinutes(schedule.getDurationMinutes());
          if (rowStart.isBefore(end) && start.isBefore(rowEnd)) {
            throw new CoreException(ErrorType.VALIDATION_ERROR);
          }
        }
      }
    }
  }
}
