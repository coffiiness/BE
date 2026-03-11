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
@Table(name = "applicationTemplates")
@NoArgsConstructor
@Getter
public class ApplicationTemplateEntity extends TenantBaseEntity {

  // Applicant name
  @Column(nullable = false, length = 50)
  private String name;

  // Applicant gender
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender;

  // Applicant birth date
  @Column(nullable = false)
  private LocalDateTime birthDate;

  // Applicant phone number
  @Column(nullable = false, length = 20)
  private String phone;

  // Applicant email
  @Column(nullable = false, unique = true, length = 50)
  private String email;

  // Template form field payload
  @Column(name = "form_fields", columnDefinition = "JSON", nullable = false)
  private String formFields;

  @Column(name = "is_default")
  private Boolean isDefault = false;

  @Builder
  private ApplicationTemplateEntity(
      String name,
      Gender gender,
      LocalDateTime birthDate,
      String phone,
      String email,
      String formFields,
      Boolean isDefault) {
    this.name = name;
    this.gender = gender;
    this.birthDate = birthDate;
    this.phone = phone;
    this.email = email;
    this.formFields = formFields;
    this.isDefault = isDefault;
  }

  public static ApplicationTemplateEntity create(
      String templateName, String formFields, boolean inUse) {
    return ApplicationTemplateEntity.builder()
        .name(templateName)
        .gender(Gender.MALE)
        .birthDate(LocalDateTime.of(2000, 1, 1, 0, 0))
        .phone("000-0000-0000")
        .email("t-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "@cf.io")
        .formFields(formFields)
        .isDefault(inUse)
        .build();
  }

  public void updateTemplate(String templateName, String formFields) {
    this.name = templateName;
    this.formFields = formFields;
  }

  public void updateUseStatus(boolean inUse) {
    this.isDefault = inUse;
  }

  public boolean isInUse() {
    return Boolean.TRUE.equals(this.isDefault);
  }
}
