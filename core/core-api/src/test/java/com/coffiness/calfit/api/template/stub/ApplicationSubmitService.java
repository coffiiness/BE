package com.coffiness.calfit.api.template.stub;

import com.coffiness.calfit.api.template.stub.dto.*;

/**
 * 지원서 제출 서비스 인터페이스 (테스트용 스텁)
 */
public interface ApplicationSubmitService {

    /** 지원서 제출 */
    ApplyResponse apply(String companySlug, Long jobId, Long applicantId, ApplyRequest request);
}