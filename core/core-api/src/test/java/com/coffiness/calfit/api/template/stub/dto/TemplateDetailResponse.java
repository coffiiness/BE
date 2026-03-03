package com.coffiness.calfit.api.template.stub.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 템플릿 상세 조회 응답 DTO (테스트용 스텁)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDetailResponse {
    private Long templateId;
    private String name;
    private String status;
    private String createdAt;
    private String updatedAt;
    private List<FieldDetail> fields;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDetail {
        private Long fieldId;
        private String fieldKey;
        private String label;
        private String type;
        private boolean required;
        private List<String> options;
        private int order;
        private boolean isDefault;
    }
}