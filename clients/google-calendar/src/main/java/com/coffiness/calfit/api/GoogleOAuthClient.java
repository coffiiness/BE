package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleOAuthResponseDto;
import com.coffiness.calfit.model.GoogleTokenModel;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient implements GoogleOAuthPort {

  private final GoogleOAuthApi googleOAuthApi;

  @Value("${spring.security.oauth2.client.registration.google.client-id}")
  private String clientId;

  @Value("${spring.security.oauth2.client.registration.google.client-secret}")
  private String clientSecret;

  public GoogleTokenModel exchangeToken(String authCode, String redirectUri) {

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("code", authCode);
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("redirect_uri", redirectUri);
    // 구글 스펙상 고정값
    formData.add("grant_type", "authorization_code");

    GoogleOAuthResponseDto rawDto = googleOAuthApi.exchange(formData);

    String extractEmail = extractEmailFromIdToken(rawDto.idToken());

    return new GoogleTokenModel(
        rawDto.accessToken(), rawDto.refreshToken(), rawDto.expiresIn(), extractEmail);
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
