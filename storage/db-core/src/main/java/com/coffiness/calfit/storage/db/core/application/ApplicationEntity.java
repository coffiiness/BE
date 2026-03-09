package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_application_recruitment_applicant",
          columnNames = {"tenant_id", "recruitment_id", "applicant_id"})
    })
@NoArgsConstructor
@Getter
public class ApplicationEntity extends TenantBaseEntity {

  // 지원자 ID
  @Column(name = "applicant_id", nullable = false)
  private Long applicantId;

  // 채용공고 ID
  @Column(name = "recruitment_id", nullable = false)
  private Long recruitmentId;

  // 채용절차 ID
  @Column(name = "recruitmentProcess_id", nullable = false)
  private Long recruitmentProcessId;

  // 지원서템플릿 ID
  @Column(name = "template_id", nullable = false)
  private Long templateId;

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
  @Column(nullable = false, length = 50)
  private String email;

  // 지원서 상세내용
  @Column(name = "`schema`", columnDefinition = "JSON", nullable = false)
  private String schema;


  @Builder
  private ApplicationEntity(
      Long applicantId,
      Long recruitmentId,
      Long recruitmentProcessId,
      Long templateId,
      String name,
      Gender gender,
      LocalDateTime birthDate,
      String phone,
      String email,
      String schema) {
    this.applicantId = applicantId;
    this.recruitmentId = recruitmentId;
    this.recruitmentProcessId = recruitmentProcessId;
    this.templateId = templateId;
    this.name = name;
    this.gender = gender;
    this.birthDate = birthDate;
    this.phone = phone;
    this.email = email;
    this.schema = schema;
  }

  public static ApplicationEntity create(
      Long applicantId,
      Long recruitmentId,
      Long recruitmentProcessId,
      Long templateId,
      String name,
      Gender gender,
      LocalDateTime birthDate,
      String phone,
      String email,
      String schema) {
    return new ApplicationEntity(
        applicantId,
        recruitmentId,
        recruitmentProcessId,
        templateId,
        name,
        gender,
        birthDate,
        phone,
        email,
        schema);
  }

  public void updateRecruitmentProcessId(Long recruitmentProcessId) {
    this.recruitmentProcessId = recruitmentProcessId;
  }
}
