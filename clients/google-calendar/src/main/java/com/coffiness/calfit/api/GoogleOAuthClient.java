package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleOAuthResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    private final GoogleOAuthApi googleOAuthApi;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    public GoogleOAuthResponseDto getToken(String authCode) {

        // 구글 스펙상 고정값
        final String GRANT_TYPE = "authorization_code";

        return googleOAuthApi.exchange(
                authCode,
                clientId,
                clientSecret,
                redirectUri,
                GRANT_TYPE
        );
    }
}
