package com.coffiness.calfit.v1.request;

import com.coffiness.calfit.core.enums.ScheduleType;
import java.time.LocalDateTime;

public record ScheduleSyncRequest(
    String googleEventId,
    String title,
    String description,
    ScheduleType type,
    LocalDateTime startTime,
    LocalDateTime endTime,
    boolean isAllDay) {}
