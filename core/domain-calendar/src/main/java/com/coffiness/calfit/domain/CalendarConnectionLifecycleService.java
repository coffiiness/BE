package com.coffiness.calfit.domain;

import com.coffiness.calfit.domain.event.CalendarWatchStopRequestedEvent;
import com.coffiness.calfit.domain.workspace.member.MemberDepartureHandler;
import com.coffiness.calfit.port.GoogleCalendarPort;
import com.coffiness.calfit.storage.db.core.calendar.GoogleCalendarOAuthStateRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarConnectionLifecycleService implements MemberDepartureHandler {
  private final ExternalCalendarReader reader;
  private final ExternalCalendarStore store;
  private final GoogleCalendarPort googleCalendarPort;
  private final DomainEventPublisher events;
  private final GoogleCalendarOAuthStateRepository states;
  private final Clock clock;

  @Override
  @Transactional
  public void disconnectUser(Long userId) {
    states.invalidatePending(userId, TenantContext.getTenantId(), LocalDateTime.now(clock));
    for (ExternalCalendar calendar : reader.readAllByUserId(userId)) {
      store.disableSync(calendar.id());
      if (calendar.channelId() != null && calendar.channelResourceId() != null) {
        events.publish(
            new CalendarWatchStopRequestedEvent(
                TenantContext.getTenantId(),
                calendar.id(),
                calendar.channelId(),
                calendar.channelResourceId(),
                System.currentTimeMillis()));
      }
    }
  }

  @Transactional
  public void stopDisabledWatch(CalendarWatchStopRequestedEvent event) {
    ExternalCalendar calendar = reader.readForUpdate(event.externalCalendarId());
    if (calendar.isSyncEnabled()
        || !Objects.equals(calendar.channelId(), event.channelId())
        || !Objects.equals(calendar.channelResourceId(), event.resourceId())) return;
    try {
      googleCalendarPort.stopWatch(calendar.accessToken(), event.channelId(), event.resourceId());
      store.clearWatchChannel(calendar.id());
    } catch (RuntimeException e) {
      // Logical disconnection is already committed. Keep channel IDs for a reviewed retry.
      log.warn(
          "Google watch stop pending: externalCalendarId={}, channelId={}",
          calendar.id(),
          event.channelId());
    }
  }
}
