package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.api.v1.response.ApplicationCreateResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.application.ApplicationService;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
