package com.coffiness.calfit.api.template.stub.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원서 제출 요청 DTO (테스트용 스텁)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApplyRequest {
    private List<AnswerRequest> answers;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerRequest {
        private String fieldKey;
        private Object value;
        private String fileId;
    }
}