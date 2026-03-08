package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MeetingRoomReservationCreateRequest(
    @NotNull LocalDateTime startDatetime, @NotNull LocalDateTime endDatetime) {}
