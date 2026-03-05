package com.coffiness.calfit.storage.db.core.recruitment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentStageRepository extends JpaRepository<RecruitmentStageEntity, Long> {
  List<RecruitmentStageEntity> findByRecruitmentIdIn(List<Long> recruitmentIds);

  List<RecruitmentStageEntity> findByRecruitmentId(Long recruitmentId);

  void deleteByRecruitmentId(Long recruitmentId);
}
