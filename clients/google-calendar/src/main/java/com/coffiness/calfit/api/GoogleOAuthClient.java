package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleOAuthResponseDto;
import com.coffiness.calfit.model.GoogleTokenModel;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient implements GoogleOAuthPort {

  private final GoogleOAuthApi googleOAuthApi;

  @Value("${spring.security.oauth2.client.registration.google.client-id}")
  private String clientId;

  @Value("${spring.security.oauth2.client.registration.google.client-secret}")
  private String clientSecret;

  @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
  private String redirectUri;

  public GoogleTokenModel exchangeToken(String authCode) {

    // 구글 스펙상 고정값
    final String GRANT_TYPE = "authorization_code";

    GoogleOAuthResponseDto rawDto =
        googleOAuthApi.exchange(authCode, clientId, clientSecret, redirectUri, GRANT_TYPE);

    String extractEmail = extractEmailFromIdToken(rawDto.idToken());

    return new GoogleTokenModel(rawDto.accessToken(), rawDto.refreshToken(), rawDto.expiresIn(), extractEmail);
  }

  // id_token에서 email(calendarId) 추출
  private String extractEmailFromIdToken(String idToken) {
      String payload = idToken.split("\\.")[1];

      String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));

      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode jsonNode = null;
      try {
          jsonNode = objectMapper.readTree(decodedPayload);
      } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
      }

      return jsonNode.get("email").asText();
  }
}
