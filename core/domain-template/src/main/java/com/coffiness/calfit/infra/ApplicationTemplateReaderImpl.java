package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.template.ApplicationTemplate;
import com.coffiness.calfit.domain.template.ApplicationTemplateReader;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationTemplateReaderImpl implements ApplicationTemplateReader {

  private final ApplicationFormTemplateRepository applicationFormTemplateRepository;

  @Override
  public List<ApplicationTemplate> readAll() {
    return applicationFormTemplateRepository
        .findAllByStatusOrderByCreatedAtDesc(EntityStatus.ACTIVE)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public ApplicationTemplate read(Long templateId) {
    ApplicationFormTemplateEntity entity =
        applicationFormTemplateRepository
            .findByIdAndStatus(templateId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    return toDomain(entity);
  }

  private ApplicationTemplate toDomain(ApplicationFormTemplateEntity entity) {
    return new ApplicationTemplate(
        entity.getId(), entity.getName(), entity.getSchema(), entity.getIsDefault());
  }
}
