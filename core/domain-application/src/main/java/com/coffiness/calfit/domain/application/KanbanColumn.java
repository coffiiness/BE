package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.api.v1.response.ApplicationSummaryResponse;
import java.util.List;

public record KanbanColumn(
    Long recruitmentProcessId, String name, int order, List<ApplicationSummaryResponse> applications) {}
