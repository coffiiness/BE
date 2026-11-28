package com.coffiness.calfit.core.api.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.coffiness.calfit.domain.GoogleCalendarOAuthStateService;
import com.coffiness.calfit.storage.db.core.calendar.GoogleCalendarOAuthStateEntity;
import com.coffiness.calfit.storage.db.core.calendar.GoogleCalendarOAuthStateRepository;
import com.coffiness.calfit.storage.db.core.member.ActiveMembershipPolicy;
import com.coffiness.calfit.support.error.CoreException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GoogleCalendarOAuthStateTest {
  private final GoogleCalendarOAuthStateRepository repository =
      mock(GoogleCalendarOAuthStateRepository.class);
  private final ActiveMembershipPolicy membership = mock(ActiveMembershipPolicy.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-11-28T00:00:00Z"), ZoneOffset.UTC);
  private final Map<String, GoogleCalendarOAuthStateEntity> stored = new HashMap<>();
  private static final String REDIRECT = "https://frontend.example.invalid/auth/callback";
  private GoogleCalendarOAuthStateService service;

  @BeforeEach
  void prepareOAuthStateService() {
    service =
        new GoogleCalendarOAuthStateService(
            repository, membership, clock, "synthetic-client", REDIRECT);
    when(membership.requireCurrentTenant(3L)).thenReturn("A");
    when(repository.save(any()))
        .thenAnswer(
            call -> {
              GoogleCalendarOAuthStateEntity entity = call.getArgument(0);
              stored.put(entity.getStateHash(), entity);
              return entity;
            });
    when(repository.findForConsumption(anyString()))
        .thenAnswer(call -> Optional.ofNullable(stored.get(call.getArgument(0))));
  }

  @Test
  void stateIsRandomHashedBoundAndConsumedOnce() {
    var first = service.issue(3L, REDIRECT);
    var second = service.issue(3L, REDIRECT);
    assertThat(first.state()).hasSize(64).isNotEqualTo(second.state());
    assertThat(stored).doesNotContainKey(first.state());
    assertThat(first.authorizationUrl()).contains("state=" + first.state(), "synthetic-client");
    assertThat(first.toString()).doesNotContain(first.state());
    assertThat(service.consume(3L, first.state(), REDIRECT)).isEqualTo("A");
    assertThatThrownBy(() -> service.consume(3L, first.state(), REDIRECT))
        .isInstanceOf(CoreException.class);
  }

  @Test
  void differentUserWorkspaceOrRedirectCannotConsumeState() {
    var state = service.issue(3L, REDIRECT).state();
    assertThatThrownBy(() -> service.consume(4L, state, REDIRECT))
        .isInstanceOf(CoreException.class);
    when(membership.requireCurrentTenant(3L)).thenReturn("B");
    assertThatThrownBy(() -> service.consume(3L, state, REDIRECT))
        .isInstanceOf(CoreException.class);
    assertThatThrownBy(() -> service.consume(3L, state, "https://attacker.invalid/auth/callback"))
        .isInstanceOf(CoreException.class);
  }

  @Test
  void deletedStateCannotBeConsumed() {
    var state = service.issue(3L, REDIRECT).state();
    stored.values().forEach(GoogleCalendarOAuthStateEntity::deleted);
    assertThatThrownBy(() -> service.consume(3L, state, REDIRECT))
        .isInstanceOf(CoreException.class);
  }

  @Test
  void expiredMissingMalformedAndUnknownStateAreRejected() {
    var state = service.issue(3L, REDIRECT).state();
    stored
        .values()
        .forEach(
            entity ->
                ReflectionTestUtils.setField(
                    entity, "expiresAt", LocalDateTime.now(clock).minusSeconds(1)));
    assertThatThrownBy(() -> service.consume(3L, state, REDIRECT))
        .isInstanceOf(CoreException.class);
    assertThatThrownBy(() -> service.consume(3L, null, REDIRECT)).isInstanceOf(CoreException.class);
    assertThatThrownBy(() -> service.consume(3L, "bad-state", REDIRECT))
        .isInstanceOf(CoreException.class);
    assertThatThrownBy(() -> service.consume(3L, "0".repeat(64), REDIRECT))
        .isInstanceOf(CoreException.class);
    assertThatThrownBy(() -> service.issue(3L, null)).isInstanceOf(CoreException.class);
  }
}
