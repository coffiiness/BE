package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 내부 내 일정 캘린더 엔티티
 * */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "schedule")
public class ScheduleEntity extends TenantBaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "schedule_type", nullable = false, length = 20)
  private ScheduleType type;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Column(name = "is_all_day", nullable = false)
  private boolean isAllDay;

  @Column(name = "room_id")
  private Long roomId;

  @Column(name = "is_busy")
  private boolean isBusy;

  @Column(name = "google_event_id", length = 255)
  private String googleEventId;

  @Builder
  public ScheduleEntity(
      String tenantId,
      Long userId,
      String title,
      String description,
      ScheduleType type,
      LocalDateTime startTime,
      LocalDateTime endTime,
      boolean isAllDay,
      Long roomId,
      String googleEventId,
      boolean isBusy) {
    super(tenantId);
    this.userId = userId;
    this.title = title;
    this.description = description;
    this.type = type;
    this.startTime = startTime;
    this.endTime = endTime;
    this.isAllDay = isAllDay;
    this.roomId = roomId;
    this.googleEventId = googleEventId;
    this.isBusy = isBusy;
  }

  public void update(
      String title,
      String description,
      ScheduleType type,
      LocalDateTime startTime,
      LocalDateTime endTime,
      boolean isAllDay,
      Long roomId,
      boolean isBusy) {
    this.title = title;
    this.description = description;
    this.type = type;
    this.startTime = startTime;
    this.endTime = endTime;
    this.isAllDay = isAllDay;
    this.roomId = roomId;
    this.isBusy = isBusy;
  }
}
