package com.coffiness.calfit.dto;

public record GoogleOAuthResponseDto(
    String accessToken, String refreshToken, Integer expiresIn, String scope, String tokenType, String idToken) {}
