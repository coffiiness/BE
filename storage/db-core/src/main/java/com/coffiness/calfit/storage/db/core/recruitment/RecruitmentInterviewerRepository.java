package com.coffiness.calfit.storage.db.core.recruitment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentInterviewerRepository
    extends JpaRepository<RecruitmentInterviewerEntity, Long> {
  List<RecruitmentInterviewerEntity> findByRecruitmentIdIn(List<Long> recruitmentIds);

  List<RecruitmentInterviewerEntity> findByRecruitmentId(Long recruitmentId);

  void deleteByRecruitmentId(Long recruitmentId);
}
