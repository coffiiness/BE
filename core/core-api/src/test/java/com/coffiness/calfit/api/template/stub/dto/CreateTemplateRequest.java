package com.coffiness.calfit.api.template.stub.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 템플릿 생성 요청 DTO (테스트용 스텁)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateRequest {
    private String name;
    private List<FieldRequest> fields;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldRequest {
        private String fieldKey;
        private String label;
        private String type;
        private boolean required;
        private List<String> options;
    }
}