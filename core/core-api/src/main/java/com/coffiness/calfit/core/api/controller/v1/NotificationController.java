package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.NotificationUnreadCountResponse;
import com.coffiness.calfit.api.v1.response.NotificationResponse;
import com.coffiness.calfit.core.api.facade.notification.NotificationFacade;
import com.coffiness.calfit.core.support.Page;
import com.coffiness.calfit.domain.notification.NotificationPage;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
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

  @GetMapping("/unread-count")
  public ApiResponse<NotificationUnreadCountResponse> unreadCount(
      @AuthenticationPrincipal SecurityUser user) {
    return ApiResponse.success(
        new NotificationUnreadCountResponse(
            notificationFacade.countUnreadNotifications(user.userId())));
  }

  @GetMapping("/unread")
  public ApiResponse<Page<NotificationResponse>> unread(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    NotificationPage notifications =
        notificationFacade.getUnreadNotifications(user.userId(), page, size);
    return ApiResponse.success(
        new Page<>(
            notifications.contents().stream().map(NotificationResponse::from).toList(),
            notifications.hasNext()));
  }

  @GetMapping
  public ApiResponse<Page<NotificationResponse>> list(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    NotificationPage notifications =
        notificationFacade.getRecentNotifications(user.userId(), page, size);
    return ApiResponse.success(
        new Page<>(
            notifications.contents().stream().map(NotificationResponse::from).toList(),
            notifications.hasNext()));
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
