package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_rooms")
@NoArgsConstructor
@Getter
public class MeetingRoomEntity extends TenantBaseEntity {

  // 회의실 이름
  @Column(name = "name", nullable = false)
  private String name;

  // 회의실 위치
  @Column(name = "location", nullable = true)
  private Integer location;

  // 수용 인원
  @Column(name = "capacity", nullable = true)
  private String capacity;

  @Builder
  public MeetingRoomEntity(String name, Integer location, String capacity) {
    this.name = name;
    this.location = location;
    this.capacity = capacity;
  }
}
