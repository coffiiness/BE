package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.InterviewAutoAssignRequest;
import com.coffiness.calfit.api.v1.request.InterviewCreateRequest;
import com.coffiness.calfit.api.v1.request.InterviewInvitationDeclineRequest;
import com.coffiness.calfit.api.v1.request.InterviewInvitationSendRequest;
import com.coffiness.calfit.api.v1.response.InterviewInvitationAcceptResponse;
import com.coffiness.calfit.api.v1.response.InterviewInvitationDeclineResponse;
import com.coffiness.calfit.api.v1.response.InterviewInvitationResponse;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.interview.InterviewScheduleService;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationAcceptResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationDeclineResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewSchedule;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interviews")
public class InterviewController {

  private final InterviewScheduleService interviewScheduleService;

  // 면접 일정 입력값으로 임시(PENDING) 일정 생성
  @PostMapping
  public ApiResponse<InterviewResponse> create(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody InterviewCreateRequest request) {
    String tenantId = TenantContext.getTenantId();
    Long userId = user != null ? user.userId() : null;

    InterviewSchedule pendingSchedule =
        interviewScheduleService.createInterviewSchedule(tenantId, userId, request.toCommand());
    return ApiResponse.success(InterviewResponse.from(pendingSchedule));
  }

  // 회의실/면접관 일정 기반으로 임시(PENDING) 일정 자동 배정
  @PostMapping("/auto-assign")
  public ApiResponse<InterviewResponse> autoAssign(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody InterviewAutoAssignRequest request) {
    String tenantId = TenantContext.getTenantId();
    Long userId = user != null ? user.userId() : null;

    InterviewSchedule pendingSchedule =
        interviewScheduleService.autoAssignInterviewSchedule(tenantId, userId, request.toCommand());
    return ApiResponse.success(InterviewResponse.from(pendingSchedule));
  }

  // 임시(PENDING) 일정에 대해 면접관 확정 요청 초대장 발송
  @PostMapping("/{interviewScheduleId}/invitations")
  public ApiResponse<InterviewInvitationResponse> sendInvitations(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long interviewScheduleId,
      @RequestBody(required = false) InterviewInvitationSendRequest request) {
    String tenantId = TenantContext.getTenantId();
    Long userId = user != null ? user.userId() : null;
    String message = request != null ? request.message() : null;

    InterviewInvitationResult result =
        interviewScheduleService.sendInvitations(tenantId, userId, interviewScheduleId, message);
    return ApiResponse.success(InterviewInvitationResponse.from(result));
  }

  // 면접관이 초대장을 수락 (전원 수락 시 CONFIRMED)
  @PostMapping("/{interviewScheduleId}/invitations/accept")
  public ApiResponse<InterviewInvitationAcceptResponse> acceptInvitation(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long interviewScheduleId) {
    String tenantId = TenantContext.getTenantId();
    Long userId = user != null ? user.userId() : null;

    InterviewInvitationAcceptResult result =
        interviewScheduleService.acceptInvitation(tenantId, userId, interviewScheduleId);
    return ApiResponse.success(InterviewInvitationAcceptResponse.from(result));
  }

  // 면접관이 초대장을 거절 (재조율을 위해 PENDING 유지)
  @PostMapping("/{interviewScheduleId}/invitations/decline")
  public ApiResponse<InterviewInvitationDeclineResponse> declineInvitation(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long interviewScheduleId,
      @Valid @RequestBody InterviewInvitationDeclineRequest request) {
    String tenantId = TenantContext.getTenantId();
    Long userId = user != null ? user.userId() : null;

    InterviewInvitationDeclineResult result =
        interviewScheduleService.declineInvitation(
            tenantId, userId, interviewScheduleId, request.reason());
    return ApiResponse.success(InterviewInvitationDeclineResponse.from(result));
  }
}
