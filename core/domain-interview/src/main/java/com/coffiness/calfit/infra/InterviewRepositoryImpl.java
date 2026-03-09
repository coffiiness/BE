package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.InterviewEventType;
import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantEntity;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.InterviewRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleHistoryEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleHistoryRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationRepository;
import com.coffiness.calfit.storage.db.core.user.UserEntity;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterviewRepositoryImpl implements InterviewRepository {

  private final MemberReader memberReader;
  private final MeetingRoomRepository meetingRoomRepository;
  private final MeetingRoomReservationRepository meetingRoomReservationRepository;
  private final InterviewScheduleRepository interviewScheduleRepository;
  private final InterviewScheduleHistoryRepository interviewScheduleHistoryRepository;
  private final InterviewScheduleInterviewerRepository interviewerRepository;
  private final InterviewScheduleApplicantRepository applicantMappingRepository;
  private final ScheduleRepository scheduleRepository;
  private final UserRepository userRepository;
  private final ApplicantRepository applicantRepository;

  @Override
  public boolean isHrMember(Long userId) {
    if (userId == null) {
      return false;
    }
    String tenantId = requireTenantId();
    Member member = memberReader.getMember(tenantId, userId);
    return member != null && member.memberType() == MemberType.HR;
  }

  @Override
  public int getMeetingRoomCapacity(Long meetingRoomId) {
    String tenantId = requireTenantId();
    return meetingRoomRepository
        .findByTenantIdAndIdAndStatus(tenantId, meetingRoomId, EntityStatus.ACTIVE)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND))
        .getCapacity();
  }

  @Override
  public List<MeetingRoomBusySlotRow> findMeetingRoomBusySlots(
      List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to) {
    if (meetingRoomIds == null || meetingRoomIds.isEmpty()) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<MeetingRoomBusySlotRow> result =
        meetingRoomReservationRepository
            .findAllByTenantIdAndMeetingRoomIdInAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
                tenantId,
                meetingRoomIds,
                List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE),
                to,
                from)
            .stream()
            .map(
                reservation ->
                    new MeetingRoomBusySlotRow(
                        reservation.getMeetingRoomId(),
                        reservation.getInterviewScheduleId() != null
                            ? reservation.getInterviewScheduleId()
                            : reservation.getId(),
                        reservation.getStartDatetime(),
                        reservation.getEndDatetime()))
            .toList();

    List<ScheduleEntity> calendarBusySchedules =
        scheduleRepository.findBusyOverlappingSchedulesByRoomIds(
            meetingRoomIds, from, to, EntityStatus.ACTIVE);

    List<MeetingRoomBusySlotRow> merged = new ArrayList<>(result);
    for (ScheduleEntity schedule : calendarBusySchedules) {
      merged.add(
          new MeetingRoomBusySlotRow(
              schedule.getRoomId(),
              schedule.getId(),
              schedule.getStartTime(),
              schedule.getEndTime()));
    }
    return merged;
  }

  @Override
  public List<InterviewerBusySlotRow> findInterviewerBusySlots(
      List<Long> interviewerIds, LocalDateTime from, LocalDateTime to) {
    if (interviewerIds == null || interviewerIds.isEmpty()) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<InterviewScheduleInterviewerEntity> mappings =
        interviewerRepository.findAllByTenantIdAndUserIdIn(tenantId, interviewerIds);

    Set<Long> scheduleIds =
        mappings.stream()
            .map(InterviewScheduleInterviewerEntity::getInterviewScheduleId)
            .collect(Collectors.toSet());
    if (scheduleIds.isEmpty()) {
      return List.of();
    }

    List<InterviewScheduleEntity> schedules =
        interviewScheduleRepository.findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
            tenantId, new ArrayList<>(scheduleIds), InterviewStatus.CANCELLED, to);

    Map<Long, InterviewScheduleEntity> scheduleMap = new HashMap<>();
    for (InterviewScheduleEntity schedule : schedules) {
      LocalDateTime start = schedule.getScheduledAt();
      LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes());
      if (start.isBefore(to) && from.isBefore(end)) {
        scheduleMap.put(schedule.getId(), schedule);
      }
    }

    List<InterviewerBusySlotRow> result = new ArrayList<>();
    for (InterviewScheduleInterviewerEntity mapping : mappings) {
      InterviewScheduleEntity schedule = scheduleMap.get(mapping.getInterviewScheduleId());
      if (schedule == null) {
        continue;
      }
      LocalDateTime start = schedule.getScheduledAt();
      LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes());
      result.add(new InterviewerBusySlotRow(mapping.getUserId(), schedule.getId(), start, end));
    }

    List<ScheduleEntity> calendarBusySchedules =
        scheduleRepository.findBusyOverlappingSchedulesByUserIds(
            interviewerIds, from, to, EntityStatus.ACTIVE);

    for (ScheduleEntity schedule : calendarBusySchedules) {
      result.add(
          new InterviewerBusySlotRow(
              schedule.getMemberId(),
              schedule.getId(),
              schedule.getStartTime(),
              schedule.getEndTime()));
    }
    return result;
  }

  @Override
  public List<InterviewScheduleCalendarRow> getSchedulesByRecruitmentId(
      Long recruitmentId, LocalDateTime from, LocalDateTime to) {
    if (recruitmentId == null || from == null || to == null || !from.isBefore(to)) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<InterviewScheduleEntity> schedules =
        interviewScheduleRepository
            .findAllByTenantIdAndRecruitmentIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNotOrderByScheduledAtAsc(
                tenantId, recruitmentId, from, to, InterviewStatus.CANCELLED);

    if (schedules.isEmpty()) {
      return List.of();
    }

    List<Long> scheduleIds = schedules.stream().map(InterviewScheduleEntity::getId).toList();

    Map<Long, List<InterviewScheduleInterviewerEntity>> interviewersByScheduleId =
        interviewerRepository
            .findAllByTenantIdAndInterviewScheduleIdIn(tenantId, scheduleIds)
            .stream()
            .collect(
                Collectors.groupingBy(InterviewScheduleInterviewerEntity::getInterviewScheduleId));

    Map<Long, List<InterviewScheduleApplicantEntity>> applicantsByScheduleId =
        applicantMappingRepository
            .findAllByTenantIdAndInterviewScheduleIdIn(tenantId, scheduleIds)
            .stream()
            .collect(
                Collectors.groupingBy(InterviewScheduleApplicantEntity::getInterviewScheduleId));

    List<Long> interviewerIds =
        interviewersByScheduleId.values().stream()
            .flatMap(List::stream)
            .map(InterviewScheduleInterviewerEntity::getUserId)
            .distinct()
            .toList();
    Map<Long, String> interviewerNameMap =
        interviewerIds.isEmpty()
            ? Map.of()
            : userRepository.findAllById(interviewerIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getName));

    List<Long> applicantIds =
        applicantsByScheduleId.values().stream()
            .flatMap(List::stream)
            .map(InterviewScheduleApplicantEntity::getApplicantId)
            .distinct()
            .toList();
    Map<Long, String> applicantNameMap =
        applicantIds.isEmpty()
            ? Map.of()
            : applicantRepository.findAllByTenantIdAndIdIn(tenantId, applicantIds).stream()
                .collect(Collectors.toMap(ApplicantEntity::getId, ApplicantEntity::getName));

    List<InterviewScheduleCalendarRow> result = new ArrayList<>();
    for (InterviewScheduleEntity schedule : schedules) {
      List<InterviewScheduleInterviewerEntity> interviewerMappings =
          interviewersByScheduleId.getOrDefault(schedule.getId(), List.of());
      List<InterviewScheduleApplicantEntity> applicantMappings =
          applicantsByScheduleId.getOrDefault(schedule.getId(), List.of());

      Long interviewerMemberId =
          interviewerMappings.isEmpty() ? null : interviewerMappings.get(0).getUserId();

      String interviewerName =
          interviewerMappings.stream()
              .map(InterviewScheduleInterviewerEntity::getUserId)
              .map(id -> interviewerNameMap.getOrDefault(id, "알 수 없는 사용자"))
              .distinct()
              .collect(Collectors.joining(", "));

      String applicantName =
          applicantMappings.stream()
              .map(InterviewScheduleApplicantEntity::getApplicantId)
              .map(id -> applicantNameMap.getOrDefault(id, "알 수 없는 지원자"))
              .distinct()
              .collect(Collectors.joining(", "));

      result.add(
          new InterviewScheduleCalendarRow(
              schedule.getId(),
              schedule.getScheduledAt(),
              schedule.getEndTime(),
              interviewerMemberId,
              interviewerName,
              applicantName,
              schedule.getMemo()));
    }
    return result;
  }

  @Override
  public Long createConfirmedSchedule(
      Long userId,
      Long recruitmentId,
      Long recruitmentStageId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo,
      List<Long> interviewerIds,
      List<Long> applicantIds) {
    lockResourcesForScheduleCreation(meetingRoomId, interviewerIds, applicantIds);

    LocalDateTime end = scheduledAt.plusMinutes(durationMinutes);
    if (hasMeetingRoomConflict(meetingRoomId, scheduledAt, end)
        || hasParticipantConflict(interviewerIds, applicantIds, scheduledAt, end)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    InterviewScheduleEntity scheduleEntity =
        new InterviewScheduleEntity(
            recruitmentId,
            recruitmentStageId,
            meetingRoomId,
            scheduledAt,
            durationMinutes,
            memo == null ? "" : memo,
            InterviewStatus.CONFIRMED);
    InterviewScheduleEntity saved = interviewScheduleRepository.save(scheduleEntity);

    List<InterviewScheduleInterviewerEntity> interviewerEntities = new ArrayList<>();
    for (Long interviewerId : interviewerIds) {
      interviewerEntities.add(new InterviewScheduleInterviewerEntity(saved.getId(), interviewerId));
    }
    interviewerRepository.saveAll(interviewerEntities);

    List<InterviewScheduleApplicantEntity> applicantEntities = new ArrayList<>();
    for (Long applicantId : applicantIds) {
      applicantEntities.add(new InterviewScheduleApplicantEntity(saved.getId(), applicantId));
    }
    applicantMappingRepository.saveAll(applicantEntities);

    Long reservationUserId =
        userId != null ? userId : interviewerIds.stream().findFirst().orElse(0L);
    MeetingRoomReservationEntity reservation =
        MeetingRoomReservationEntity.builder()
            .meetingRoomId(meetingRoomId)
            .userId(reservationUserId)
            .interviewScheduleId(saved.getId())
            .startDatetime(scheduledAt)
            .endDatetime(end)
            .reservationStatus(MeetingRoomStatus.RESERVED)
            .build();
    meetingRoomReservationRepository.save(reservation);

    Map<String, Object> eventData = new HashMap<>();
    eventData.put("recruitmentId", recruitmentId);
    eventData.put("recruitmentStageId", recruitmentStageId);
    eventData.put("meetingRoomId", meetingRoomId);
    eventData.put("scheduledAt", scheduledAt.toString());
    eventData.put("durationMinutes", durationMinutes);
    eventData.put("memo", memo == null ? "" : memo);
    eventData.put("interviewerIds", interviewerIds);
    eventData.put("applicantIds", applicantIds);

    interviewScheduleHistoryRepository.save(
        InterviewScheduleHistoryEntity.builder()
            .interviewScheduleId(saved.getId())
            .eventType(InterviewEventType.CREATED)
            .eventData(eventData)
            .createdBy(reservationUserId)
            .build());

    return saved.getId();
  }

  private void lockResourcesForScheduleCreation(
      Long meetingRoomId, List<Long> interviewerIds, List<Long> applicantIds) {
    String tenantId = requireTenantId();

    meetingRoomRepository
        .findByTenantIdAndId(tenantId, meetingRoomId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    List<Long> orderedInterviewerIds =
        interviewerIds == null
            ? List.of()
            : interviewerIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    if (!orderedInterviewerIds.isEmpty()) {
      userRepository.findAllByIdInOrderByIdAsc(orderedInterviewerIds);
    }

    List<Long> orderedApplicantIds =
        applicantIds == null
            ? List.of()
            : applicantIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    if (!orderedApplicantIds.isEmpty()) {
      applicantRepository.findAllByTenantIdAndIdInOrderByIdAsc(tenantId, orderedApplicantIds);
    }
  }

  private boolean hasParticipantConflict(
      List<Long> interviewerIds, List<Long> applicantIds, LocalDateTime start, LocalDateTime end) {
    String tenantId = requireTenantId();
    boolean hasInterviewerIds = interviewerIds != null && !interviewerIds.isEmpty();
    boolean hasApplicantIds = applicantIds != null && !applicantIds.isEmpty();
    if (!hasInterviewerIds && !hasApplicantIds) {
      return false;
    }

    if (hasInterviewerIds) {
      List<Long> interviewerScheduleIds =
          interviewerRepository.findAllByTenantIdAndUserIdIn(tenantId, interviewerIds).stream()
              .map(InterviewScheduleInterviewerEntity::getInterviewScheduleId)
              .toList();
      if (hasScheduleOverlap(tenantId, interviewerScheduleIds, start, end)) {
        return true;
      }
    }

    if (hasApplicantIds) {
      List<Long> applicantScheduleIds =
          applicantMappingRepository
              .findAllByTenantIdAndApplicantIdIn(tenantId, applicantIds)
              .stream()
              .map(InterviewScheduleApplicantEntity::getInterviewScheduleId)
              .toList();
      return hasScheduleOverlap(tenantId, applicantScheduleIds, start, end);
    }

    return false;
  }

  private boolean hasMeetingRoomConflict(
      Long meetingRoomId, LocalDateTime start, LocalDateTime end) {
    String tenantId = requireTenantId();
    long roomConflict =
        meetingRoomReservationRepository
            .countByTenantIdAndMeetingRoomIdAndReservationStatusInAndStartDatetimeBeforeAndEndDatetimeAfter(
                tenantId,
                meetingRoomId,
                List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE),
                end,
                start);
    return roomConflict > 0;
  }

  private boolean hasScheduleOverlap(
      String tenantId, List<Long> scheduleIds, LocalDateTime start, LocalDateTime end) {
    if (scheduleIds.isEmpty()) {
      return false;
    }
    List<InterviewScheduleEntity> schedules =
        interviewScheduleRepository.findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
            tenantId, scheduleIds, InterviewStatus.CANCELLED, end);

    for (InterviewScheduleEntity schedule : schedules) {
      LocalDateTime rowStart = schedule.getScheduledAt();
      LocalDateTime rowEnd = rowStart.plusMinutes(schedule.getDurationMinutes());
      if (rowStart.isBefore(end) && start.isBefore(rowEnd)) {
        return true;
      }
    }
    return false;
  }

  private String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return tenantId;
  }
}
