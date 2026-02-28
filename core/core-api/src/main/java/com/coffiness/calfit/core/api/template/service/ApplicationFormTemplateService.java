package com.coffiness.calfit.core.api.template.service;

import com.coffiness.calfit.core.api.template.dto.request.ApplicationFormTemplateCreateRequest;
import com.coffiness.calfit.core.api.template.dto.request.ApplicationFormTemplateUpdateRequest;
import com.coffiness.calfit.core.api.template.dto.response.ApplicationFormTemplateResponse;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationFormTemplateService {

  private final ApplicationFormTemplateRepository repository;

  @Transactional
  public ApplicationFormTemplateResponse createTemplate(
      ApplicationFormTemplateCreateRequest request) {
    ApplicationFormTemplateEntity entity =
        ApplicationFormTemplateEntity.builder()
            .title(request.getTitle())
            .schema(request.getSchema())
            .isDefault(request.getIsDefault())
            .build();

    ApplicationFormTemplateEntity saved = repository.save(entity);
    return ApplicationFormTemplateResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<ApplicationFormTemplateResponse> getTemplates() {
    String tenantId = TenantContext.getTenantId();
    List<ApplicationFormTemplateEntity> entities =
        repository.findAllByTenantIdAndStatus(tenantId, EntityStatus.ACTIVE);
    return entities.stream()
        .map(ApplicationFormTemplateResponse::from)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ApplicationFormTemplateResponse getTemplate(Long templateId) {
    String tenantId = TenantContext.getTenantId();
    ApplicationFormTemplateEntity entity =
        repository
            .findByIdAndTenantIdAndStatus(templateId, tenantId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    return ApplicationFormTemplateResponse.from(entity);
  }

  @Transactional
  public ApplicationFormTemplateResponse updateTemplate(
      Long templateId, ApplicationFormTemplateUpdateRequest request) {
    String tenantId = TenantContext.getTenantId();
    ApplicationFormTemplateEntity entity =
        repository
            .findByIdAndTenantIdAndStatus(templateId, tenantId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));

    entity.update(request.getTitle(), request.getSchema(), request.getIsDefault());
    return ApplicationFormTemplateResponse.from(entity);
  }

  @Transactional
  public void deleteTemplate(Long templateId) {
    String tenantId = TenantContext.getTenantId();
    ApplicationFormTemplateEntity entity =
        repository
            .findByIdAndTenantIdAndStatus(templateId, tenantId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));

    // Soft delete
    entity.deleted();
  }
}
