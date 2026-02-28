package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "application_form_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ApplicationFormTemplateEntity extends TenantBaseEntity {

  @Column(nullable = false, length = 100)
  private String title;

  @Column(columnDefinition = "JSON", nullable = false)
  private String schema;

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  @Builder
  public ApplicationFormTemplateEntity(String title, String schema, Boolean isDefault) {
    this.title = title;
    this.schema = schema;
    this.isDefault = isDefault != null ? isDefault : false;
  }

  public void update(String title, String schema, Boolean isDefault) {
    if (title != null) this.title = title;
    if (schema != null) this.schema = schema;
    if (isDefault != null) this.isDefault = isDefault;
  }
}
