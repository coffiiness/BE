package com.coffiness.calfit.storage.db.core.application;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {
  boolean existsByApplicantIdAndRecruitmentId(Long applicantId, Long recruitmentId);
}
