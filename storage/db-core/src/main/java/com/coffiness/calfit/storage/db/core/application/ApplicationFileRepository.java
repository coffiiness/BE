package com.coffiness.calfit.storage.db.core.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationFileRepository extends JpaRepository<ApplicationFileEntity, Long> {
    Optional<ApplicationFileEntity> findByApplicationIdAndFieldKey(Long applicationId, String fieldKey);
}
