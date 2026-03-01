package com.coffiness.calfit.domain;

import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.request.ScheduleUpdateRequest;
import com.coffiness.calfit.response.ScheduleDetailResponse;
import com.coffiness.calfit.response.ScheduleResponse;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

  // TODO : 유저 관련 보안 예외 처리 삽입

  private final ScheduleRepository scheduleRepository;
  private final ScheduleAttendeeRepository scheduleAttendeeRepository;
  private final MeetingRoomRepository meetingRoomRepository;

  private final MemberReader memberReader;
  private final UserReader userReader;

  public void createSchedule(Long userId, ScheduleCreateRequest request) {
    ScheduleEntity scheduleEntity =
        ScheduleEntity.builder()
            .userId(userId)
            .title(request.title())
            .description(request.description())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .isAllDay(request.isAllDay() != null ? request.isAllDay() : false)
            .roomId(request.roomId())
            .type(request.type())
            .isBusy(request.isBusy() != null ? request.isBusy() : true)
            .build();

    ScheduleEntity savedSchedule = scheduleRepository.save(scheduleEntity);

    if (request.attendeeIds() != null && !request.attendeeIds().isEmpty()) {

      List<ScheduleAttendeeEntity> attendees =
          request.attendeeIds().stream()
              .map(
                  attendeeId ->
                      ScheduleAttendeeEntity.builder()
                          .scheduleId(savedSchedule.getId())
                          .attendeeId(attendeeId)
                          .build())
              .toList();

      scheduleAttendeeRepository.saveAll(attendees);
    }
  }

  public List<ScheduleResponse> getSchedules(
      long userId, LocalDateTime startDate, LocalDateTime endDate) {

    List<ScheduleEntity> schedules =
        scheduleRepository.findOverlappingSchedules(userId, startDate, endDate);

    return schedules.stream().map(ScheduleResponse::from).toList();
  }

  public ScheduleDetailResponse getDetailSchedule(long userId, Long scheduleId) {

    ScheduleEntity schedule =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

    List<Long> attendeeIds =
        scheduleAttendeeRepository.findByScheduleId(scheduleId).stream()
            .map(ScheduleAttendeeEntity::getAttendeeId)
            .toList();

    boolean isAttendee =
        attendeeIds.stream()
            .map(
                memberId -> {
                  // DB에 없는 ID 요청 시 방지
                  try {
                    return memberReader.getMemberById(memberId);
                  } catch (Exception e) {
                    return null;
                  }
                })
            .filter(member -> member != null)
            .anyMatch(member -> member.userId().equals(userId));

    if (schedule.getUserId() != userId && !isAttendee) {
      throw new IllegalArgumentException("해당 일정을 조회할 권한이 없습니다.");
    }

    String location = "지정된 장소 없음";
    if (schedule.getRoomId() != null) {
      location =
          meetingRoomRepository
              .findById(schedule.getRoomId())
              .map(MeetingRoomEntity::getName)
              .orElse("알 수 없는 장소");
    }

    List<String> attendees =
        attendeeIds.stream()
            .map(
                memberId -> {
                  // 멤버 ID 추출
                  Member member;
                  try {
                    member = memberReader.getMemberById(memberId);
                  } catch (Exception e) {
                    return "알 수 없는 멤버 (" + memberId + ")";
                  }

                  if (member == null) {
                    return "알 수 없는 멤버 (" + memberId + ")";
                  }

                  // 유저 이름 추출
                  UserInfo user = userReader.getUser(member.userId());
                  if (user == null || user.name() == null) {
                    return "이름 없는 사용자";
                  }

                  return String.format("%s (%s)", user.name(), member.memberType().name());
                })
            .toList();

    // 기본 내 일정은 지원자 이름이 없음
    String applicantName = null;

    return ScheduleDetailResponse.of(schedule, location, attendeeIds, attendees, applicantName);
  }

  public ScheduleDetailResponse updateSchedule(
      long userId, Long scheduleId, @Valid ScheduleUpdateRequest request) {

    ScheduleEntity schedule =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

    if (schedule.getUserId() != userId) {
      throw new IllegalArgumentException("해당 일정을 수정할 권한이 없습니다.");
    }

    schedule.update(
        request.title(),
        request.description(),
        request.type(),
        request.startTime(),
        request.endTime(),
        request.isAllDay() != null ? request.isAllDay() : false,
        request.roomId(),
        request.isBusy() != null ? request.isBusy() : true);

    scheduleAttendeeRepository.deleteByScheduleId(scheduleId);

    if (request.attendeeIds() != null && !request.attendeeIds().isEmpty()) {
      List<ScheduleAttendeeEntity> newAttendees =
          request.attendeeIds().stream()
              .map(
                  attendeeId ->
                      ScheduleAttendeeEntity.builder()
                          .scheduleId(scheduleId)
                          .attendeeId(attendeeId)
                          .build())
              .toList();
      scheduleAttendeeRepository.saveAllAndFlush(newAttendees);
    }

    return getDetailSchedule(userId, scheduleId);
  }

  public void deleteSchedule(long userId, Long scheduleId) {

    ScheduleEntity schedule =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

    if (schedule.getUserId() != userId) {
      throw new IllegalArgumentException("해당 일정을 삭제할 권한이 없습니다.");
    }

    // 참석자 삭제
    // TODO : 일정 내에 있는 참석자는 Hard vs Soft? Soft라면 Repository에 'DELETED' 검증이 되어야 할 것
    scheduleAttendeeRepository.deleteByScheduleId(scheduleId);
    schedule.deleted();
  }
}
