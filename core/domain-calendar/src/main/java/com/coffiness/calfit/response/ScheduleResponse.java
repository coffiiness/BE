package com.coffiness.calfit.response;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;

import java.time.format.DateTimeFormatter;

public record ScheduleResponse(
        Long id,
        String title,
        String date,
        String startTime,
        String endTime,
        ScheduleType type,
        String description,
        Long roomId
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static ScheduleResponse from(ScheduleEntity entity) {
        return new ScheduleResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getStartTime().format(DATE_FORMATTER),
                entity.getStartTime().format(TIME_FORMATTER),
                entity.getEndTime().format(TIME_FORMATTER),
                entity.getType(),
                entity.getDescription(),
                entity.getRoomId()
        );
    }
}
