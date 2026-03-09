package com.coffiness.calfit.domain.application;

import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.api.v1.response.ApplicationDetailResponse;
import com.coffiness.calfit.api.v1.response.ApplicationFileResponse;
import com.coffiness.calfit.api.v1.response.ApplicationSummaryResponse;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.UploadStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileRepository;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.automation.AutomationEventEntity;
import com.coffiness.calfit.storage.db.core.automation.AutomationEventRepository;
import com.coffiness.calfit.storage.db.core.automation.AutomationRuleEntity;
import com.coffiness.calfit.storage.db.core.automation.AutomationRuleRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.automation.ApplicationProcessHistoryEntity;
import com.coffiness.calfit.storage.db.core.automation.ApplicationProcessHistoryRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.core.enums.AutomationEventStatus;
import com.coffiness.calfit.core.enums.AutomationTriggerType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationService {

  private static final String DEFAULT_TENANT = "default";

  private final ApplicationRepository applicationRepository;
  private final ApplicationFileRepository applicationFileRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final ApplicationProcessHistoryRepository applicationProcessHistoryRepository;
  private final AutomationRuleRepository automationRuleRepository;
  private final AutomationEventRepository automationEventRepository;
  private final AutomationEventExecutor automationEventExecutor;

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
    ApplicationEntity entity =
        applicationRepository
            .findByIdAndStatus(applicationId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    List<ApplicationFileEntity> files =
        applicationFileRepository.findByApplicationIdAndStatusAndUploadStatus(
            applicationId, EntityStatus.ACTIVE, UploadStatus.ACTIVE);
    List<ApplicationFileResponse> fileResponses =
        files.stream().map(ApplicationFileResponse::from).collect(Collectors.toList());

    return ApplicationDetailResponse.from(entity, fileResponses);
  }

  @Transactional(readOnly = true)
  public KanbanBoard getKanbanBoard(Long recruitmentId) {
    requireTenant();
    if (recruitmentId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    List<RecruitmentStageEntity> stages = recruitmentStageRepository.findByRecruitmentId(recruitmentId);
    if (stages.isEmpty()) {
      return new KanbanBoard(List.of());
    }

    List<KanbanColumn> columns =
        stages.stream()
            .sorted((a, b) -> a.getStageStep().compareTo(b.getStageStep()))
            .map(
                stage -> {
                  List<ApplicationEntity> applications =
                      applicationRepository.findByRecruitmentIdAndRecruitmentProcessIdAndStatus(
                          recruitmentId, stage.getId(), EntityStatus.ACTIVE);
                  List<ApplicationSummaryResponse> summaries =
                      applications.stream().map(ApplicationSummaryResponse::from).toList();
                  return new KanbanColumn(
                      stage.getId(), stage.getStageName(), stage.getStageStep(), summaries);
                })
            .toList();

    return new KanbanBoard(columns);
  }

  @Transactional
  public void updateProcess(Long applicationId, Long recruitmentProcessId, Long actorId) {
    requireTenant();
    if (applicationId == null || recruitmentProcessId == null || actorId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    ApplicationEntity entity =
        applicationRepository
            .findByIdAndStatus(applicationId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    if (!recruitmentStageRepository.existsByIdAndRecruitmentId(
        recruitmentProcessId, entity.getRecruitmentId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    if (recruitmentProcessId.equals(entity.getRecruitmentProcessId())) {
      return;
    }

    Long beforeProcessId = entity.getRecruitmentProcessId();
    entity.updateRecruitmentProcessId(recruitmentProcessId);

    applicationProcessHistoryRepository.save(
        ApplicationProcessHistoryEntity.builder()
            .applicationId(applicationId)
            .fromRecruitmentProcessId(beforeProcessId)
            .toRecruitmentProcessId(recruitmentProcessId)
            .actorId(actorId)
            .build());

    List<AutomationRuleEntity> rules =
        automationRuleRepository.findByRecruitmentIdAndRecruitmentProcessIdAndTriggerType(
            entity.getRecruitmentId(), recruitmentProcessId, AutomationTriggerType.ON_ENTER);
    for (AutomationRuleEntity rule : rules) {
      automationEventRepository.save(
          AutomationEventEntity.builder()
              .applicationId(applicationId)
              .ruleId(rule.getId())
              .actionType(rule.getActionType())
              .eventStatus(AutomationEventStatus.PENDING)
              .build());
    }

    automationEventExecutor.executePendingForApplication(applicationId);
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
