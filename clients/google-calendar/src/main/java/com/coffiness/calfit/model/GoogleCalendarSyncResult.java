package com.coffiness.calfit.model;

import com.coffiness.calfit.dto.GoogleCalendarSyncResponseDto.Item;
import java.util.List;

public record GoogleCalendarSyncResult(
        List<Item> items,
        String nextSyncToken
) {}
