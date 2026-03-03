package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.response.InterviewScheduleResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentDetailResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.core.env.Environment;

public record RecruitmentFixture(BaseFixture base) {

  public static RecruitmentFixture create(Environment environment, ObjectMapper objectMapper) {
    return new RecruitmentFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<Void> createRecruitment(String token, RecruitmentCreateRequest request) {
    return base.post("/api/v1/recruitments", request, token, Void.class);
  }

  public ApiResponse<List<RecruitmentListResponse>> getRecruitmentList(String token) {
    ApiResponse<RecruitmentListResponse[]> response =
        base.get("/api/v1/recruitments", token, RecruitmentListResponse[].class);

    return ApiResponse.success(List.of(response.getData()));
  }

  public ApiResponse<RecruitmentDetailResponse> getRecruitmentDetail(
      String token, Long recruitmentId) {
    String url = String.format("/api/v1/recruitments/%s", recruitmentId);
    return base.get(url, token, RecruitmentDetailResponse.class);
  }

  public ApiResponse<List<InterviewScheduleResponse>> getRecruitmentSchedule(
      String token, Long recruitmentId, String yearMonth) {

    String url =
        String.format(
            "/api/v1/recruitments/%s/interview-schedules?yearMonth=%s", recruitmentId, yearMonth);
    ApiResponse<InterviewScheduleResponse[]> response =
        base.get(url, token, InterviewScheduleResponse[].class);

    return ApiResponse.success(List.of(response.getData()));
  }
}
