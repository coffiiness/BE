package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "google_calendar_oauth_states")
public class GoogleCalendarOAuthStateEntity extends BaseEntity {
  @Column(name = "state_hash", nullable = false, unique = true, length = 64)
  private String stateHash;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "workspace_id", nullable = false)
  private String workspaceId;

  @Column(name = "redirect_uri", nullable = false, length = 1024)
  private String redirectUri;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  public GoogleCalendarOAuthStateEntity(
      String stateHash,
      Long userId,
      String workspaceId,
      String redirectUri,
      LocalDateTime expiresAt) {
    this.stateHash = stateHash;
    this.userId = userId;
    this.workspaceId = workspaceId;
    this.redirectUri = redirectUri;
    this.expiresAt = expiresAt;
  }

  public void consume(LocalDateTime now) {
    this.usedAt = now;
  }
}
