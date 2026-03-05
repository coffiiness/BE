package com.coffiness.calfit.domain.recruitment;

import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentStore {

  Recruitment store(Recruitment recruitment);

  Recruitment update(Recruitment recruitment);

  void delete(Long recruitmentId);
}
