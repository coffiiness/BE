package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.NotificationResponse;
import com.coffiness.calfit.core.api.facade.notification.NotificationFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationFacade notificationFacade;

  @GetMapping
  public ApiResponse<List<NotificationResponse>> list(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(defaultValue = "20") int limit) {
    return ApiResponse.success(
        notificationFacade.getRecentNotifications(user.userId(), limit).stream()
            .map(NotificationResponse::from)
            .toList());
  }

  @PatchMapping("/{notificationId}/read")
  public ApiResponse<NotificationResponse> read(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long notificationId) {
    return ApiResponse.success(
        NotificationResponse.from(
            notificationFacade.readNotification(user.userId(), notificationId)));
  }

  @PatchMapping("/read-all")
  public ApiResponse<?> readAll(@AuthenticationPrincipal SecurityUser user) {
    notificationFacade.readAllNotifications(user.userId());
    return ApiResponse.success();
  }
}
