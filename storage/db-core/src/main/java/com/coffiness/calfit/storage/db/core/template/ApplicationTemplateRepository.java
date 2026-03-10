package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationTemplateRepository
    extends JpaRepository<ApplicationTemplateEntity, Long> {

  List<ApplicationTemplateEntity> findByStatusOrderByIdDesc(EntityStatus status);

  Optional<ApplicationTemplateEntity> findByIdAndStatus(Long id, EntityStatus status);
}
