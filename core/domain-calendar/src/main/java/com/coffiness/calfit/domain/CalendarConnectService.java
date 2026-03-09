package com.coffiness.calfit.domain;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult.SyncEventModel;
import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
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
  private final GoogleCalendarPort googleCalendarPort;
  private final ExternalCalendarReader externalCalendarReader;
  private final ExternalCalendarStore externalCalendarStore;
  private final ScheduleService scheduleService;

  // 인가 코드 교환 후 사용자 구글 계정 기준으로 연동 토큰을 저장
  public String connectGoogleCalendar(String authCode, String redirectUri, Long userId) {

    OAuthExchangeResult exchangeResult =
        googleOAuthPort.exchangeAuthorizationCode(authCode, redirectUri);

    String googleEmail = extractEmailFromIdToken(exchangeResult.idToken());

    googleCalendarTokenService.upsertConnectedToken(userId, googleEmail, exchangeResult);

    return googleEmail;
  }

  // 구글 증분 동기화를 수행하고 이벤트별로 캘핏 일정에 반영한 뒤 syncToken을 갱신
  public void syncGoogleCalendar(Long userId, Long memberId) {
    ExternalCalendar externalCalendar = externalCalendarReader.readSyncEnabledByUserId(userId);

    if (externalCalendar == null) {
      return;
    }

    String accessToken = googleCalendarTokenService.getValidAccessToken(externalCalendar.id());

    GoogleCalendarSyncResult syncResult =
        googleCalendarPort.syncEvent(accessToken, externalCalendar.syncToken());

    if (syncResult == null) {
      return;
    }

    if (syncResult.items() != null) {
      for (SyncEventModel syncEvent : syncResult.items()) {
        syncSingleEvent(memberId, syncEvent);
      }
    }

    if (hasText(syncResult.nextSyncToken())) {
      externalCalendarStore.updateSyncToken(externalCalendar.id(), syncResult.nextSyncToken());
    }
  }

  // 단일 구글 이벤트를 상태별로 일정 처리
  private void syncSingleEvent(Long memberId, SyncEventModel syncEvent) {
    if (syncEvent == null || !hasText(syncEvent.googleEventId())) {
      return;
    }

    if ("cancelled".equalsIgnoreCase(syncEvent.status())) {
      scheduleService.deleteScheduleByGoogleEventId(memberId, syncEvent.googleEventId());
      return;
    }

    if (syncEvent.startTime() == null || syncEvent.endTime() == null) {
      return;
    }

    ScheduleSyncRequest request =
        new ScheduleSyncRequest(
            syncEvent.googleEventId(),
            hasText(syncEvent.summary()) ? syncEvent.summary() : "제목 없음",
            syncEvent.description(),
            ScheduleType.MEETING,
            syncEvent.startTime().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
            syncEvent.endTime().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
            syncEvent.allDay());

    scheduleService.upsertScheduleByGoogleEventId(memberId, request);
  }

  // idToken payload 에서 사용자 email 값을 추출
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

  // 문자열이 null 또는 공백인지 확인
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
