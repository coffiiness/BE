package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.request.CalendarConnectRequest;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.request.ScheduleUpdateRequest;
import com.coffiness.calfit.response.ScheduleDetailResponse;
import com.coffiness.calfit.response.ScheduleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.core.env.Environment;

public record CalendarFixture(BaseFixture base) {

  public static CalendarFixture create(Environment environment, ObjectMapper objectMapper) {
    return new CalendarFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<String> connectGoogleCalendar(String token, String authCode) {
    CalendarConnectRequest request =
        new CalendarConnectRequest(authCode, "http://localhost:5173/auth/callback");

    return base.post("/api/v1/calendars/google/connect", request, token, String.class);
  }

  public ApiResponse<Void> createSchedule(String token, ScheduleCreateRequest request) {
    return base.post("/api/v1/schedules", request, token, Void.class);
  }

  public ApiResponse<List<ScheduleResponse>> getSchedules(
      String token, String startDate, String endDate) {
    String url = String.format("/api/v1/schedules?startDate=%s&endDate=%s", startDate, endDate);

    ApiResponse<ScheduleResponse[]> response = base.get(url, token, ScheduleResponse[].class);

    // TODO : 에러 났을 때 에러 반환 로직 필요 (Response에 ErrorType 부재)

    return ApiResponse.success(List.of(response.getData()));
  }

  public ApiResponse<ScheduleDetailResponse> getDetailSchedule(String token, Long scheduleId) {
    String url = String.format("/api/v1/schedules/%d", scheduleId);
    return base.get(url, token, ScheduleDetailResponse.class);
  }

  public ApiResponse<Void> updateSchedule(
      String token, Long scheduleId, ScheduleUpdateRequest request) {
    String url = String.format("/api/v1/schedules/%d", scheduleId);

    return base.put(url, request, token, Void.class);
  }

  public ApiResponse<Void> deleteSchedule(String token, Long scheduleId) {
    String url = String.format("/api/v1/schedules/%d", scheduleId);

    return base.delete(url, token, Void.class);
  }
}
