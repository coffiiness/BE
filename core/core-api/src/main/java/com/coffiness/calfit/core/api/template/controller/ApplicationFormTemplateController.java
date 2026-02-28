package com.coffiness.calfit.core.api.template.controller;

import com.coffiness.calfit.core.api.template.dto.request.ApplicationFormTemplateCreateRequest;
import com.coffiness.calfit.core.api.template.dto.request.ApplicationFormTemplateUpdateRequest;
import com.coffiness.calfit.core.api.template.dto.response.ApplicationFormTemplateResponse;
import com.coffiness.calfit.core.api.template.service.ApplicationFormTemplateService;
import com.coffiness.calfit.core.support.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/application-templates")
@RequiredArgsConstructor
public class ApplicationFormTemplateController {

  private final ApplicationFormTemplateService service;

  @PostMapping
  public ApiResponse<ApplicationFormTemplateResponse> createTemplate(
      @Valid @RequestBody ApplicationFormTemplateCreateRequest request) {
    return ApiResponse.success(service.createTemplate(request));
  }

  @GetMapping
  public ApiResponse<List<ApplicationFormTemplateResponse>> getTemplates() {
    return ApiResponse.success(service.getTemplates());
  }

  @GetMapping("/{templateId}")
  public ApiResponse<ApplicationFormTemplateResponse> getTemplate(@PathVariable Long templateId) {
    return ApiResponse.success(service.getTemplate(templateId));
  }

  @PutMapping("/{templateId}")
  public ApiResponse<ApplicationFormTemplateResponse> updateTemplate(
      @PathVariable Long templateId,
      @Valid @RequestBody ApplicationFormTemplateUpdateRequest request) {
    return ApiResponse.success(service.updateTemplate(templateId, request));
  }

  @DeleteMapping("/{templateId}")
  public ApiResponse<Void> deleteTemplate(@PathVariable Long templateId) {
    service.deleteTemplate(templateId);
    return ApiResponse.success(null);
  }
}
