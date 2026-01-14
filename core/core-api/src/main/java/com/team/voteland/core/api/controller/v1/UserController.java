package com.team.voteland.core.api.controller.v1;

import com.team.voteland.domain.user.api.v1.request.LoginRequest;
import com.team.voteland.domain.user.api.v1.response.LoginResponse;
import com.team.voteland.domain.user.api.v1.request.SignUpRequest;
import com.team.voteland.domain.user.api.v1.response.UserResponse;
import com.team.voteland.domain.user.domain.User;
import com.team.voteland.domain.user.domain.UserService;
import com.team.voteland.support.security.jwt.SecurityUser;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        User user = userService.signUp(request.email(), request.password(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UserService.LoginResult result = userService.login(request.email(), request.password());
        LoginResponse response = new LoginResponse(result.accessToken(), result.refreshToken(),
                UserResponse.from(result.user()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal SecurityUser securityUser) {
        User user = userService.getUser(securityUser.userId());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal SecurityUser securityUser) {
        userService.deleteUser(securityUser.userId());
        return ResponseEntity.noContent().build();
    }

}
