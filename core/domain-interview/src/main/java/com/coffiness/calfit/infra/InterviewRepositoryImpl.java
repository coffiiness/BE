package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.InterviewEventType;
import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
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
  public List<ApplicantBusySlotRow> findApplicantBusySlots(
      List<Long> applicantIds, LocalDateTime from, LocalDateTime to) {
    if (applicantIds == null || applicantIds.isEmpty()) {
      return List.of();
    }

    String tenantId = requireTenantId();
    List<InterviewScheduleApplicantEntity> mappings =
        applicantMappingRepository.findAllByTenantIdAndApplicantIdIn(tenantId, applicantIds);

    Set<Long> scheduleIds =
        mappings.stream()
            .map(InterviewScheduleApplicantEntity::getInterviewScheduleId)
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

    List<ApplicantBusySlotRow> result = new ArrayList<>();
    for (InterviewScheduleApplicantEntity mapping : mappings) {
      InterviewScheduleEntity schedule = scheduleMap.get(mapping.getInterviewScheduleId());
      if (schedule == null) {
        continue;
      }
      LocalDateTime start = schedule.getScheduledAt();
      LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes());
      result.add(new ApplicantBusySlotRow(mapping.getApplicantId(), schedule.getId(), start, end));
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
    Map<Long, String> interviewerNameMap = resolveInterviewerNameMap(interviewerIds);
    Map<Long, Long> interviewerMemberIdMap =
        resolveInterviewerMemberIdMap(tenantId, interviewerIds);

    List<Long> applicantIds =
        applicantsByScheduleId.values().stream()
            .flatMap(List::stream)
            .map(InterviewScheduleApplicantEntity::getApplicantId)
            .distinct()
            .toList();
    Map<Long, String> applicantNameMap = resolveApplicantNameMap(tenantId, applicantIds);

    List<InterviewScheduleCalendarRow> result = new ArrayList<>();
    for (InterviewScheduleEntity schedule : schedules) {
      List<InterviewScheduleInterviewerEntity> interviewerMappings =
          interviewersByScheduleId.getOrDefault(schedule.getId(), List.of());
      List<InterviewScheduleApplicantEntity> applicantMappings =
          applicantsByScheduleId.getOrDefault(schedule.getId(), List.of());

      Long interviewerMemberId =
          interviewerMappings.isEmpty()
              ? null
              : interviewerMemberIdMap.getOrDefault(
                  interviewerMappings.get(0).getUserId(), interviewerMappings.get(0).getUserId());

      String interviewerName =
          interviewerMappings.stream()
              .map(InterviewScheduleInterviewerEntity::getUserId)
              .map(id -> fallbackName(interviewerNameMap.get(id), "면접관#" + id))
              .distinct()
              .collect(Collectors.joining(", "));

      String applicantName =
          applicantMappings.stream()
              .map(InterviewScheduleApplicantEntity::getApplicantId)
              .map(id -> fallbackName(applicantNameMap.get(id), "지원자#" + id))
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
              fallbackName(schedule.getMemo(), "")));
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

  private Map<Long, String> resolveInterviewerNameMap(List<Long> interviewerIds) {
    if (interviewerIds == null || interviewerIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> resolved = new HashMap<>();

    Map<Long, Member> memberMap =
        memberReader.getMembersByIds(interviewerIds).stream()
            .collect(Collectors.toMap(Member::id, member -> member, (left, right) -> left));
    List<Long> memberUserIds =
        memberMap.values().stream()
            .map(Member::userId)
            .filter(id -> id != null)
            .distinct()
            .toList();

    Map<Long, String> userNameMap = new HashMap<>();
    if (!memberUserIds.isEmpty()) {
      userRepository
          .findAllById(memberUserIds)
          .forEach(
              user -> {
                if (user.getId() != null && hasText(user.getName())) {
                  userNameMap.put(user.getId(), user.getName());
                }
              });
    }

    memberMap.forEach(
        (memberId, member) -> {
          String userName = userNameMap.get(member.userId());
          if (hasText(userName)) {
            resolved.put(memberId, userName);
          }
        });

    List<Long> unresolvedIds =
        interviewerIds.stream()
            .filter(id -> id != null && !resolved.containsKey(id))
            .distinct()
            .toList();
    if (unresolvedIds.isEmpty()) {
      return resolved;
    }

    userRepository
        .findAllById(unresolvedIds)
        .forEach(
            user -> {
              if (user.getId() != null && hasText(user.getName())) {
                resolved.put(user.getId(), user.getName());
              }
            });

    return resolved;
  }

  private Map<Long, Long> resolveInterviewerMemberIdMap(
      String tenantId, List<Long> interviewerIds) {
    if (interviewerIds == null || interviewerIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, Long> resolved = new HashMap<>();
    memberReader
        .getMembersByIds(interviewerIds)
        .forEach(member -> resolved.put(member.id(), member.id()));

    for (Long interviewerId : interviewerIds) {
      if (interviewerId == null || resolved.containsKey(interviewerId)) {
        continue;
      }
      Member member = memberReader.getMember(tenantId, interviewerId);
      if (member != null && member.id() != null) {
        resolved.put(interviewerId, member.id());
      }
    }
    return resolved;
  }

  private Map<Long, String> resolveApplicantNameMap(String tenantId, List<Long> applicantIds) {
    if (applicantIds == null || applicantIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> resolved = new HashMap<>();
    applicantRepository
        .findByTenantIdAndIdIn(tenantId, applicantIds)
        .forEach(
            applicant -> {
              if (applicant.getId() != null && applicant.getName() != null) {
                resolved.put(applicant.getId(), applicant.getName());
              }
            });

    List<Long> unresolvedIds =
        applicantIds.stream()
            .filter(id -> id != null && !resolved.containsKey(id))
            .distinct()
            .toList();
    if (unresolvedIds.isEmpty()) {
      return resolved;
    }

    applicantRepository
        .findAllByIdIn(unresolvedIds)
        .forEach(
            applicant -> {
              if (applicant.getId() != null && hasText(applicant.getName())) {
                resolved.putIfAbsent(applicant.getId(), applicant.getName());
              }
            });
    return resolved;
  }

  private String fallbackName(String value, String fallback) {
    return hasText(value) ? value : fallback;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return tenantId;
  }
}
