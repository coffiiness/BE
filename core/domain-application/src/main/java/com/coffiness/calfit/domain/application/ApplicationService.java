package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationService {

  private static final String DEFAULT_TENANT = "default";

  private final ApplicationRepository applicationRepository;

  @Transactional
  public Long create(ApplicationCreateRequest request, Long requesterUserId) {
    requireTenant();
    Long applicantId = request.applicantId() != null ? request.applicantId() : requesterUserId;
    if (requesterUserId == null || !requesterUserId.equals(applicantId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (applicationRepository.existsByApplicantIdAndRecruitmentId(
        applicantId, request.recruitmentId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR, "APPLICATION_ALREADY_EXISTS", null);
    }

    LocalDateTime birthDate = request.birthDate().atStartOfDay();
    ApplicationEntity entity =
        ApplicationEntity.create(
            applicantId,
            request.recruitmentId(),
            request.recruitmentProcessId(),
            request.templateId(),
            request.name(),
            request.gender(),
            birthDate,
            request.phone(),
            request.email(),
            request.schema().toString());

    ApplicationEntity saved = applicationRepository.save(entity);
    return saved.getId();
  }

  private void requireTenant() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || DEFAULT_TENANT.equals(tenantId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR, "TENANT_ID_REQUIRED", null);
    }
  }
}
