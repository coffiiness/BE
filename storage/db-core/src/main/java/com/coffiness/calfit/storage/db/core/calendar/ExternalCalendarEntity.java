package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 구글 계정 연동 엔티티
 * */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        name = "external_calendars",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uq_external_calendars_user_calendar",
                columnNames = {"user_id", "calendar_id"}
            )
        }
)
public class ExternalCalendarEntity extends TenantBaseEntity {

  // 연관 사용자의 식별자
  @Column(name = "user_id", nullable = false)
  private Long userId;

  // 구글 캘린더 ID (보통 구글 이메일 주소)
  @Column(name = "calendar_id", nullable = false)
  private String calendarId;

  // OAuth2 인증 정보
  // TODO: 추후 암호화 적용 예정
  @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
  private String accessToken;

  @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
  private String refreshToken;

  @Column(name = "token_expires_at", nullable = false)
  private LocalDateTime tokenExpiresAt;

  // 변경된 일정만 가져오는 동기화한 지점의 토큰
  @Column(name = "sync_token", columnDefinition = "TEXT")
  private String syncToken;

  @Column(name = "is_sync_enabled", nullable = false)
  private boolean isSyncEnabled;

  @Builder
  public ExternalCalendarEntity(
      String tenantId,
      Long userId,
      String calendarId,
      String accessToken,
      String refreshToken,
      LocalDateTime tokenExpiresAt,
      String syncToken,
      boolean isSyncEnabled) {
    super(tenantId);
    this.userId = userId;
    this.calendarId = calendarId;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.tokenExpiresAt = tokenExpiresAt;
    this.syncToken = syncToken;
    this.isSyncEnabled = isSyncEnabled;
  }
}
