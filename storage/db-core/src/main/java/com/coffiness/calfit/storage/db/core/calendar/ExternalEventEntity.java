package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.core.enums.EventStatus;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
* 구글 캘린더 내부 개별 일정 엔티티
* */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "external_event")
public class ExternalEventEntity extends TenantBaseEntity {

    // ExternalCalendar Id
    @Column(name = "external_calendar_id", nullable = false)
    private Long externalCalendarId;

    // 구글 캘린더 고유 일정 ID
    @Column(name = "google_event_id", nullable = false)
    private String googleEventId;

    @Column(name = "title", length =  255)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // 종일 일정 여부
    @Column(name = "is_all_day", nullable = false)
    private boolean isAllDay;

    // 일정 상태 (취소됨은 시간 계산 시 제외)
    @Enumerated(EnumType.STRING)
    @Column(name = "status",  nullable = false, length = 20)
    private EventStatus status;

    @Builder
    public ExternalEventEntity(String tenantId, Long externalCalendarId, String googleEventId, String title, LocalDateTime startTime, LocalDateTime endTime, boolean isAllDay, EventStatus status) {
        super(tenantId);
        this.externalCalendarId = externalCalendarId;
        this.googleEventId = googleEventId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isAllDay = isAllDay;
        this.status = status;
    }
}
