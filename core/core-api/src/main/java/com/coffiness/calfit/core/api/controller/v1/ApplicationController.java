package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.api.v1.response.ApplicationCreateResponse;
import com.coffiness.calfit.api.v1.response.ApplicationDetailResponse;
import com.coffiness.calfit.api.v1.response.ApplicationSummaryResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.application.ApplicationService;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

  private final ApplicationService applicationService;

  @PostMapping("/api/public/v1/applications")
  public ApiResponse<ApplicationCreateResponse> createApplication(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody ApplicationCreateRequest request) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    Long applicationId = applicationService.create(request, user.userId());
    return ApiResponse.success(new ApplicationCreateResponse(applicationId));
  }

  @GetMapping("/api/v1/applications")
  public ApiResponse<List<ApplicationSummaryResponse>> listApplications(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(required = false) Long recruitmentId,
      @RequestParam(required = false, defaultValue = "false") boolean all) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if ("APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (all) {
      return ApiResponse.success(applicationService.listAll());
    }
    if (recruitmentId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR, "RECRUITMENT_ID_REQUIRED", null);
    }
    return ApiResponse.success(applicationService.listByRecruitment(recruitmentId));
  }

  @GetMapping("/api/v1/applications/{applicationId}")
  public ApiResponse<ApplicationDetailResponse> getApplicationDetail(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long applicationId) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if ("APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    return ApiResponse.success(applicationService.getDetail(applicationId));
  }
}
