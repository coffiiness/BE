package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {
  boolean existsByApplicantIdAndRecruitmentId(Long applicantId, Long recruitmentId);

  Optional<ApplicationEntity> findByIdAndStatus(Long id, EntityStatus status);

  List<ApplicationEntity> findByRecruitmentIdAndStatus(Long recruitmentId, EntityStatus status);

  List<ApplicationEntity> findByRecruitmentIdAndRecruitmentProcessIdAndStatus(
      Long recruitmentId, Long recruitmentProcessId, EntityStatus status);

  List<ApplicationEntity> findByStatus(EntityStatus status);
}
