package com.coffiness.calfit.infra;

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
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
      Long memberId, LocalDateTime startDate, LocalDateTime endDate) {
    return scheduleRepository.findOverlappingSchedules(memberId, startDate, endDate).stream()
        .map(this::toDomain)
        .toList();
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
  public List<Long> readAttendeeIds(Long scheduleId) {
    return scheduleAttendeeRepository.findByScheduleId(scheduleId).stream()
        .map(ScheduleAttendeeEntity::getAttendeeId)
        .toList();
  }

  @Override
  public ScheduleDetailInfo readDetail(Long scheduleId) {
    Schedule schedule = read(scheduleId);
    List<Long> attendeeIds = readAttendeeIds(scheduleId);

    List<Member> members = memberReader.getMembersByIds(attendeeIds);

    String location = "지정된 장소 없음";
    if (schedule.roomId() != null) {
      location =
          meetingRoomRepository
              .findById(schedule.roomId())
              .map(MeetingRoomEntity::getName)
              .orElse("알 수 없는 장소");
    }

    List<Long> userIdsForAttendees = members.stream().map(Member::userId).toList();
    Map<Long, UserInfo> userMap =
        userReader.getUsers(userIdsForAttendees).stream()
            .collect(Collectors.toMap(UserInfo::id, u -> u, (u1, u2) -> u1));

    Map<Long, Member> memberMap =
        members.stream().collect(Collectors.toMap(Member::id, m -> m, (m1, m2) -> m1));

    List<String> attendees =
        attendeeIds.stream()
            .map(
                memberId -> {
                  Member member = memberMap.get(memberId);
                  if (member == null) {
                    return "알 수 없는 멤버 (" + memberId + ")";
                  }
                  UserInfo user = userMap.get(member.userId());
                  if (user == null || user.name() == null) {
                    return "이름 없는 사용자";
                  }
                  return String.format("%s (%s)", user.name(), member.memberType().name());
                })
            .toList();

    String applicantName = null;

    return ScheduleDetailInfo.of(schedule, location, attendeeIds, attendees, applicantName);
  }

  private Schedule toDomain(ScheduleEntity entity) {
    return new Schedule(
        entity.getId(),
        entity.getMemberId(),
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
}
