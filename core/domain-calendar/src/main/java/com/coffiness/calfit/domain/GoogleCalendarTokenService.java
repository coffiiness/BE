package com.coffiness.calfit.domain;

import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.model.OAuthRefreshResult;
import com.coffiness.calfit.port.GoogleOAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/*
 * 구글 캘린더 토큰의 만료 판단과 갱신을 담당
 * */
@Service
@RequiredArgsConstructor
@Transactional
public class GoogleCalendarTokenService {

  private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 120L;

  private final ExternalCalendarReader externalCalendarReader;
  private final ExternalCalendarStore externalCalendarStore;
  private final GoogleOAuthPort googleOAuthPort;
  private final Clock clock;

  // 구글 연결 직후 받은 토큰을 신규 저장하거나 기존 연결 토큰을 갱신
  public void upsertConnectedToken(Long userId, String calendarId, OAuthExchangeResult result) {
    validateExchangeResult(result);

    LocalDateTime expiresAt = calculateExpiresAt(result.expiresIn());

    ExternalCalendar existing = externalCalendarReader.readByUserIdAndCalendarId(userId, calendarId);
    if (existing != null) {
      String refreshToken = resolveRefreshToken(result.refreshToken(), existing.refreshToken());
      externalCalendarStore.updateAuthTokens(existing.id(), result.accessToken(), refreshToken, expiresAt);
      return;
    }

    if (isBlank(result.refreshToken())) {
      throw new IllegalStateException("GOOGLE_REAUTH_REQUIRED");
    }

    ExternalCalendar toCreate =
        new ExternalCalendar(
            null,
            userId,
            calendarId,
            result.accessToken(),
            result.refreshToken(),
            expiresAt,
            null,
            true);

    externalCalendarStore.store(toCreate);
  }

  // 현재 사용 가능한 access token 을 반환하고, 만료 임박 시 refresh 후 갱신
  public String getValidAccessToken(Long externalCalendarId) {
    ExternalCalendar firstRead = externalCalendarReader.read(externalCalendarId);

    if (!isExpiringSoon(firstRead.tokenExpiresAt())) {
      return firstRead.accessToken();
    }

    // 동시 요청에서 이미 refresh 가 끝났는지 확인하기 위해 직전에 한 번 더 조회
    ExternalCalendar latestRead = externalCalendarReader.read(externalCalendarId);
    if (!isExpiringSoon(latestRead.tokenExpiresAt())) {
      return latestRead.accessToken();
    }

    if (isBlank(latestRead.refreshToken())) {
      throw new IllegalStateException("GOOGLE_REAUTH_REQUIRED");
    }

    OAuthRefreshResult refreshed;
    try {
      refreshed = googleOAuthPort.refreshAccessToken(latestRead.refreshToken());
    } catch (IllegalStateException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new IllegalStateException("GOOGLE_OAUTH_ERROR", e);
    }

    validateRefreshResult(refreshed);

    LocalDateTime expiresAt = calculateExpiresAt(refreshed.expiresIn());
    String nextRefreshToken = resolveRefreshToken(refreshed.refreshToken(), latestRead.refreshToken());

    ExternalCalendar updated =
        externalCalendarStore.updateAuthTokens(
            latestRead.id(), refreshed.accessToken(), nextRefreshToken, expiresAt);

    return updated.accessToken();
  }

  // OAuth expiresIn 값을 기준으로 토큰 만료 시각을 계산
  private LocalDateTime calculateExpiresAt(Long expiresIn) {
    if (expiresIn == null || expiresIn <= 0) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }

    return LocalDateTime.now(clock).plusSeconds(expiresIn);
  }

  // 토큰 만료 시각이 없거나, 현재 시각 기준 버퍼 이내로 만료 예정이면 true 를 반환
  private boolean isExpiringSoon(LocalDateTime tokenExpiresAt) {
    if (tokenExpiresAt == null) {
      return true;
    }

    LocalDateTime now = LocalDateTime.now(clock);
    return !tokenExpiresAt.isAfter(now.plusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));
  }

  // refresh token을 새 값이 오면 교체하고, 없으면 기존 값을 유지
  private String resolveRefreshToken(String newRefreshToken, String existingRefreshToken) {
    return !isBlank(newRefreshToken) ? newRefreshToken : existingRefreshToken;
  }

  // authorization code 교환 응답의 유효성을 검증
  private void validateExchangeResult(OAuthExchangeResult result) {
    if (result == null || isBlank(result.accessToken())) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }
  }

  // refresh 응답의 유효성을 검증
  private void validateRefreshResult(OAuthRefreshResult result) {
    if (result == null || isBlank(result.accessToken())) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }
    if (result.expiresIn() == null || result.expiresIn() <= 0) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }
  }

  // 문자열이 null 이거나 공백인지 확인
  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
