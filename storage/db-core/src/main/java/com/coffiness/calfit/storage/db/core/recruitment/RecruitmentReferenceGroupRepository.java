package com.coffiness.calfit.storage.db.core.recruitment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentReferenceGroupRepository
    extends JpaRepository<RecruitmentReferenceGroupEntity, Long> {
  List<RecruitmentReferenceGroupEntity> findByRecruitmentId(Long recruitmentId);

  void deleteByRecruitmentId(Long recruitmentId);
}
