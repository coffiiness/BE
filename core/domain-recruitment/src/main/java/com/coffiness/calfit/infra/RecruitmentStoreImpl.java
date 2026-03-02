package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.RecruitmentActionType;
import com.coffiness.calfit.domain.recruitment.Recruitment;
import com.coffiness.calfit.domain.recruitment.RecruitmentStore;
import com.coffiness.calfit.storage.db.core.recruitment.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentStoreImpl implements RecruitmentStore {

  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final RecruitmentHistoryRepository recruitmentHistoryRepository;
  private final RecruitmentReferenceGroupRepository recruitmentReferenceGroupRepository;
  private final RecruitmentInterviewerRepository recruitmentInterviewerRepository;

  @Override
  public Recruitment store(Recruitment recruitment) {
    RecruitmentEntity entity =
        RecruitmentEntity.builder()
            .creatorId(recruitment.creatorId())
            .title(recruitment.title())
            .recruitmentStatus(recruitment.status())
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

    if (recruitment.stages() != null && !recruitment.stages().isEmpty()) {
      List<RecruitmentStageEntity> stageEntities =
          recruitment.stages().stream()
              .map(
                  s ->
                      RecruitmentStageEntity.builder()
                          .recruitmentId(newId)
                          .stageName(s.stageName())
                          .stageType(s.stageType())
                          .stageStep(s.stageStep())
                          .build())
              .toList();
      recruitmentStageRepository.saveAll(stageEntities);
    }

    if (recruitment.interviewerIds() != null && !recruitment.interviewerIds().isEmpty()) {
      List<RecruitmentInterviewerEntity> interviewerEntities =
          recruitment.interviewerIds().stream()
              .map(
                  i ->
                      RecruitmentInterviewerEntity.builder()
                          .recruitmentId(newId)
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
                          .recruitmentId(newId)
                          .groupId(groupId)
                          .build())
              .toList();
      recruitmentReferenceGroupRepository.saveAll(referenceGroupEntities);
    }

    RecruitmentHistoryEntity historyEntity =
        RecruitmentHistoryEntity.builder()
            .recruitmentId(newId)
            .actorId(entity.getCreatorId())
            .recruitmentActionType(RecruitmentActionType.RECRUITMENT_CREATED)
            .build();

    recruitmentHistoryRepository.save(historyEntity);

    return new Recruitment(
        newId,
        recruitment.creatorId(),
        recruitment.title(),
        recruitment.contents(),
        recruitment.status(),
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
}
