package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.template.ApplicationTemplate;
import com.coffiness.calfit.domain.template.ApplicationTemplateStore;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationTemplateStoreImpl implements ApplicationTemplateStore {

  private final ApplicationFormTemplateRepository applicationFormTemplateRepository;

  @Override
  public ApplicationTemplate store(ApplicationTemplate template) {
    ApplicationFormTemplateEntity entity =
        ApplicationFormTemplateEntity.builder()
            .name(template.name())
            .schema(template.schema())
            .isDefault(template.isDefault())
            .build();

    ApplicationFormTemplateEntity saved = applicationFormTemplateRepository.save(entity);

    return toDomain(saved);
  }

  @Override
  public ApplicationTemplate update(ApplicationTemplate template) {
    ApplicationFormTemplateEntity entity =
        applicationFormTemplateRepository
            .findByIdAndStatus(template.id(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    entity.update(template.name(), template.schema(), template.isDefault());

    return toDomain(entity);
  }

  @Override
  public void delete(Long templateId) {
    ApplicationFormTemplateEntity entity =
        applicationFormTemplateRepository
            .findByIdAndStatus(templateId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    entity.deleted();
  }

  private ApplicationTemplate toDomain(ApplicationFormTemplateEntity entity) {
    return new ApplicationTemplate(
        entity.getId(), entity.getName(), entity.getSchema(), entity.getIsDefault());
  }
}
