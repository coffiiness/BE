package com.coffiness.calfit.api.template.stub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원서 제출 응답 DTO (테스트용 스텁)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApplyResponse {
    private Long applicationId;
    private Long applicantId;
    private Long jobId;
    private String status;
    private String submittedAt;
}