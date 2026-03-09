package com.coffiness.calfit.domain;

import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/*
 * 구글 캘린더 연동 서비스 클래스
 * 프론트에서 넘어온 authCode를 구글 토큰으로 교환 후 DB에 저장
 * */
@Service
@RequiredArgsConstructor
public class CalendarConnectService {

  private final GoogleOAuthPort googleOAuthPort;
  private final GoogleCalendarTokenService googleCalendarTokenService;

  public String connectGoogleCalendar(String authCode, String redirectUri, Long userId) {

    OAuthExchangeResult exchangeResult =
        googleOAuthPort.exchangeAuthorizationCode(authCode, redirectUri);

    String googleEmail = extractEmailFromIdToken(exchangeResult.idToken());

    googleCalendarTokenService.upsertConnectedToken(userId, googleEmail, exchangeResult);

    return googleEmail;
  }

  private String extractEmailFromIdToken(String idToken) {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }

    String[] splitToken = idToken.split("\\.");
    if (splitToken.length < 2) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }

    String decodedPayload = new String(Base64.getUrlDecoder().decode(splitToken[1]));

    ObjectMapper objectMapper = new ObjectMapper();
    try {
      JsonNode jsonNode = objectMapper.readTree(decodedPayload);
      JsonNode emailNode = jsonNode.get("email");
      if (emailNode == null || emailNode.asText().isBlank()) {
        throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
      }
      return emailNode.asText();
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE", e);
    }
  }
}
