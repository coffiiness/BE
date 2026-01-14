package com.team.voteland.domain.user.api.v1.response;

public record LoginResponse(String accessToken, String refreshToken, UserResponse user) {
}
