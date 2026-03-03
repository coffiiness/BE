package com.coffiness.calfit.api.template.stub;

import com.coffiness.calfit.api.template.stub.dto.*;
import com.coffiness.calfit.core.support.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 지원서 템플릿 컨트롤러 (테스트용 스텁)
 *
 * <p>실제 구현 시 이 컨트롤러를 참고하여 core-api 모듈에 구현하세요.
 */
@RestController
@RequestMapping("/api/v1/recruitment/templates")
@RequiredArgsConstructor
public class RecruitmentTemplateController {

    private final RecruitmentTemplateService service;

    @GetMapping
    public ApiResponse<TemplatesPageResponse> getTemplates(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "") @Size(max = 50) String search) {
        return ApiResponse.success(service.getTemplates(page, size, search));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateDetailResponse> getTemplateDetail(
            @PathVariable @Min(1) Long templateId) {
        return ApiResponse.success(service.getTemplateDetail(templateId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request) {
        CreateTemplateResponse response = service.createTemplate(request);
        return ResponseEntity
                .created(URI.create("/api/v1/recruitment/templates/" + response.getTemplateId()))
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<UpdateTemplateResponse> updateTemplate(
            @PathVariable @Min(1) Long templateId,
            @Valid @RequestBody UpdateTemplateRequest request) {
        return ApiResponse.success(service.updateTemplate(templateId, request));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable @Min(1) Long templateId) {
        service.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
}