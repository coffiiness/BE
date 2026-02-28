package com.coffiness.calfit.core.api.template.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ApplicationFormTemplateCreateRequest {

  @NotBlank(message = "템플릿 제목을 입력해주세요.")
  private String title;

  @NotBlank(message = "스키마를 입력해주세요.")
  private String schema;

  @NotNull(message = "기본 템플릿 여부를 설정해주세요.")
  private Boolean isDefault;
}
