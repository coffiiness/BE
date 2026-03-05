package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationTemplateCreateRequest(
    @NotBlank(message = "Template name is required.")
        @Size(max = 100, message = "Template name must be at most 100 characters.")
        String name,
    @NotBlank(message = "Template schema is required.")
        @Size(max = 20000, message = "Template schema must be at most 20000 characters.")
        String schema,
    Boolean isDefault) {}
