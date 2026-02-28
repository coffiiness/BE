package com.coffiness.calfit.core.api.controller.v1.calendar;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
