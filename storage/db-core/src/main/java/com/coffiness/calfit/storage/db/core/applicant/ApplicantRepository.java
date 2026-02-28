package com.coffiness.calfit.storage.db.core.applicant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantRepository extends JpaRepository<ApplicantEntity, Long> {

  Optional<ApplicantEntity> findByEmail(String email);

  boolean existsByEmail(String email);
}
