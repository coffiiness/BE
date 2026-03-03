package com.coffiness.calfit.api.template.stub.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 템플릿 목록 조회 응답 DTO (테스트용 스텁)
 * 실제 구현 시 core-api 모듈의 DTO로 교체하세요.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TemplatesPageResponse {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<TemplateItem> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateItem {
        private Long templateId;
        private String name;
        private String createdAt;
        private String updatedAt;
        private String status;
    }
}
