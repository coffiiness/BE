package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;

public record RecruitmentFixture(BaseFixture base) {

  public static RecruitmentFixture create(Environment environment, ObjectMapper objectMapper) {
    return new RecruitmentFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<Void> createRecruitment(String token, RecruitmentCreateRequest request) {
    return base.post("/api/v1/recruitments", request, token, Void.class);
  }
}
