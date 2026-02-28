package com.coffiness.calfit.core.api.template.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ApplicationFormTemplateUpdateRequest {

  private String title;

  private String schema;

  private Boolean isDefault;
}
