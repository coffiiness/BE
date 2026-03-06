package com.coffiness.calfit.storage.db.core.applicant;

import com.coffiness.calfit.core.enums.UserRole;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "applicants")
@NoArgsConstructor
@Getter
public class ApplicantEntity extends TenantBaseEntity {

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;
}
