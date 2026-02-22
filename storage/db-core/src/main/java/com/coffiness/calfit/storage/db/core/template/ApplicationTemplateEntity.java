package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "applicationTemplates") // 템플릿
@NoArgsConstructor
@Getter
public class ApplicationTemplateEntity extends TenantBaseEntity {

  // 지원자 이름
  @Column(nullable = false, length = 50)
  private String name;

  // 지원자 성별
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender;

  // 지원자 생년월일
  @Column(nullable = false)
  private LocalDateTime birthDate;

  // 지원자 전화번호
  @Column(nullable = false, length = 20)
  private String phone;

  // 지원자 이메일
  @Column(nullable = false, unique = true, length = 50)
  private String email;

  // 지원자 상세내용
  @Column(name = "schema", columnDefinition = "JSON", nullable = false)
  private String schema;

  @Column(name = "is_default")
  private Boolean isDefault = false;
}
