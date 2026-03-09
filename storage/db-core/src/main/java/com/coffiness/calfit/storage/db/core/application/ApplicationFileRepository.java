package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.UploadStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationFileRepository extends JpaRepository<ApplicationFileEntity, Long> {
  Optional<ApplicationFileEntity> findByApplicationIdAndFieldKey(
      Long applicationId, String fieldKey);

  List<ApplicationFileEntity> findByApplicationIdAndStatusAndUploadStatus(
      Long applicationId, EntityStatus status, UploadStatus uploadStatus);
}
