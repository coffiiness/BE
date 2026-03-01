package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.InterviewAutoAssignRequest;
import com.coffiness.calfit.api.v1.request.InterviewCreateRequest;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.interview.InterviewScheduleService;
import com.coffiness.calfit.domain.interview.command.model.InterviewSchedule;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interviews")
public class InterviewController {

  private final InterviewScheduleService interviewScheduleService;

  // 면접 일정 수동 생성
  @PostMapping
  public ApiResponse<InterviewResponse> create(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody InterviewCreateRequest request) {
    String tenantId = TenantContext.getTenantId();
    Long userId = user != null ? user.userId() : null;

    InterviewSchedule created =
        interviewScheduleService.createInterviewSchedule(tenantId, userId, request.toCommand());
    return ApiResponse.success(InterviewResponse.from(created));
  }

  // 회의실/면접관 일정 기반 자동 배정
  @PostMapping("/auto-assign")
  public ApiResponse<InterviewResponse> autoAssign(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody InterviewAutoAssignRequest request) {
    String tenantId = TenantContext.getTenantId();
    Long userId = user != null ? user.userId() : null;

    InterviewSchedule created =
        interviewScheduleService.autoAssignInterviewSchedule(tenantId, userId, request.toCommand());
    return ApiResponse.success(InterviewResponse.from(created));
  }
}
