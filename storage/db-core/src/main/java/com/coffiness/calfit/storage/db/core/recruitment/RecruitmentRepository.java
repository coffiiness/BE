package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentRepository extends JpaRepository<RecruitmentEntity, Long> {
  Page<RecruitmentEntity> findByRecruitmentStatus(
      RecruitmentStatus recruitmentStatus, Pageable pageable);

  Optional<RecruitmentEntity> findByIdAndStatus(Long id, EntityStatus status);
}
