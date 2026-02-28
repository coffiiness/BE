package com.coffiness.calfit.response;

import com.coffiness.calfit.core.enums.ScheduleType;

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
}
