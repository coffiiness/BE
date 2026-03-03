package com.coffiness.calfit.api.template.stub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 템플릿 수정 요청 DTO (테스트용 스텁)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemplateRequest {

    @NotBlank(message = "name은 필수입니다")
    @Size(max = 50, message = "name은 50자 이하여야 합니다")
    private String name;

    @NotEmpty(message = "fields는 1개 이상이어야 합니다")
    @Valid
    private List<FieldUpdateRequest> fields;

    private List<Long> deletedFieldIds;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldUpdateRequest {

        private Long fieldId;

        @NotBlank(message = "fieldKey는 필수입니다")
        private String fieldKey;

        @NotBlank(message = "label은 필수입니다")
        private String label;

        @NotBlank(message = "type은 필수입니다")
        private String type;

        private boolean required;

        private List<String> options;

        @Min(value = 1, message = "order는 1 이상이어야 합니다")
        private int order;
    }
}