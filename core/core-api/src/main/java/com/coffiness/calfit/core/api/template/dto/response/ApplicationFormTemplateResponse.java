package com.coffiness.calfit.core.api.template.dto.response;

import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateEntity;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApplicationFormTemplateResponse {

  private Long id;
  private String title;
  private String schema;
  private Boolean isDefault;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static ApplicationFormTemplateResponse from(ApplicationFormTemplateEntity entity) {
    return ApplicationFormTemplateResponse.builder()
        .id(entity.getId())
        .title(entity.getTitle())
        .schema(entity.getSchema())
        .isDefault(entity.getIsDefault())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
