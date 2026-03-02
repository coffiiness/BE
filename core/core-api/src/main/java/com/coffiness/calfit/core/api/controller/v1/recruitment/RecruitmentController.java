package com.coffiness.calfit.core.api.controller.v1.recruitment;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.recruitment.RecruitmentService;
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
public class RecruitmentController {

  private final RecruitmentService recruitmentService;

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/api/v1/recruitments")
  public ApiResponse<Long> createRecruitment(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody RecruitmentCreateRequest request) {

    long userId = user.userId();

    Long RecruitmentId = recruitmentService.createRecruitment(userId, request);

    return ApiResponse.success(RecruitmentId);
  }
}
