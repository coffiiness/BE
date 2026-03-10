package com.coffiness.calfit.domain;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult.SyncEventModel;
import com.coffiness.calfit.model.GoogleCalendarWatchResult;
import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;

/*
 * 구글 캘린더 연동 서비스 클래스
 * 프론트에서 넘어온 authCode를 구글 토큰으로 교환 후 DB에 저장
 * */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarConnectService {

  private final GoogleOAuthPort googleOAuthPort;
  private final GoogleCalendarTokenService googleCalendarTokenService;
  private final GoogleCalendarPort googleCalendarPort;
  private final ExternalCalendarReader externalCalendarReader;
  private final ExternalCalendarStore externalCalendarStore;
  private final ScheduleService scheduleService;
  private final GoogleChannelTokenService googleChannelTokenService;

  @Value(
      "${calendar.google-sync.watch-callback-url:http://localhost:8080/api/v1/calendars/google/notifications}")
  private String watchCallbackUrl;

  @Value("${calendar.google-sync.watch-ttl-seconds:604800}")
  private Long watchTtlSeconds;

  // 인가 코드 교환 후 사용자 구글 계정 기준으로 연동 토큰을 저장
  public String connectGoogleCalendar(
      String authCode, String redirectUri, Long userId, String tenantId) {
    OAuthExchangeResult exchangeResult =
        googleOAuthPort.exchangeAuthorizationCode(authCode, redirectUri);

    String googleEmail = extractEmailFromIdToken(exchangeResult.idToken());

    googleCalendarTokenService.upsertConnectedToken(userId, googleEmail, exchangeResult);
    registerWatchChannel(userId, googleEmail, tenantId);

    return googleEmail;
  }

  // 수동 동기화 시 watch 채널이 없거나 만료 임박이면 자동 재등록
  public void ensureWatchChannel(Long userId, String tenantId) {
    ExternalCalendar externalCalendar = externalCalendarReader.readSyncEnabledByUserId(userId);
    if (externalCalendar == null) {
      return;
    }

    if (isWatchChannelActive(externalCalendar)) {
      return;
    }

    registerWatchChannel(userId, externalCalendar.calendarId(), tenantId);
  }

  // 구글 증분 동기화를 수행하고 이벤트별로 캘핏 일정에 반영한 뒤 syncToken을 갱신
  public void syncGoogleCalendar(Long userId, Long memberId) {
    ExternalCalendar externalCalendar = externalCalendarReader.readSyncEnabledByUserId(userId);
    if (externalCalendar == null) {
      return;
    }

    syncExternalCalendar(memberId, externalCalendar);
  }

  // 웹훅으로 들어온 특정 외부캘린더 기준 동기화
  public void syncGoogleCalendarByExternalCalendarId(Long externalCalendarId, Long memberId) {
    ExternalCalendar externalCalendar = externalCalendarReader.read(externalCalendarId);
    if (externalCalendar == null || !externalCalendar.isSyncEnabled()) {
      return;
    }

    syncExternalCalendar(memberId, externalCalendar);
  }

  // 지정된 외부 캘린더를 기준으로 구글 증분 동기화를 수행
  private void syncExternalCalendar(Long memberId, ExternalCalendar externalCalendar) {
    String accessToken = googleCalendarTokenService.getValidAccessToken(externalCalendar.id());

    GoogleCalendarSyncResult syncResult =
        syncGoogleEventsWithRecovery(accessToken, externalCalendar);

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

  // 구글 watch 채널을 등록하고 채널 메타데이터를 저장
  private void registerWatchChannel(Long userId, String calendarId, String tenantId) {
    if (!hasText(tenantId)) {
      log.warn("watch 등록을 건너뜁니다: tenantId가 비어 있습니다.");
      return;
    }

    ExternalCalendar externalCalendar =
        externalCalendarReader.readByUserIdAndCalendarId(userId, calendarId);

    if (externalCalendar == null) {
      log.warn(
          "watch 등록 대상 연동 정보를 찾을 수 없습니다. userId={}, calendarId={}",
          userId,
          calendarId);
      return;
    }

    try {
      String accessToken = googleCalendarTokenService.getValidAccessToken(externalCalendar.id());
      String channelToken = googleChannelTokenService.createToken(tenantId, externalCalendar.id());

      GoogleCalendarWatchResult watchResult =
          googleCalendarPort.watchEvents(
              accessToken, watchCallbackUrl, channelToken, resolveWatchTtlSeconds());

      externalCalendarStore.updateWatchChannel(
          externalCalendar.id(),
          watchResult.channelId(),
          watchResult.resourceId(),
          watchResult.channelExpiresAt());
    } catch (RuntimeException e) {
      log.warn(
          "watch 등록에 실패했습니다. userId={}, externalCalendarId={}, callbackUrl={}",
          userId,
          externalCalendar.id(),
          watchCallbackUrl,
          e);
    }
  }

  // 저장된 watch 채널이 존재하고 만료 임계시점 이후인지 확인
  private boolean isWatchChannelActive(ExternalCalendar externalCalendar) {
    if (!hasText(externalCalendar.channelId()) || !hasText(externalCalendar.channelResourceId())) {
      return false;
    }

    if (externalCalendar.channelExpiresAt() == null) {
      return false;
    }

    LocalDateTime refreshThreshold = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5);
    return externalCalendar.channelExpiresAt().isAfter(refreshThreshold);
  }

  private GoogleCalendarSyncResult syncGoogleEventsWithRecovery(
      String accessToken, ExternalCalendar externalCalendar) {
    try {
      return googleCalendarPort.syncEvent(accessToken, externalCalendar.syncToken());
    } catch (IllegalStateException e) {
      if (!"GOOGLE_SYNC_TOKEN_EXPIRED".equals(e.getMessage())) {
        throw e;
      }

      externalCalendarStore.updateSyncToken(externalCalendar.id(), null);
      return googleCalendarPort.syncEvent(accessToken, null);
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

    String decodedPayload;
    try {
      decodedPayload =
          new String(Base64.getUrlDecoder().decode(splitToken[1]), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE", e);
    }

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

  // watch TTL 설정값이 비정상이면 기본값(7일)으로 보정
  private Long resolveWatchTtlSeconds() {
    if (watchTtlSeconds == null || watchTtlSeconds <= 0) {
      return 604800L;
    }

    return watchTtlSeconds;
  }

  // 문자열이 null 또는 공백인지 확인
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
