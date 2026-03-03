package com.coffiness.calfit.api.template.stub;

import com.coffiness.calfit.api.template.stub.dto.*;

/**
 * 지원서 템플릿 서비스 인터페이스 (테스트용 스텁)
 *
 * <p>
 * 실제 서비스 구현 시 이 인터페이스를 참고하여 core-api 모듈에 구현하세요.
 * 테스트에서는 Mockito로 mock하여 사용합니다.
 */
public interface RecruitmentTemplateService {

    /** 템플릿 목록 조회 */
    TemplatesPageResponse getTemplates(int page, int size, String search);

    /** 템플릿 상세 조회 */
    TemplateDetailResponse getTemplateDetail(Long templateId);

    /** 템플릿 생성 */
    CreateTemplateResponse createTemplate(CreateTemplateRequest request);

    /** 템플릿 수정 */
    UpdateTemplateResponse updateTemplate(Long templateId, UpdateTemplateRequest request);

    /** 템플릿 삭제 */
    void deleteTemplate(Long templateId);
}