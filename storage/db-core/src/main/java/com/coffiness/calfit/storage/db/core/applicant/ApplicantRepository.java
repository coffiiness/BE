package com.coffiness.calfit.storage.db.core.applicant;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ApplicantRepository extends JpaRepository<ApplicantEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<ApplicantEntity> findAllByTenantIdAndIdIn(String tenantId, List<Long> ids);

  List<ApplicantEntity> findAllByIdIn(List<Long> ids);
}
