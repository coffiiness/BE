package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationFormTemplateRepository
    extends JpaRepository<ApplicationFormTemplateEntity, Long> {

  Optional<ApplicationFormTemplateEntity> findByIdAndTenantIdAndStatus(
      Long id, String tenantId, EntityStatus status);

  List<ApplicationFormTemplateEntity> findAllByTenantIdAndStatus(
      String tenantId, EntityStatus status);
}
