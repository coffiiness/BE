package com.team.voteland.core.api.controller.v1;

import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.domain.user.api.v1.request.LoginRequest;
import com.team.voteland.domain.user.api.v1.request.SignUpRequest;
import com.team.voteland.domain.user.api.v1.response.LoginResponse;
import com.team.voteland.domain.user.api.v1.response.UserResponse;
import com.team.voteland.domain.user.domain.User;
import com.team.voteland.domain.user.domain.UserService;
import com.team.voteland.support.security.jwt.SecurityUser;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ApiResponse<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        User user = userService.signUp(request.email(), request.password(), request.name());
        return ApiResponse.success(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UserService.LoginResult result = userService.login(request.email(), request.password());
        LoginResponse response = new LoginResponse(result.accessToken(), result.refreshToken(),
                UserResponse.from(result.user()));
        return ApiResponse.success(response);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal SecurityUser securityUser) {
        User user = userService.getUser(securityUser.userId());
        return ApiResponse.success(UserResponse.from(user));
    }

    @DeleteMapping("/me")
    public ApiResponse<?> deleteMe(@AuthenticationPrincipal SecurityUser securityUser) {
        userService.deleteUser(securityUser.userId());
        return ApiResponse.success();
    }

}
