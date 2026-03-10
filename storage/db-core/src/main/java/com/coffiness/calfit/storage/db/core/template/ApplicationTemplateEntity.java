package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "applicationTemplates") // ?쒗뵆由?
@NoArgsConstructor
@Getter
public class ApplicationTemplateEntity extends TenantBaseEntity {

  // 吏?먯옄 ?대쫫
  @Column(nullable = false, length = 50)
  private String name;

  // 吏?먯옄 ?깅퀎
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender;

  // 吏?먯옄 ?앸뀈?붿씪
  @Column(nullable = false)
  private LocalDateTime birthDate;

  // 吏?먯옄 ?꾪솕踰덊샇
  @Column(nullable = false, length = 20)
  private String phone;

  // 吏?먯옄 ?대찓??
  @Column(nullable = false, unique = true, length = 50)
  private String email;

  // 吏?먯옄 ?곸꽭?댁슜
  @Column(name = "`schema`", columnDefinition = "JSON", nullable = false)
  private String schema;

  @Column(name = "is_default")
  private Boolean isDefault = false;

  @Builder
  private ApplicationTemplateEntity(
      String name,
      Gender gender,
      LocalDateTime birthDate,
      String phone,
      String email,
      String schema,
      Boolean isDefault) {
    this.name = name;
    this.gender = gender;
    this.birthDate = birthDate;
    this.phone = phone;
    this.email = email;
    this.schema = schema;
    this.isDefault = isDefault;
  }

  public static ApplicationTemplateEntity create(
      String templateName, String schema, boolean inUse) {
    return ApplicationTemplateEntity.builder()
        .name(templateName)
        .gender(Gender.MALE)
        .birthDate(LocalDateTime.of(2000, 1, 1, 0, 0))
        .phone("000-0000-0000")
        .email("t-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "@cf.io")
        .schema(schema)
        .isDefault(inUse)
        .build();
  }

  public void updateTemplate(String templateName, String schema) {
    this.name = templateName;
    this.schema = schema;
  }

  public void updateUseStatus(boolean inUse) {
    this.isDefault = inUse;
  }

  public boolean isInUse() {
    return Boolean.TRUE.equals(this.isDefault);
  }
}
