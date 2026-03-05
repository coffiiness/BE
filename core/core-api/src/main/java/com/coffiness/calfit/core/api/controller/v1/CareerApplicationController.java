package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.core.api.dto.v1.request.CareerApplicationSubmitRequest;
import com.coffiness.calfit.core.api.dto.v1.response.CareerApplicationSubmitResponse;
import com.coffiness.calfit.core.api.service.ApplicantApplicationService;
import com.coffiness.calfit.core.support.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/careers")
public class CareerApplicationController {

  private final ApplicantApplicationService applicantApplicationService;

  @PostMapping("/{companySlugOrWorkspaceId}/recruitments/{recruitmentId}/apply")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<CareerApplicationSubmitResponse> submitApplication(
      @PathVariable String companySlugOrWorkspaceId,
      @PathVariable Long recruitmentId,
      @Valid @RequestBody CareerApplicationSubmitRequest request) {

    CareerApplicationSubmitResponse response =
        applicantApplicationService.submitCareerApplication(
            companySlugOrWorkspaceId, recruitmentId, request);
    return ApiResponse.success(response);
  }

  @PostMapping("/{companySlugOrWorkspaceId}/{recruitmentId}/apply")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<CareerApplicationSubmitResponse> submitApplicationAlias(
      @PathVariable String companySlugOrWorkspaceId,
      @PathVariable Long recruitmentId,
      @Valid @RequestBody CareerApplicationSubmitRequest request) {

    return submitApplication(companySlugOrWorkspaceId, recruitmentId, request);
  }
}
