package com.coffiness.calfit.domain;

import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.response.ScheduleDetailResponse;
import com.coffiness.calfit.response.ScheduleResponse;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleAttendeeRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
        ScheduleEntity scheduleEntity = ScheduleEntity.builder()
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

        if(request.attendeeIds() != null && !request.attendeeIds().isEmpty()) {

            List<ScheduleAttendeeEntity> attendees = request.attendeeIds().stream()
                    .map(attendeeId -> ScheduleAttendeeEntity.builder()
                            .scheduleId(savedSchedule.getId())
                            .attendeeId(attendeeId)
                            .build()
                    ).toList();

            scheduleAttendeeRepository.saveAll(attendees);
        }
    }

    public List<ScheduleResponse> getSchedules(long userId, LocalDateTime startDate, LocalDateTime endDate) {

        List<ScheduleEntity> schedules = scheduleRepository.findOverlappingSchedules(userId, startDate, endDate);

        return schedules.stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public ScheduleDetailResponse getDetailSchedule(long userId, Long scheduleId) {

        ScheduleEntity schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        List<Long> attendeeIds = scheduleAttendeeRepository.findByScheduleId(scheduleId)
                .stream()
                .map(ScheduleAttendeeEntity::getAttendeeId)
                .toList();

        boolean isAttendee = attendeeIds.stream()
                .map(memberId -> {
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
            location = meetingRoomRepository.findById(schedule.getRoomId())
                    .map(MeetingRoomEntity::getName)
                    .orElse("알 수 없는 장소");
        }

        List<String> attendees = attendeeIds.stream()
                .map(memberId -> {
                    // 멤버 ID 추출
                    Member member;
                    try {
                        member = memberReader.getMemberById(memberId);
                    } catch(Exception e) {
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

        return ScheduleDetailResponse.of(
                schedule,
                location,
                attendeeIds,
                attendees,
                applicantName
        );
    }
}
