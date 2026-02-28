package com.coffiness.calfit.core.api.controller.v1.calendar;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.CalendarConnectService;
import com.coffiness.calfit.request.CalendarConnectRequest;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalendarConnectController {

  private final CalendarConnectService calendarConnectService;

  @PostMapping("/api/v1/calendars/google/connect")
  public ApiResponse<String> connectGoogleCalendar(
      @Valid @RequestBody CalendarConnectRequest request,
      @AuthenticationPrincipal SecurityUser securityUser) {
    String googleEmail =
        calendarConnectService.connectGoogleCalendar(
            request.authCode(), request.redirectUri(), securityUser.userId());

    return ApiResponse.success(googleEmail);
  }
}
