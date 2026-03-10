package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.LoginRequest;
import com.coffiness.calfit.api.v1.request.SignUpRequest;
import com.coffiness.calfit.api.v1.response.LoginResponse;
import com.coffiness.calfit.api.v1.response.UserResponse;
import com.coffiness.calfit.api.v1.response.UserWorkspaceResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.user.User;
import com.coffiness.calfit.domain.user.UserService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping("/api/v1/users/signup")
  public ApiResponse<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
    User user = userService.signUp(request.email(), request.password(), request.name());
    return ApiResponse.success(UserResponse.from(user));
  }

  @PostMapping("/api/v1/users/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    UserService.LoginResult result = userService.login(request.email(), request.password());
    LoginResponse response =
        new LoginResponse(
            result.accessToken(),
            result.refreshToken(),
            UserResponse.from(result.user()),
            result.workspaceId());
    return ApiResponse.success(response);
  }

  @GetMapping("/api/v1/users/me")
  public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal SecurityUser securityUser) {
    User user = userService.getUser(securityUser.userId());
    return ApiResponse.success(UserResponse.from(user));
  }

  @GetMapping("/api/v1/users/me/workspace")
  public ApiResponse<UserWorkspaceResponse> getMyWorkspace(
      @AuthenticationPrincipal SecurityUser securityUser) {
    String workspaceId = userService.getWorkspaceId(securityUser.userId());
    return ApiResponse.success(new UserWorkspaceResponse(workspaceId));
  }

  @DeleteMapping("/api/v1/users/me")
  public ApiResponse<?> deleteMe(@AuthenticationPrincipal SecurityUser securityUser) {
    userService.deleteUser(securityUser.userId());
    return ApiResponse.success();
  }
}
