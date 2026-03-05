package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
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

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Lob
  @Column(name = "template_schema", nullable = false)
  private String schema;

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault;

  @Builder
  public ApplicationFormTemplateEntity(String name, String schema, Boolean isDefault) {
    this.name = name;
    this.schema = schema;
    this.isDefault = isDefault;
  }

  public void update(String name, String schema, Boolean isDefault) {
    this.name = name;
    this.schema = schema;
    this.isDefault = isDefault;
  }
}
