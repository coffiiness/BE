package com.coffiness.calfit.api.template.stub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 템플릿 생성 응답 DTO (테스트용 스텁)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateResponse {
    private Long templateId;
    private String name;
    private String status;
    private String createdAt;
    private String updatedAt;
}