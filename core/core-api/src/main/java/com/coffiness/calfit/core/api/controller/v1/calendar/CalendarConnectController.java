package com.coffiness.calfit.core.api.controller.v1.calendar;

import com.coffiness.calfit.core.api.facade.calendar.CalendarConnectFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import com.coffiness.calfit.v1.request.CalendarConnectRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalendarConnectController {

  private final CalendarConnectFacade calendarConnectFacade;

  // 계정 연동
  @PostMapping("/api/v1/calendars/google/connect")
  public ApiResponse<String> connectGoogleCalendar(
      @Valid @RequestBody CalendarConnectRequest request,
      @AuthenticationPrincipal SecurityUser securityUser) {
    String googleEmail =
        calendarConnectFacade.connectGoogleCalendar(
            securityUser.userId(), request.authCode(), request.redirectUri());

    return ApiResponse.success(googleEmail);
  }
}
