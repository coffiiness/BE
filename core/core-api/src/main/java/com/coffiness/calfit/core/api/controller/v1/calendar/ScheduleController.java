package com.coffiness.calfit.core.api.controller.v1.calendar;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleInfo;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import com.coffiness.calfit.v1.response.ScheduleDetailResponse;
import com.coffiness.calfit.v1.response.ScheduleResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

    long userId = user.userId();

    // 날짜에 시간을 붙여 서비스에 전달
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusNanos(1);

    List<ScheduleInfo> infos = scheduleService.getSchedules(userId, startDateTime, endDateTime);

    List<ScheduleResponse> response = infos.stream().map(ScheduleResponse::from).toList();

    return ApiResponse.success(response);
  }

  @GetMapping("/api/v1/schedules/{scheduleId}")
  public ApiResponse<ScheduleDetailResponse> getDetailSchedule(
      @AuthenticationPrincipal SecurityUser user, @PathVariable("scheduleId") Long scheduleId) {

    long userId = user.userId();

    ScheduleDetailInfo info = scheduleService.getDetailSchedule(userId, scheduleId);
    ScheduleDetailResponse response = ScheduleDetailResponse.from(info);

    return ApiResponse.success(response);
  }

  @PutMapping("/api/v1/schedules/{scheduleId}")
  public ApiResponse<ScheduleDetailResponse> updateSchedule(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable("scheduleId") Long scheduleId,
      @Valid @RequestBody ScheduleUpdateRequest request) {

    long userId = user.userId();

    ScheduleDetailInfo info = scheduleService.updateSchedule(userId, scheduleId, request);

    return ApiResponse.success(ScheduleDetailResponse.from(info));
  }

  @DeleteMapping("/api/v1/schedules/{scheduleId}")
  public ApiResponse<Void> deleteSchedule(
      @AuthenticationPrincipal SecurityUser user, @PathVariable("scheduleId") Long scheduleId) {

    long userId = user.userId();

    scheduleService.deleteSchedule(userId, scheduleId);
    return ApiResponse.success(null);
  }
}
