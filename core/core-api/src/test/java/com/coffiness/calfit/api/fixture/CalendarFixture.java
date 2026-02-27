package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.request.CalendarConnectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
