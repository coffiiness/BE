package com.coffiness.calfit.core.api.controller.v1.calendar;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.response.ScheduleDetailResponse;
import com.coffiness.calfit.response.ScheduleResponse;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/schedules")
    public ApiResponse<Void> createSchedule(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody ScheduleCreateRequest request) {

        long userId = user.userId();

        scheduleService.createSchedule(userId, request);

        return ApiResponse.success(null);
    }

    @GetMapping("/api/v1/schedules")
    public ApiResponse<List<ScheduleResponse>> getSchedules(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {

        long userId = user.userId();

        // 날짜에 시간을 붙여 서비스에 전달
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<ScheduleResponse> response = scheduleService.getSchedules(userId, startDateTime, endDateTime);

        return ApiResponse.success(response);
    }

    @GetMapping("/api/v1/schedules/{scheduleId}")
    public ApiResponse<ScheduleDetailResponse> getDetailSchedule(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable("scheduleId") Long scheduleId
    ) {

        long userId = user.userId();

        ScheduleDetailResponse response = scheduleService.getDetailSchedule(userId, scheduleId);

        return ApiResponse.success(response);

    }
}
