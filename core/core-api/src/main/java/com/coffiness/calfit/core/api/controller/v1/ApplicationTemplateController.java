package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ApplicationTemplateController {

  private final ApplicationTemplateRepository applicationTemplateRepository;
  private final ObjectMapper objectMapper;

  @GetMapping("/api/v1/application-templates")
  public ApiResponse<List<ApplicationTemplateResponse>> getTemplates(
      @AuthenticationPrincipal SecurityUser user) {
    validateUser(user);

    List<ApplicationTemplateResponse> responses =
        applicationTemplateRepository.findByStatusOrderByIdDesc(EntityStatus.ACTIVE).stream()
            .map(ApplicationTemplateResponse::from)
            .toList();

    return ApiResponse.success(responses);
  }

  @GetMapping("/api/v1/application-templates/{templateId}")
  public ApiResponse<ApplicationTemplateResponse> getTemplate(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long templateId) {
    validateUser(user);

    ApplicationTemplateEntity entity =
        applicationTemplateRepository
            .findByIdAndStatus(templateId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    return ApiResponse.success(ApplicationTemplateResponse.from(entity));
  }

  @PostMapping("/api/v1/application-templates")
  public ApiResponse<ApplicationTemplateResponse> createTemplate(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody ApplicationTemplateRequest request) {
    validateUser(user);

    String name = request.resolveName();
    String schema = request.resolveSchema(objectMapper);

    ApplicationTemplateEntity saved =
        applicationTemplateRepository.save(
            ApplicationTemplateEntity.create(name, schema, request.isDefault()));

    return ApiResponse.success(ApplicationTemplateResponse.from(saved));
  }

  @PutMapping("/api/v1/application-templates/{templateId}")
  public ApiResponse<ApplicationTemplateResponse> updateTemplate(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long templateId,
      @Valid @RequestBody ApplicationTemplateRequest request) {
    validateUser(user);

    ApplicationTemplateEntity entity =
        applicationTemplateRepository
            .findByIdAndStatus(templateId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    entity.update(request.resolveName(), request.resolveSchema(objectMapper), request.isDefault());
    ApplicationTemplateEntity saved = applicationTemplateRepository.save(entity);

    return ApiResponse.success(ApplicationTemplateResponse.from(saved));
  }

  @DeleteMapping("/api/v1/application-templates/{templateId}")
  public ApiResponse<Long> deleteTemplate(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long templateId) {
    validateUser(user);

    ApplicationTemplateEntity entity =
        applicationTemplateRepository
            .findByIdAndStatus(templateId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    entity.deleted();
    applicationTemplateRepository.save(entity);

    return ApiResponse.success(templateId);
  }

  private void validateUser(SecurityUser user) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
  }

  public record ApplicationTemplateRequest(
      String name,
      String title,
      String schema,
      List<Map<String, Object>> customFields,
      Boolean isDefault) {

    public String resolveName() {
      String resolved = title != null && !title.isBlank() ? title : name;
      if (resolved == null || resolved.isBlank()) {
        throw new CoreException(ErrorType.VALIDATION_ERROR);
      }
      return resolved;
    }

    public String resolveSchema(ObjectMapper objectMapper) {
      if (schema != null && !schema.isBlank()) {
        return schema;
      }
      try {
        List<Map<String, Object>> source = customFields != null ? customFields : List.of();
        return objectMapper.writeValueAsString(source);
      } catch (JsonProcessingException e) {
        throw new CoreException(ErrorType.VALIDATION_ERROR);
      }
    }
  }

  public record ApplicationTemplateResponse(
      Long id,
      @NotBlank String name,
      String title,
      String schema,
      Boolean isDefault,
      String status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    static ApplicationTemplateResponse from(ApplicationTemplateEntity entity) {
      return new ApplicationTemplateResponse(
          entity.getId(),
          entity.getName(),
          entity.getName(),
          entity.getSchema(),
          Boolean.TRUE.equals(entity.getIsDefault()),
          entity.isActive() ? "ACTIVE" : "DELETED",
          entity.getCreatedAt(),
          entity.getUpdatedAt());
    }
  }
}
