package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.storage.db.core.TenancyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_room")
@NoArgsConstructor
@Getter
public class MeetingRoomEntity extends TenancyEntity {

    // 워크스페이스 id
    @Column(name = "workspace_id", nullable = false)
    private Long workSpaceId;

    // 회의실 이름
    @Column(name = "name", nullable = false)
    private String name;

    // 회의실 위치
    @Column(name = "location", nullable = true)
    private String location;

    // 수용 인원
    @Column(name = "capacity", nullable = true)
    private Long capacity;

    @Builder
    public MeetingRoomEntity(Long workSpaceId, String name, String location, Long capacity) {
        this.workSpaceId = workSpaceId;
        this.name = name;
        this.location = location;
        this.capacity = capacity;
    }

}
