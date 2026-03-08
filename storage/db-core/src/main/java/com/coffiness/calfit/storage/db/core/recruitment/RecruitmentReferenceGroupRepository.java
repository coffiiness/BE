package com.coffiness.calfit.storage.db.core.recruitment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecruitmentReferenceGroupRepository
    extends JpaRepository<RecruitmentReferenceGroupEntity, Long> {
  List<RecruitmentReferenceGroupEntity> findByRecruitmentId(Long recruitmentId);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM RecruitmentReferenceGroupEntity r WHERE r.recruitmentId = :recruitmentId")
  void deleteAllByRecruitmentId(Long recruitmentId);
}
