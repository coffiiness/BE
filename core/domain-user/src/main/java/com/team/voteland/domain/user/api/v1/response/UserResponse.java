package com.team.voteland.domain.user.api.v1.response;

import com.team.voteland.domain.user.domain.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String email, String name, String role, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.email(), user.name(), user.role(), user.createdAt());
    }

}
