package com.coffiness.calfit.domain;

import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.request.ScheduleUpdateRequest;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

  // TODO : 유저 관련 보안 예외 처리 삽입

  private final ScheduleReader scheduleReader;
  private final ScheduleStore scheduleStore;
  private final MeetingRoomRepository meetingRoomRepository;

  private final MemberReader memberReader;
  private final UserReader userReader;

  public void createSchedule(Long userId, ScheduleCreateRequest request) {
    Schedule newSchedule =
        new Schedule(
            null,
            userId,
            request.title(),
            request.description(),
            request.type(),
            request.startTime(),
            request.endTime(),
            request.isAllDay() != null ? request.isAllDay() : false,
            request.roomId(),
            request.isBusy() != null ? request.isBusy() : true,
            null);

    scheduleStore.store(newSchedule, request.attendeeIds());
  }

  @Transactional(readOnly = true)
  public List<ScheduleInfo> getSchedules(
      long userId, LocalDateTime startDate, LocalDateTime endDate) {

    List<Schedule> schedules = scheduleReader.findOverlappingSchedules(userId, startDate, endDate);

    return schedules.stream().map(ScheduleInfo::from).toList();
  }

  @Transactional(readOnly = true)
  public ScheduleDetailInfo getDetailSchedule(long userId, Long scheduleId) {

    Schedule schedule = scheduleReader.read(scheduleId);
    List<Long> attendeeIds = scheduleReader.readAttendeeIds(scheduleId);

    List<Member> members = memberReader.getMembersByIds(attendeeIds);

    boolean isAttendee = members.stream().anyMatch(member -> member.userId().equals(userId));

    if (!schedule.userId().equals(userId) && !isAttendee) {
      throw new IllegalArgumentException("해당 일정을 조회할 권한이 없습니다.");
    }

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
                  // 유저 이름 추출
                  UserInfo user = userMap.get(member.userId());
                  if (user == null || user.name() == null) {
                    return "이름 없는 사용자";
                  }
                  return String.format("%s (%s)", user.name(), member.memberType().name());
                })
            .toList();

    // 기본 내 일정은 지원자 이름이 없음
    String applicantName = null;

    return ScheduleDetailInfo.of(schedule, location, attendeeIds, attendees, applicantName);
  }

  public ScheduleDetailInfo updateSchedule(
      long userId, Long scheduleId, @Valid ScheduleUpdateRequest request) {

    Schedule schedule = scheduleReader.read(scheduleId);

    if (!schedule.userId().equals(userId)) {
      throw new IllegalArgumentException("해당 일정을 수정할 권한이 없습니다.");
    }

    Schedule updatedSchedule =
        new Schedule(
            schedule.id(),
            schedule.userId(),
            request.title(),
            request.description(),
            request.type(),
            request.startTime(),
            request.endTime(),
            request.isAllDay() != null ? request.isAllDay() : false,
            request.roomId(),
            request.isBusy() != null ? request.isBusy() : true,
            schedule.googleEventId());

    scheduleStore.update(updatedSchedule, request.attendeeIds());

    return getDetailSchedule(userId, scheduleId);
  }

  public void deleteSchedule(long userId, Long scheduleId) {

    Schedule schedule = scheduleReader.read(scheduleId);

    if (!schedule.userId().equals(userId)) {
      throw new IllegalArgumentException("해당 일정을 삭제할 권한이 없습니다.");
    }

    // 참석자 삭제
    // TODO : 일정 내에 있는 참석자는 Hard vs Soft? Soft라면 Repository에 'DELETED' 검증이 되어야 할 것
    scheduleStore.delete(schedule);
  }
}
