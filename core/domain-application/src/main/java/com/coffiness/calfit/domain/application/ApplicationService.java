package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.api.v1.request.ApplicationStageUpdateRequest;
import com.coffiness.calfit.api.v1.response.ApplicationDetailResponse;
import com.coffiness.calfit.api.v1.response.ApplicationFileResponse;
import com.coffiness.calfit.api.v1.response.ApplicationSummaryResponse;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.core.enums.UploadStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileRepository;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceRepository;
import com.coffiness.calfit.support.email.ApplicantEmailMessage;
import com.coffiness.calfit.support.email.EmailProperties;
import com.coffiness.calfit.support.email.EmailService;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationService {

  private static final String DEFAULT_TENANT = "default";
  private static final String DEFAULT_COMPANY_NAME = "CalFit";

  private final ApplicationRepository applicationRepository;
  private final ApplicationFileRepository applicationFileRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final WorkspaceRepository workspaceRepository;
  private final EmailService emailService;
  private final EmailProperties emailProperties;

  @Transactional
  public Long create(ApplicationCreateRequest request, Long requesterUserId) {
    ensureTenantFromRecruitment(request.recruitmentId());

    Long applicantId = request.applicantId() != null ? request.applicantId() : requesterUserId;
    if (requesterUserId == null || !requesterUserId.equals(applicantId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (applicationRepository.existsByApplicantIdAndRecruitmentId(
        applicantId, request.recruitmentId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    RecruitmentEntity recruitment = getOpenRecruitment(request.recruitmentId());
    Long templateId = resolveTemplateId(request.templateId(), recruitment);
    Long recruitmentProcessId =
        resolveRecruitmentProcessId(request.recruitmentProcessId(), request.recruitmentId());

    LocalDateTime birthDate = request.birthDate().atStartOfDay();
    ApplicationEntity entity =
        ApplicationEntity.create(
            applicantId,
            request.recruitmentId(),
            recruitmentProcessId,
            templateId,
            request.name(),
            request.gender(),
            birthDate,
            request.phone(),
            request.email(),
            request.schema().toString());

    ApplicationEntity saved = applicationRepository.save(entity);
    return saved.getId();
  }

  @Transactional(readOnly = true)
  public List<ApplicationSummaryResponse> listByRecruitment(Long recruitmentId) {
    requireTenant();
    List<ApplicationEntity> entities =
        applicationRepository.findByRecruitmentIdAndStatus(recruitmentId, EntityStatus.ACTIVE);
    return entities.stream().map(ApplicationSummaryResponse::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ApplicationSummaryResponse> listAll() {
    requireTenant();
    List<ApplicationEntity> entities = applicationRepository.findByStatus(EntityStatus.ACTIVE);
    return entities.stream().map(ApplicationSummaryResponse::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ApplicationDetailResponse getDetail(Long applicationId) {
    requireTenant();
    ApplicationEntity entity = getActiveApplication(applicationId);
    return buildDetailResponse(entity);
  }

  @Transactional
  public ApplicationDetailResponse updateStage(
      Long applicationId, ApplicationStageUpdateRequest request) {
    requireTenant();

    ApplicationEntity application = getActiveApplication(applicationId);
    RecruitmentStageEntity destinationStage = getRecruitmentStage(request.recruitmentProcessId());
    if (!Objects.equals(destinationStage.getRecruitmentId(), application.getRecruitmentId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    RecruitmentStageEntity currentStage =
        getRecruitmentStage(application.getRecruitmentProcessId());
    if (Objects.equals(currentStage.getId(), destinationStage.getId())) {
      return buildDetailResponse(application);
    }

    application.changeRecruitmentProcess(destinationStage.getId());
    sendStageNotification(application, currentStage, destinationStage);
    return buildDetailResponse(application);
  }

  private void sendStageNotification(
      ApplicationEntity application,
      RecruitmentStageEntity currentStage,
      RecruitmentStageEntity destinationStage) {
    RecruitmentEntity recruitment = getRecruitment(application.getRecruitmentId());
    ApplicantEmailMessage message =
        buildApplicantEmailMessage(application, recruitment, currentStage, destinationStage);
    if (message == null) {
      return;
    }
    emailService.sendApplicantResultEmail(emailProperties.getSender(), message);
  }

  private ApplicantEmailMessage buildApplicantEmailMessage(
      ApplicationEntity application,
      RecruitmentEntity recruitment,
      RecruitmentStageEntity currentStage,
      RecruitmentStageEntity destinationStage) {
    String companyName = resolveCompanyName(recruitment.getId());
    String positionName = recruitment.getTitle();

    if (destinationStage.getStageType() == RecruitmentStageType.FAIL) {
      return ApplicantEmailMessage.builder()
          .applicantEmail(application.getEmail())
          .applicantName(application.getName())
          .companyName(companyName)
          .positionName(positionName)
          .accepted(false)
          .subject(String.format("[%s] %s 전형 결과 안내", companyName, positionName))
          .headline("전형 결과를 안내드립니다")
          .summary("아쉽게도 이번 전형에서는 다음 단계로 함께하지 못하게 되었습니다.")
          .highlightLabel("마지막 검토 단계")
          .highlightValue(currentStage.getStageName())
          .build();
    }

    if (destinationStage.getStageType() == RecruitmentStageType.PASS) {
      return ApplicantEmailMessage.builder()
          .applicantEmail(application.getEmail())
          .applicantName(application.getName())
          .companyName(companyName)
          .positionName(positionName)
          .accepted(true)
          .subject(String.format("[%s] %s 최종 합격 안내", companyName, positionName))
          .headline("최종 합격을 축하드립니다")
          .summary("모든 전형을 통과하셨습니다. 이후 절차는 담당자가 별도로 안내드릴 예정입니다.")
          .highlightLabel("현재 상태")
          .highlightValue(destinationStage.getStageName())
          .build();
    }

    if (currentStage.getStageType() == RecruitmentStageType.DOCUMENT
        && destinationStage.getStageType() != RecruitmentStageType.DOCUMENT) {
      return ApplicantEmailMessage.builder()
          .applicantEmail(application.getEmail())
          .applicantName(application.getName())
          .companyName(companyName)
          .positionName(positionName)
          .accepted(true)
          .subject(String.format("[%s] %s 서류 전형 합격 안내", companyName, positionName))
          .headline("서류 전형 합격을 축하드립니다")
          .summary("서류 검토가 완료되었으며 다음 전형으로 이동했습니다. 세부 일정은 담당자가 별도로 안내드립니다.")
          .highlightLabel("다음 전형")
          .highlightValue(destinationStage.getStageName())
          .build();
    }

    return null;
  }

  private String resolveCompanyName(Long recruitmentId) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || DEFAULT_TENANT.equals(tenantId)) {
      tenantId = recruitmentRepository.findTenantIdById(recruitmentId).orElse(null);
    }
    if (tenantId == null || tenantId.isBlank()) {
      return DEFAULT_COMPANY_NAME;
    }
    return workspaceRepository
        .findByWorkspaceId(tenantId)
        .map(workspace -> workspace.getName())
        .orElse(DEFAULT_COMPANY_NAME);
  }

  private RecruitmentEntity getOpenRecruitment(Long recruitmentId) {
    RecruitmentEntity recruitment = getRecruitment(recruitmentId);

    if (recruitment.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    return recruitment;
  }

  private RecruitmentEntity getRecruitment(Long recruitmentId) {
    return recruitmentRepository
        .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  private ApplicationEntity getActiveApplication(Long applicationId) {
    return applicationRepository
        .findByIdAndStatus(applicationId, EntityStatus.ACTIVE)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  private RecruitmentStageEntity getRecruitmentStage(Long stageId) {
    return recruitmentStageRepository
        .findById(stageId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  private ApplicationDetailResponse buildDetailResponse(ApplicationEntity entity) {
    List<ApplicationFileEntity> files =
        applicationFileRepository.findByApplicationIdAndStatusAndUploadStatus(
            entity.getId(), EntityStatus.ACTIVE, UploadStatus.ACTIVE);
    List<ApplicationFileResponse> fileResponses =
        files.stream().map(ApplicationFileResponse::from).collect(Collectors.toList());
    return ApplicationDetailResponse.from(entity, fileResponses);
  }

  private Long resolveTemplateId(Long requestTemplateId, RecruitmentEntity recruitment) {
    Long recruitmentTemplateId = recruitment.getApplicationTemplateId();
    if (requestTemplateId == null) {
      return recruitmentTemplateId;
    }
    if (!recruitmentTemplateId.equals(requestTemplateId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return requestTemplateId;
  }

  private Long resolveRecruitmentProcessId(Long requestProcessId, Long recruitmentId) {
    if (requestProcessId == null) {
      return recruitmentStageRepository
          .findByRecruitmentIdOrderByStageStepAsc(recruitmentId)
          .stream()
          .findFirst()
          .map(stage -> stage.getId())
          .orElseThrow(() -> new CoreException(ErrorType.VALIDATION_ERROR));
    }

    if (!recruitmentStageRepository.existsByIdAndRecruitmentId(requestProcessId, recruitmentId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    return requestProcessId;
  }

  private void requireTenant() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || DEFAULT_TENANT.equals(tenantId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  private void ensureTenantFromRecruitment(Long recruitmentId) {
    String currentTenantId = TenantContext.getTenantId();
    if (currentTenantId != null
        && !currentTenantId.isBlank()
        && !DEFAULT_TENANT.equals(currentTenantId)) {
      return;
    }
    if (recruitmentId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    String tenantId =
        recruitmentRepository
            .findTenantIdById(recruitmentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    TenantContext.setTenantId(tenantId);
  }
}
