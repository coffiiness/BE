package com.coffiness.calfit.storage.db.core.template;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationTemplateRepository
    extends JpaRepository<ApplicationTemplateEntity, Long> {

  @Query(
      value =
          "SELECT t.schema FROM applicationTemplates t"
              + " WHERE t.id = :templateId AND t.status = 'ACTIVE'",
      nativeQuery = true)
  Optional<String> findSchemaById(@Param("templateId") Long templateId);
}
