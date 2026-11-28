package com.coffiness.calfit.domain;

import com.coffiness.calfit.storage.db.core.calendar.GoogleCalendarOAuthStateEntity;
import com.coffiness.calfit.storage.db.core.calendar.GoogleCalendarOAuthStateRepository;
import com.coffiness.calfit.storage.db.core.member.ActiveMembershipPolicy;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleCalendarOAuthStateService {
  private final GoogleCalendarOAuthStateRepository repository;
  private final ActiveMembershipPolicy membershipPolicy;
  private final Clock clock;
  private final String clientId;
  private final Set<String> allowedRedirectUris;
  private final SecureRandom random = new SecureRandom();

  public GoogleCalendarOAuthStateService(
      GoogleCalendarOAuthStateRepository repository,
      ActiveMembershipPolicy membershipPolicy,
      Clock clock,
      @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
      @Value("${calendar.google-sync.allowed-redirect-uris}") String allowedRedirectUris) {
    this.repository = repository;
    this.membershipPolicy = membershipPolicy;
    this.clock = clock;
    this.clientId = clientId;
    this.allowedRedirectUris =
        Arrays.stream(allowedRedirectUris.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
  }

  @Transactional
  public Authorization issue(Long userId, String redirectUri) {
    membershipPolicy.lockUser(userId);
    String workspaceId = membershipPolicy.requireCurrentTenant(userId);
    requireAllowedRedirect(redirectUri);
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String state = HexFormat.of().formatHex(bytes);
    repository.save(
        new GoogleCalendarOAuthStateEntity(
            hash(state),
            userId,
            workspaceId,
            redirectUri,
            LocalDateTime.now(clock).plusMinutes(5)));
    String url =
        "https://accounts.google.com/o/oauth2/v2/auth?client_id="
            + encode(clientId)
            + "&redirect_uri="
            + encode(redirectUri)
            + "&response_type=code&scope="
            + encode("https://www.googleapis.com/auth/calendar openid email profile")
            + "&access_type=offline&prompt=consent&state="
            + state;
    return new Authorization(url, state, 300);
  }

  // Commit consumption before exchanging the code: an upstream failure cannot revive state.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String consume(Long userId, String state, String redirectUri) {
    requireAllowedRedirect(redirectUri);
    if (state == null || !state.matches("[a-f0-9]{64}")) throw invalidState();
    // Use the same lock order as membership departure and state invalidation.
    membershipPolicy.lockUser(userId);
    var entity = repository.findForConsumption(hash(state)).orElseThrow(this::invalidState);
    LocalDateTime now = LocalDateTime.now(clock);
    if (!entity.isActive()
        || !entity.getUserId().equals(userId)
        || entity.getUsedAt() != null
        || !entity.getExpiresAt().isAfter(now)
        || !entity.getRedirectUri().equals(redirectUri)
        || !entity.getWorkspaceId().equals(membershipPolicy.requireCurrentTenant(userId))) {
      throw invalidState();
    }
    entity.consume(now);
    return entity.getWorkspaceId();
  }

  private void requireAllowedRedirect(String redirectUri) {
    if (redirectUri == null || !allowedRedirectUris.contains(redirectUri)) {
      throw new CoreException(
          ErrorType.FORBIDDEN, "GOOGLE_REDIRECT_URI_NOT_ALLOWED", "허용되지 않은 OAuth 콜백 주소입니다.");
    }
  }

  private CoreException invalidState() {
    return new CoreException(
        ErrorType.CONFLICT,
        "GOOGLE_OAUTH_STATE_INVALID",
        "인증 요청이 만료되었거나 계정/워크스페이스가 변경되었습니다. 연동을 다시 시작해 주세요.");
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  public record Authorization(String authorizationUrl, String state, long expiresInSeconds) {
    @Override
    public String toString() {
      return "Authorization[expiresInSeconds=" + expiresInSeconds + "]";
    }
  }
}
