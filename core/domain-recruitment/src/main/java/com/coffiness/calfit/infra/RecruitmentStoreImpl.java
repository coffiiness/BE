package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.recruitment.Recruitment;
import com.coffiness.calfit.domain.recruitment.RecruitmentStore;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentInterviewerEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentInterviewerRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentReferenceGroupEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentReferenceGroupRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentStoreImpl implements RecruitmentStore {

  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final RecruitmentReferenceGroupRepository recruitmentReferenceGroupRepository;
  private final RecruitmentInterviewerRepository recruitmentInterviewerRepository;

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

  @Override
  // 시작 시각이 지난 DRAFT 공고를 OPEN으로 전환
  public int openScheduledRecruitments(LocalDateTime currentTime) {
    return recruitmentRepository.openScheduledRecruitments(
        currentTime, EntityStatus.ACTIVE, RecruitmentStatus.DRAFT, RecruitmentStatus.OPEN);
  }

  @Override
  // 종료 시각이 지난 DRAFT/OPEN 공고를 CLOSED로 전환
  public int closeEndedRecruitments(LocalDateTime currentTime) {
    return recruitmentRepository.closeEndedRecruitments(
        currentTime,
        EntityStatus.ACTIVE,
        List.of(RecruitmentStatus.DRAFT, RecruitmentStatus.OPEN),
        RecruitmentStatus.CLOSED);
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
      List<RecruitmentInterviewerEntity> interviewerEntities =
          recruitment.interviewerIds().stream()
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
