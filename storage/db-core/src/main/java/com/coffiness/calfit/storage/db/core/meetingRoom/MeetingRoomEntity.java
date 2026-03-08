package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "meeting_rooms",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "name"})})
@SQLDelete(
    sql =
        "UPDATE meeting_rooms SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("status = 'ACTIVE'")
@NoArgsConstructor
@Getter
public class MeetingRoomEntity extends TenantBaseEntity {

  // 회의실 이름
  @Column(name = "name", nullable = false)
  private String name;

  // 회의실 위치
  @Column(name = "location", nullable = false)
  private Integer location;

  // 수용 인원
  @Column(name = "capacity", nullable = false)
  private Integer capacity;

  @Builder
  public MeetingRoomEntity(String name, Integer location, Integer capacity) {
    this.name = name;
    this.location = location;
    this.capacity = capacity;
  }

  public void update(String name, Integer capacity) {
    this.name = name;
    this.capacity = capacity;
  }
}
