package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.recruitment.Recruitment;
import com.coffiness.calfit.domain.recruitment.RecruitmentStore;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.*;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentStoreImpl implements RecruitmentStore {

  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final RecruitmentReferenceGroupRepository recruitmentReferenceGroupRepository;
  private final RecruitmentInterviewerRepository recruitmentInterviewerRepository;
  private final MemberReader memberReader;

  @Override
  public Recruitment store(Recruitment recruitment) {
    RecruitmentEntity entity =
        RecruitmentEntity.builder()
            .creatorId(recruitment.creatorId())
            .title(recruitment.title())
            .recruitmentStatus(recruitment.recruitmentStatus())
            .targetCount(recruitment.targetCount())
            .startDate(recruitment.startDate())
            .endDate(recruitment.endDate())
            .applicationTemplateId(recruitment.applicationTemplateId())
            .contents(recruitment.contents())
            .careerType(recruitment.careerType())
            .minExperienceYears(recruitment.minExperienceYears())
            .maxExperienceYears(recruitment.maxExperienceYears())
            .leadGroupId(recruitment.leadGroupId())
            .build();

    RecruitmentEntity savedEntity = recruitmentRepository.save(entity);
    Long newId = savedEntity.getId();

    insertChildren(newId, recruitment);

    return new Recruitment(
        newId,
        recruitment.creatorId(),
        recruitment.title(),
        recruitment.contents(),
        recruitment.recruitmentStatus(),
        recruitment.targetCount(),
        recruitment.startDate(),
        recruitment.endDate(),
        recruitment.applicationTemplateId(),
        recruitment.careerType(),
        recruitment.minExperienceYears(),
        recruitment.maxExperienceYears(),
        recruitment.leadGroupId(),
        recruitment.referenceGroupIds(),
        recruitment.interviewerIds(),
        recruitment.stages());
  }

  @Override
  public Recruitment update(Recruitment recruitment) {
    if (recruitment.id() == null) {
      throw new IllegalArgumentException("수정할 채용 공고 ID가 없습니다.");
    }

    recruitmentStageRepository.deleteAllByRecruitmentId(recruitment.id());
    recruitmentInterviewerRepository.deleteAllByRecruitmentId(recruitment.id());
    recruitmentReferenceGroupRepository.deleteAllByRecruitmentId(recruitment.id());

    RecruitmentEntity entity =
        recruitmentRepository
            .findById(recruitment.id())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채용 공고입니다."));

    entity.update(
        recruitment.title(),
        recruitment.recruitmentStatus(),
        recruitment.targetCount(),
        recruitment.startDate(),
        recruitment.endDate(),
        recruitment.applicationTemplateId(),
        recruitment.contents(),
        recruitment.careerType(),
        recruitment.minExperienceYears(),
        recruitment.maxExperienceYears(),
        recruitment.leadGroupId());

    insertChildren(recruitment.id(), recruitment);

    return recruitment;
  }

  @Override
  public void delete(Long recruitmentId) {
    RecruitmentEntity entity =
        recruitmentRepository
            .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("이미 삭제되었거나 존재하지 않는 채용 공고입니다."));

    entity.deleted();
  }

  private void insertChildren(Long recruitmentId, Recruitment recruitment) {
    if (recruitment.stages() != null && !recruitment.stages().isEmpty()) {
      List<RecruitmentStageEntity> stageEntities =
          recruitment.stages().stream()
              .map(
                  s ->
                      RecruitmentStageEntity.builder()
                          .recruitmentId(recruitmentId)
                          .stageName(s.stageName())
                          .stageType(s.stageType())
                          .stageStep(s.stageStep())
                          .build())
              .toList();
      recruitmentStageRepository.saveAll(stageEntities);
    }

    if (recruitment.interviewerIds() != null && !recruitment.interviewerIds().isEmpty()) {
      String tenantId = TenantContext.getTenantId();
      List<Long> interviewerMemberIds =
          recruitment.interviewerIds().stream()
              .filter(Objects::nonNull)
              .distinct()
              .map(
                  userId -> {
                    if (tenantId == null || tenantId.isBlank()) {
                      throw new CoreException(ErrorType.UNAUTHORIZED);
                    }
                    return memberReader.getMember(tenantId, userId).id();
                  })
              .filter(Objects::nonNull)
              .toList();
      List<RecruitmentInterviewerEntity> interviewerEntities =
          interviewerMemberIds.stream()
              .map(
                  i ->
                      RecruitmentInterviewerEntity.builder()
                          .recruitmentId(recruitmentId)
                          .memberId(i)
                          .build())
              .toList();
      recruitmentInterviewerRepository.saveAll(interviewerEntities);
    }

    if (recruitment.referenceGroupIds() != null && !recruitment.referenceGroupIds().isEmpty()) {
      List<RecruitmentReferenceGroupEntity> referenceGroupEntities =
          recruitment.referenceGroupIds().stream()
              .map(
                  groupId ->
                      RecruitmentReferenceGroupEntity.builder()
                          .recruitmentId(recruitmentId)
                          .groupId(groupId)
                          .build())
              .toList();
      recruitmentReferenceGroupRepository.saveAll(referenceGroupEntities);
    }
  }
}
