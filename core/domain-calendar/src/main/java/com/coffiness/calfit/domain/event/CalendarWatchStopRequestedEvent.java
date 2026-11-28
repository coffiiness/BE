package com.coffiness.calfit.domain.event;

import com.coffiness.calfit.support.event.DomainEvent;

public record CalendarWatchStopRequestedEvent(
    String tenantId, Long externalCalendarId, String channelId, String resourceId, long occurredAt)
    implements DomainEvent {}
