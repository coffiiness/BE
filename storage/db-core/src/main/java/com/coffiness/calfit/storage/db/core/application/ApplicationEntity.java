package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "applications")
@NoArgsConstructor
@Getter
public class ApplicationEntity extends TenantBaseEntity {

  @Column(name = "applicant_id", nullable = false)
  private Long applicantId;

  @Column(name = "recruitment_id", nullable = false)
  private Long recruitmentId;

  @Column(name = "recruitmentProcess_id", nullable = false)
  private Long recruitmentProcessId;

  @Column(name = "template_id", nullable = false)
  private Long templateId;

  @Column(nullable = false, length = 50)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender;

  @Column(nullable = false)
  private LocalDateTime birthDate;

  @Column(nullable = false, length = 20)
  private String phone;

  @Column(nullable = false, length = 50)
  private String email;

  @Column(columnDefinition = "JSON", nullable = false)
  private String schema;
}
