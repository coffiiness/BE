package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.core.enums.MeetingRoomActionType;
import com.coffiness.calfit.storage.db.core.TenancyEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_room_history")
@NoArgsConstructor
@Getter
public class MeetingRoomHistoryEntity extends TenancyEntity {

    // 워크스페이스 id
    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    // 회의실 id
    @Column(name = "meeting_room_id", nullable = false)
    private Long meetingRoomId;

    // 회의실 예약 id(예약 관련이면)
    @Column(name = "meeting_room_resevation_id", nullable = true)
    private Long meetingRoomReservationId;

    // 행위를 한 사용자 id
    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    // 이유
    @Column(name = "reason", nullable = true)
    private String reason;

    // 행위가 발생한 시간
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    // 행위 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private MeetingRoomActionType actionType;

    @Column(name = "detail_json", columnDefinition = "json")
    private String detailJson;

    @Builder
    public MeetingRoomHistoryEntity(String workspaceId, Long meetingRoomId, Long meetingRoomReservationId, Long actorId,
            LocalDateTime occurredAt, MeetingRoomActionType actionType) {
        this.workspaceId = workspaceId;
        this.meetingRoomId = meetingRoomId;
        this.meetingRoomReservationId = meetingRoomReservationId;
        this.actorId = actorId;
        this.occurredAt = occurredAt;
        this.actionType = actionType;
    }

}
