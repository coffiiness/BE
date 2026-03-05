package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.template.ApplicationTemplate;

public record ApplicationTemplateResponse(Long id, String name, String schema, Boolean isDefault) {

  public static ApplicationTemplateResponse from(ApplicationTemplate template) {
    return new ApplicationTemplateResponse(
        template.id(), template.name(), template.schema(), template.isDefault());
  }
}
