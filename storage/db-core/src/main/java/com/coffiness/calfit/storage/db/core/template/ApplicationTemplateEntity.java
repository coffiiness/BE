package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "application_templates")
@NoArgsConstructor
@Getter
public class ApplicationTemplateEntity extends TenantBaseEntity {

  @Column(nullable = false, length = 50)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender;

  @Column(nullable = false)
  private LocalDateTime birthDate;

  @Column(nullable = false, length = 20)
  private String phone;

  @Column(nullable = false, unique = true, length = 50)
  private String email;

  @Column(name = "form_fields", columnDefinition = "JSON", nullable = false)
  private String formFields;

  @Column(name = "is_default")
  private Boolean isDefault = false;

  public static ApplicationTemplateEntity create(
      String name, String formFields, Boolean isDefault) {
    ApplicationTemplateEntity entity = new ApplicationTemplateEntity();
    entity.name = name;
    entity.formFields = formFields;
    entity.isDefault = Boolean.TRUE.equals(isDefault);

    // legacy non-null columns
    entity.gender = Gender.MALE;
    entity.birthDate = LocalDateTime.of(2000, 1, 1, 0, 0);
    entity.phone = "01000000000";
    entity.email = buildUniqueTemplateEmail();
    return entity;
  }

  public void update(String name, String formFields, Boolean isDefault) {
    this.name = name;
    this.formFields = formFields;
    this.isDefault = Boolean.TRUE.equals(isDefault);
  }

  private static String buildUniqueTemplateEmail() {
    String raw = "tpl" + UUID.randomUUID().toString().replace("-", "");
    return raw.substring(0, Math.min(raw.length(), 30)) + "@tpl.local";
  }
}
