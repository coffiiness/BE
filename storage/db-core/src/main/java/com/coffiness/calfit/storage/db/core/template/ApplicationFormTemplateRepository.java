package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationFormTemplateRepository
    extends JpaRepository<ApplicationFormTemplateEntity, Long> {

  List<ApplicationFormTemplateEntity> findAllByStatusOrderByCreatedAtDesc(EntityStatus status);

  Optional<ApplicationFormTemplateEntity> findByIdAndStatus(Long id, EntityStatus status);
}
