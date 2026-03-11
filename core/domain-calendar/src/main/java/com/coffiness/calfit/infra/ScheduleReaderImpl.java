package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.Schedule;
import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleReader;
import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleReaderImpl implements ScheduleReader {

  private final ScheduleRepository scheduleRepository;
  private final ScheduleAttendeeRepository scheduleAttendeeRepository;
  private final MeetingRoomRepository meetingRoomRepository;
  private final MemberReader memberReader;
  private final UserReader userReader;

  @Override
  public List<Schedule> findOverlappingSchedules(
      Long userId, LocalDateTime startDate, LocalDateTime endDate) {
    return scheduleRepository.findOverlappingSchedules(userId, startDate, endDate).stream()
        .map(this::toDomain)
        .toList();
  }

  // 바쁜 일정 충돌 여부를 조회
  @Override
  public boolean existsBusyOverlappingScheduleConflict(
      List<Long> userIds, LocalDateTime startDate, LocalDateTime endDate) {
    if (userIds == null || userIds.isEmpty()) {
      return false;
    }

    return !scheduleRepository
        .findBusyOverlappingSchedulesByUserIds(userIds, startDate, endDate, EntityStatus.ACTIVE)
        .isEmpty();
  }

  // 특정 일정을 제외하고 바쁜 일정 충돌 여부를 조회
  @Override
  public boolean existsBusyOverlappingScheduleConflictExcludingSchedule(
      Long excludedScheduleId, List<Long> userIds, LocalDateTime startDate, LocalDateTime endDate) {
    if (excludedScheduleId == null || userIds == null || userIds.isEmpty()) {
      return false;
    }

    return !scheduleRepository
        .findBusyOverlappingSchedulesByUserIdsExcludingScheduleId(
            excludedScheduleId, userIds, startDate, endDate, EntityStatus.ACTIVE)
        .isEmpty();
  }

  @Override
  public Schedule read(Long scheduleId) {
    ScheduleEntity entity =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));
    return toDomain(entity);
  }

  @Override
  public Schedule readByGoogleEventId(String googleEventId) {
    return scheduleRepository
        .findByGoogleEventIdAndStatus(googleEventId, EntityStatus.ACTIVE)
        .map(this::toDomain)
        .orElse(null);
  }

  @Override
  public List<Schedule> findByReservationId(Long reservationId) {
    if (reservationId == null) {
      return List.of();
    }

    return scheduleRepository
        .findAllByReservationIdAndStatus(reservationId, EntityStatus.ACTIVE)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Long> readAttendeeIds(Long scheduleId) {
    return scheduleAttendeeRepository.findByScheduleId(scheduleId).stream()
        .map(ScheduleAttendeeEntity::getAttendeeId)
        .toList();
  }

  @Override
  public ScheduleDetailInfo readDetail(Long scheduleId) {
    Schedule schedule = read(scheduleId);
    List<Long> attendeeIds = readAttendeeIds(scheduleId);

    List<Member> members = memberReader.getMembersByUserIds(currentTenantId(), attendeeIds);

    String location = "지정된 장소 없음";
    if (schedule.roomId() != null) {
      location =
          meetingRoomRepository
              .findById(schedule.roomId())
              .map(MeetingRoomEntity::getName)
              .orElse("알 수 없는 장소");
    }

    List<Long> userIdsForLookup =
        Stream.concat(
                Stream.of(schedule.userId()),
                members.stream().map(Member::userId).filter(Objects::nonNull))
            .distinct()
            .toList();
    Map<Long, UserInfo> userMap =
        userReader.getUsers(userIdsForLookup).stream()
            .collect(Collectors.toMap(UserInfo::id, u -> u, (u1, u2) -> u1));

    Map<Long, Member> memberMap =
        members.stream()
            .filter(member -> member.userId() != null)
            .collect(Collectors.toMap(Member::userId, m -> m, (m1, m2) -> m1));

    List<String> attendees =
        attendeeIds.stream()
            .map(
                userId -> {
                  Member member = memberMap.get(userId);
                  if (member == null) {
                    return "알 수 없는 멤버 (" + userId + ")";
                  }
                  UserInfo user = userMap.get(member.userId());
                  if (user == null || user.name() == null) {
                    return "이름 없는 사용자";
                  }
                  return user.name();
                })
            .toList();

    UserInfo owner = userMap.get(schedule.userId());
    String ownerName = owner != null && owner.name() != null ? owner.name() : "알 수 없는 사용자";
    String applicantName = null;

    return ScheduleDetailInfo.of(
        schedule, location, ownerName, attendeeIds, attendees, applicantName);
  }

  private Schedule toDomain(ScheduleEntity entity) {
    return new Schedule(
        entity.getId(),
        entity.getUserId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getType(),
        entity.getStartTime(),
        entity.getEndTime(),
        entity.isAllDay(),
        entity.getRoomId(),
        entity.getReservationId(),
        entity.isBusy(),
        entity.getGoogleEventId());
  }

  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalStateException("워크스페이스 ID가 필요합니다.");
    }
    return tenantId;
  }
}
