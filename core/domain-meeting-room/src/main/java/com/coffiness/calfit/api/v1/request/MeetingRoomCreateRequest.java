package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MeetingRoomCreateRequest(
    @NotBlank(message = "회의실 이름은 필수입니다.")
    @Size(min = 2, max = 20, message = "회의실 이름은 2자 이상 20자 이하여야 합니다.")
    String name,

    @NotNull(message = "회의실 층수는 필수 입니다.")
    @Min(value = 1, message = "회의실 위치는 1층 이상이어야 합니다.")
    @Max(value = 100, message = "회의실 위치는 100층 이하여야 합니다.")
    Integer location,

    @NotNull(message = "수용 인원은 필수입니다.")
    @Min(value = 2, message = "수용 인원은 2 이상이어야 합니다.")
    Integer capacity) {}
