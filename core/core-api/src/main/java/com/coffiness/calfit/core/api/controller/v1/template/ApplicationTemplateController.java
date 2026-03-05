package com.coffiness.calfit.core.api.controller.v1.template;

import com.coffiness.calfit.api.v1.request.ApplicationTemplateCreateRequest;
import com.coffiness.calfit.api.v1.request.ApplicationTemplateUpdateRequest;
import com.coffiness.calfit.api.v1.response.ApplicationTemplateResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.template.ApplicationTemplate;
import com.coffiness.calfit.domain.template.ApplicationTemplateService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/application-templates")
public class ApplicationTemplateController {

  private final ApplicationTemplateService applicationTemplateService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ApplicationTemplateResponse> createTemplate(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody ApplicationTemplateCreateRequest request) {

    Long userId = user != null ? user.userId() : null;
    ApplicationTemplate created = applicationTemplateService.createTemplate(userId, request);

    return ApiResponse.success(ApplicationTemplateResponse.from(created));
  }

  @GetMapping
  public ApiResponse<List<ApplicationTemplateResponse>> getTemplates(
      @AuthenticationPrincipal SecurityUser user) {

    Long userId = user != null ? user.userId() : null;
    List<ApplicationTemplateResponse> response =
        applicationTemplateService.getTemplates(userId).stream()
            .map(ApplicationTemplateResponse::from)
            .toList();

    return ApiResponse.success(response);
  }

  @GetMapping("/{templateId}")
  public ApiResponse<ApplicationTemplateResponse> getTemplate(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long templateId) {

    Long userId = user != null ? user.userId() : null;
    ApplicationTemplate template = applicationTemplateService.getTemplate(templateId, userId);

    return ApiResponse.success(ApplicationTemplateResponse.from(template));
  }

  @PutMapping("/{templateId}")
  public ApiResponse<ApplicationTemplateResponse> updateTemplate(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long templateId,
      @Valid @RequestBody ApplicationTemplateUpdateRequest request) {

    Long userId = user != null ? user.userId() : null;
    ApplicationTemplate updated =
        applicationTemplateService.updateTemplate(templateId, userId, request);

    return ApiResponse.success(ApplicationTemplateResponse.from(updated));
  }

  @DeleteMapping("/{templateId}")
  public ApiResponse<?> deleteTemplate(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long templateId) {

    Long userId = user != null ? user.userId() : null;
    applicationTemplateService.deleteTemplate(templateId, userId);

    return ApiResponse.success();
  }
}
