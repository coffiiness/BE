package com.coffiness.calfit.domain.template;

import com.coffiness.calfit.api.v1.request.ApplicationTemplateCreateRequest;
import com.coffiness.calfit.api.v1.request.ApplicationTemplateUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationTemplateService {

  private final ApplicationTemplateReader applicationTemplateReader;
  private final ApplicationTemplateStore applicationTemplateStore;
  private final TemplateSchemaValidator templateSchemaValidator;

  public ApplicationTemplate createTemplate(Long userId, ApplicationTemplateCreateRequest request) {
    templateSchemaValidator.validate(request.schema());

    ApplicationTemplate template =
        new ApplicationTemplate(
            null,
            request.name(),
            request.schema(),
            request.isDefault() != null ? request.isDefault() : Boolean.FALSE);

    return applicationTemplateStore.store(template);
  }

  public ApplicationTemplate updateTemplate(
      Long templateId, Long userId, ApplicationTemplateUpdateRequest request) {
    templateSchemaValidator.validate(request.schema());

    ApplicationTemplate current = applicationTemplateReader.read(templateId);

    ApplicationTemplate template =
        new ApplicationTemplate(
            current.id(),
            request.name(),
            request.schema(),
            request.isDefault() != null ? request.isDefault() : Boolean.FALSE);

    return applicationTemplateStore.update(template);
  }

  @Transactional(readOnly = true)
  public List<ApplicationTemplate> getTemplates(Long userId) {
    return applicationTemplateReader.readAll();
  }

  @Transactional(readOnly = true)
  public ApplicationTemplate getTemplate(Long templateId, Long userId) {
    return applicationTemplateReader.read(templateId);
  }

  public void deleteTemplate(Long templateId, Long userId) {
    applicationTemplateStore.delete(templateId);
  }
}
