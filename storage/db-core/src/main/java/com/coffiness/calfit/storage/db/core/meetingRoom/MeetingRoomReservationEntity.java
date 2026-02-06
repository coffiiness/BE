package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.storage.db.core.TenancyEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_room_reservations")
@NoArgsConstructor
@Getter
public class MeetingRoomReservationEntity extends TenancyEntity {

    // 회의실 id
    @Column(name = "meeting_room_id", nullable = false)
    private Long meetingRoomId;

    // 사용자 id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 면접 일정 id
    @Column(name = "interview_schedule_id", nullable = false)
    private Long interviewScheduleId;

    // 예약한 시간(시작)
    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    // 예약한 시작(끝)
    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    // 회의실 예약 상태(예약됨, 취소됨)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MeetingRoomStatus status;

    @Builder
    public MeetingRoomReservationEntity(Long meetingRoomId, Long userId, Long interviewScheduleId,
            LocalDateTime startDatetime, LocalDateTime endDatetime, MeetingRoomStatus status) {
        this.meetingRoomId = meetingRoomId;
        this.userId = userId;
        this.interviewScheduleId = interviewScheduleId;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.status = status;
    }

}
