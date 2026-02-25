package com.coffiness.calfit.model;

public record GoogleTokenModel(String accessToken, String refreshToken, Integer expiresIn) {}
