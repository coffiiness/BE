package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.ApplicantLoginRequest;
import com.coffiness.calfit.api.v1.request.ApplicantSignUpRequest;
import com.coffiness.calfit.api.v1.response.ApplicantLoginResponse;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.applicant.ApplicantPortalService;
import com.coffiness.calfit.domain.applicant.ApplicantPortalService.ApplicantLoginResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicantPortalController {

  private final ApplicantPortalService applicantPortalService;

  @PostMapping("/api/public/v1/applicants/signup")
  public ApiResponse<ApplicantResponse> signUp(
      @Valid @RequestBody ApplicantSignUpRequest request) {
    return ApiResponse.success(
        ApplicantResponse.from(
            applicantPortalService.signUp(request.email(), request.password(), request.name())));
  }

  @PostMapping("/api/public/v1/applicants/login")
  public ApiResponse<ApplicantLoginResponse> login(@Valid @RequestBody ApplicantLoginRequest request) {
    ApplicantLoginResult result = applicantPortalService.login(request.email(), request.password());
    return ApiResponse.success(
        new ApplicantLoginResponse(result.accessToken(), ApplicantResponse.from(result.applicant())));
  }
}
