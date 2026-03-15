package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentUpdateRequest;
import com.coffiness.calfit.core.enums.RecruitmentActionType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitmentService {

  private static final long SYSTEM_ACTOR_ID = 0L;
  private static final String RECRUITMENT_SCOPE = "RECRUITMENT";
  private static final String INTERVIEWER_SCOPE = "INTERVIEWER";

  private final RecruitmentStore recruitmentStore;
  private final RecruitmentHistoryAppender recruitmentHistoryAppender;
  private final RecruitmentReader recruitmentReader;

  public Long createRecruitment(long userId, RecruitmentCreateRequest request) {

    validateCreatableSchedule(request.startDate(), request.endDate(), LocalDateTime.now());

    List<RecruitmentStage> stages = toStagesWithRequiredFail(request.stages());

    RecruitmentStatus initialStatus =
        resolveStatusAtCreation(request.startDate(), request.endDate(), LocalDateTime.now());

    Recruitment newRecruitment =
        new Recruitment(
            null,
            userId,
            request.title(),
            request.contents(),
            initialStatus,
            request.targetCount(),
            request.startDate(),
            request.endDate(),
            request.applicationTemplateId(),
            request.careerType(),
            request.minExperienceYears(),
            request.maxExperienceYears(),
            request.leadGroupId(),
            request.referenceGroupIds(),
            request.interviewerIds(),
            stages);

    Recruitment saveRecruitment = recruitmentStore.store(newRecruitment);

    appendRecruitmentHistory(
        saveRecruitment.creatorId(),
        RecruitmentActionType.RECRUITMENT_CREATED,
        "공고 생성",
        null,
        saveRecruitment);

    return saveRecruitment.id();
  }

  public Recruitment updateRecruitment(
      long memberId, Long recruitmentId, RecruitmentUpdateRequest request) {

    Recruitment recruitment = recruitmentReader.readById(recruitmentId);
    if (recruitment == null) {
      throw new IllegalArgumentException("존재하지 않는 채용 공고입니다.");
    }

    recruitment.validateUpdatable(LocalDateTime.now());

    List<RecruitmentStage> newStages = toStagesWithRequiredFail(request.stages());

    Recruitment updatedRecruitment =
        recruitment.updateDetails(
            request.title(),
            request.contents(),
            request.targetCount(),
            request.startDate(),
            request.endDate(),
            request.applicationTemplateId(),
            request.careerType(),
            request.minExperienceYears(),
            request.maxExperienceYears(),
            request.leadGroupId(),
            request.referenceGroupIds(),
            request.interviewerIds(),
            newStages);

    Recruitment savedRecruitment = recruitmentStore.update(updatedRecruitment);

    List<String> recruitmentChangedFields =
        getRecruitmentChangedFields(recruitment, savedRecruitment);
    if (!recruitmentChangedFields.isEmpty()) {
      recruitmentHistoryAppender.append(
          savedRecruitment.id(),
          memberId,
          RecruitmentActionType.RECRUITMENT_INFO_UPDATED,
          "채용 공고 수정",
          RECRUITMENT_SCOPE,
          toRecruitmentHistorySnapshot(recruitment),
          toRecruitmentHistorySnapshot(savedRecruitment),
          recruitmentChangedFields);
    }

    appendInterviewerHistory(memberId, recruitment, savedRecruitment);
    appendStageHistories(memberId, recruitment, savedRecruitment);

    return savedRecruitment;
  }

  // 게시 여부와 무관하게 채용 공고의 면접관 목록만 별도로 수정
  public Recruitment updateRecruitmentInterviewers(
      long memberId, Long recruitmentId, List<Long> interviewerIds) {
    Recruitment recruitment = recruitmentReader.readById(recruitmentId);
    if (recruitment == null) {
      throw new IllegalArgumentException("존재하지 않는 채용 공고입니다.");
    }

    Recruitment updatedRecruitment = recruitment.updateInterviewers(interviewerIds);
    Recruitment savedRecruitment = recruitmentStore.updateInterviewers(updatedRecruitment);

    appendInterviewerHistory(memberId, recruitment, savedRecruitment);

    return savedRecruitment;
  }

  // DRAFT 공고를 즉시 게시하고 실제 시작 시각도 현재로 반영
  public Recruitment publishRecruitmentNow(long memberId, Long recruitmentId) {
    return publishRecruitmentNow(memberId, recruitmentId, LocalDateTime.now());
  }

  // 테스트와 스케줄링 검증을 위해 기준 시각을 주입받는 게시 전환 로직
  public Recruitment publishRecruitmentNow(
      long memberId, Long recruitmentId, LocalDateTime currentTime) {
    Recruitment recruitment = recruitmentReader.readById(recruitmentId);
    if (recruitment == null) {
      throw new IllegalArgumentException("존재하지 않는 채용 공고입니다.");
    }

    Recruitment publishedRecruitment = recruitment.publishNow(currentTime);
    Recruitment savedRecruitment = recruitmentStore.update(publishedRecruitment);

    appendRecruitmentHistory(
        memberId,
        RecruitmentActionType.RECRUITMENT_OPENED,
        "채용 공고 즉시 게시",
        recruitment,
        savedRecruitment);

    return savedRecruitment;
  }

  public void deleteRecruitment(Long memberId, Long recruitmentId) {
    Recruitment recruitment = recruitmentReader.readById(recruitmentId);
    if (recruitment == null) {
      throw new IllegalArgumentException("존재하지 않는 채용 공고입니다.");
    }

    if (recruitment.recruitmentStatus() != RecruitmentStatus.DRAFT) {
      throw new IllegalArgumentException("진행중이거나 마감된 채용 공고는 삭제할 수 없습니다.");
    }

    recruitmentStore.delete(recruitmentId);

    appendRecruitmentHistory(
        memberId, RecruitmentActionType.RECRUITMENT_DELETE, "채용 공고 삭제", recruitment, null);
  }

  public RecruitmentStatusTransitionResult updateRecruitmentStatusBySchedule(
      LocalDateTime currentTime) {
    List<Recruitment> recruitmentsToOpen = recruitmentReader.readScheduledToOpen(currentTime);
    List<Recruitment> recruitmentsToClose = recruitmentReader.readScheduledToClose(currentTime);

    int closedCount = recruitmentStore.closeEndedRecruitments(currentTime);
    int openedCount = recruitmentStore.openScheduledRecruitments(currentTime);

    appendScheduledStatusHistories(
        recruitmentsToOpen,
        RecruitmentStatus.OPEN,
        RecruitmentActionType.RECRUITMENT_OPENED,
        "채용 공고 자동 게시");
    appendScheduledStatusHistories(
        recruitmentsToClose,
        RecruitmentStatus.CLOSED,
        RecruitmentActionType.RECRUITMENT_CLOSED,
        "채용 공고 자동 마감");

    return new RecruitmentStatusTransitionResult(openedCount, closedCount);
  }

  public RecruitmentStatusTransitionResult updateRecruitmentStatusBySchedule() {
    return updateRecruitmentStatusBySchedule(LocalDateTime.now());
  }

  // 면접관 추가/해제 이력을 변경 내용에 맞춰 남김
  private void appendInterviewerHistory(
      long memberId, Recruitment beforeRecruitment, Recruitment afterRecruitment) {
    Set<Long> beforeIds = new LinkedHashSet<>(beforeRecruitment.interviewerIds());
    Set<Long> afterIds = new LinkedHashSet<>(afterRecruitment.interviewerIds());

    Set<Long> addedIds = new LinkedHashSet<>(afterIds);
    addedIds.removeAll(beforeIds);
    if (!addedIds.isEmpty()) {
      recruitmentHistoryAppender.append(
          afterRecruitment.id(),
          memberId,
          RecruitmentActionType.INTERVIEWER_ADDED,
          "면접관 설정 수정",
          INTERVIEWER_SCOPE,
          toInterviewerHistorySnapshot(beforeIds),
          toInterviewerHistorySnapshot(mergeInterviewerIds(beforeIds, addedIds)),
          List.of("interviewerIds"));
    }

    Set<Long> removedIds = new LinkedHashSet<>(beforeIds);
    removedIds.removeAll(afterIds);
    if (!removedIds.isEmpty()) {
      Set<Long> removalBeforeIds = mergeInterviewerIds(beforeIds, addedIds);
      recruitmentHistoryAppender.append(
          afterRecruitment.id(),
          memberId,
          RecruitmentActionType.INTERVIEWER_REMOVED,
          "면접관 설정 수정",
          INTERVIEWER_SCOPE,
          toInterviewerHistorySnapshot(removalBeforeIds),
          toInterviewerHistorySnapshot(afterIds),
          List.of("interviewerIds"));
    }
  }

  // 자동 상태 전환 이력 적재
  private void appendScheduledStatusHistories(
      List<Recruitment> recruitments,
      RecruitmentStatus targetStatus,
      RecruitmentActionType actionType,
      String reason) {
    for (Recruitment recruitment : recruitments) {
      appendRecruitmentHistory(
          SYSTEM_ACTOR_ID, actionType, reason, recruitment, recruitment.updateStatus(targetStatus));
    }
  }

  // 채용 공고 변경 필드 추출
  private List<String> getRecruitmentChangedFields(
      Recruitment beforeRecruitment, Recruitment afterRecruitment) {
    return determineChangedFields(
        toRecruitmentHistorySnapshot(beforeRecruitment),
        toRecruitmentHistorySnapshot(afterRecruitment),
        Set.of());
  }

  // 채용 단계 이력 적재
  private void appendStageHistories(
      long memberId, Recruitment beforeRecruitment, Recruitment afterRecruitment) {
    List<RecruitmentStage> beforeStages = filterVisibleStages(beforeRecruitment.stages());
    List<RecruitmentStage> afterStages = filterVisibleStages(afterRecruitment.stages());

    List<RecruitmentStage> remainingBefore = new ArrayList<>(beforeStages);
    List<RecruitmentStage> remainingAfter = new ArrayList<>(afterStages);

    List<StageChange> reorderChanges = new ArrayList<>();
    List<StageChange> updateChanges = new ArrayList<>();
    List<RecruitmentStage> deletedStages = new ArrayList<>();
    List<RecruitmentStage> createdStages = new ArrayList<>();

    for (RecruitmentStage beforeStage : beforeStages) {
      RecruitmentStage matchedAfter = findStageBySignature(remainingAfter, beforeStage);
      if (matchedAfter == null) {
        continue;
      }

      remainingBefore.remove(beforeStage);
      remainingAfter.remove(matchedAfter);

      if (!Objects.equals(beforeStage.stageStep(), matchedAfter.stageStep())) {
        reorderChanges.add(new StageChange(beforeStage, matchedAfter));
      }
    }

    for (RecruitmentStage beforeStage : List.copyOf(remainingBefore)) {
      RecruitmentStage matchedAfter = findStageByStep(remainingAfter, beforeStage.stageStep());
      if (matchedAfter == null) {
        continue;
      }

      remainingBefore.remove(beforeStage);
      remainingAfter.remove(matchedAfter);
      updateChanges.add(new StageChange(beforeStage, matchedAfter));
    }

    deletedStages.addAll(remainingBefore);
    createdStages.addAll(remainingAfter);

    for (StageChange reorderChange : reorderChanges) {
      recruitmentHistoryAppender.appendStage(
          afterRecruitment.id(),
          reorderChange.before().id(),
          memberId,
          RecruitmentActionType.STAGE_REORDERED,
          "채용 단계 순서 변경",
          toStageChangeLog(reorderChange.before()),
          toStageChangeLog(reorderChange.after()),
          getStageChangedFields(reorderChange.before(), reorderChange.after()));
    }

    for (StageChange updateChange : updateChanges) {
      recruitmentHistoryAppender.appendStage(
          afterRecruitment.id(),
          updateChange.before().id(),
          memberId,
          RecruitmentActionType.STAGE_UPDATED,
          "채용 단계 수정",
          toStageChangeLog(updateChange.before()),
          toStageChangeLog(updateChange.after()),
          getStageChangedFields(updateChange.before(), updateChange.after()));
    }

    for (RecruitmentStage deletedStage : deletedStages) {
      recruitmentHistoryAppender.appendStage(
          afterRecruitment.id(),
          deletedStage.id(),
          memberId,
          RecruitmentActionType.STAGE_DELETED,
          "채용 단계 삭제",
          toStageChangeLog(deletedStage),
          null,
          getStageChangedFields(deletedStage, null));
    }

    for (RecruitmentStage createdStage : createdStages) {
      recruitmentHistoryAppender.appendStage(
          afterRecruitment.id(),
          createdStage.id(),
          memberId,
          RecruitmentActionType.STAGE_CREATED,
          "채용 단계 생성",
          null,
          toStageChangeLog(createdStage),
          getStageChangedFields(null, createdStage));
    }
  }

  // 사용자 노출 채용 단계 필터링
  private List<RecruitmentStage> filterVisibleStages(List<RecruitmentStage> stages) {
    if (stages == null || stages.isEmpty()) {
      return List.of();
    }

    return stages.stream().filter(stage -> stage.stageType() != RecruitmentStageType.FAIL).toList();
  }

  // 단계 이름과 타입 기준 단계 탐색
  private RecruitmentStage findStageBySignature(
      List<RecruitmentStage> candidates, RecruitmentStage target) {
    return candidates.stream()
        .filter(
            candidate ->
                Objects.equals(candidate.stageName(), target.stageName())
                    && candidate.stageType() == target.stageType())
        .findFirst()
        .orElse(null);
  }

  // 단계 순서 기준 단계 탐색
  private RecruitmentStage findStageByStep(List<RecruitmentStage> candidates, Integer stageStep) {
    return candidates.stream()
        .filter(candidate -> Objects.equals(candidate.stageStep(), stageStep))
        .findFirst()
        .orElse(null);
  }

  // 채용 단계 변경 로그 생성
  private Map<String, Object> toStageChangeLog(RecruitmentStage stage) {
    if (stage == null) {
      return null;
    }

    Map<String, Object> changeLog = new HashMap<>();
    changeLog.put("id", stage.id());
    changeLog.put("stageName", stage.stageName());
    changeLog.put("stageStep", stage.stageStep());
    changeLog.put("stageType", stage.stageType() != null ? stage.stageType().name() : null);
    return changeLog;
  }

  // 채용 공고 이력 적재
  private void appendRecruitmentHistory(
      long actorId,
      RecruitmentActionType actionType,
      String reason,
      Recruitment beforeRecruitment,
      Recruitment afterRecruitment) {
    Recruitment targetRecruitment =
        afterRecruitment != null ? afterRecruitment : Objects.requireNonNull(beforeRecruitment);

    recruitmentHistoryAppender.append(
        targetRecruitment.id(),
        actorId,
        actionType,
        reason,
        RECRUITMENT_SCOPE,
        toRecruitmentHistorySnapshot(beforeRecruitment),
        toRecruitmentHistorySnapshot(afterRecruitment),
        getRecruitmentChangedFields(beforeRecruitment, afterRecruitment));
  }

  // 채용 공고 이력 스냅샷 생성
  private Map<String, Object> toRecruitmentHistorySnapshot(Recruitment recruitment) {
    if (recruitment == null) {
      return null;
    }

    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("title", recruitment.title());
    snapshot.put("contents", recruitment.contents());
    snapshot.put(
        "recruitmentStatus",
        recruitment.recruitmentStatus() != null ? recruitment.recruitmentStatus().name() : null);
    snapshot.put("targetCount", recruitment.targetCount());
    snapshot.put(
        "startDate", recruitment.startDate() != null ? recruitment.startDate().toString() : null);
    snapshot.put(
        "endDate", recruitment.endDate() != null ? recruitment.endDate().toString() : null);
    snapshot.put("applicationTemplateId", recruitment.applicationTemplateId());
    snapshot.put(
        "careerType", recruitment.careerType() != null ? recruitment.careerType().name() : null);
    snapshot.put("minExperienceYears", recruitment.minExperienceYears());
    snapshot.put("maxExperienceYears", recruitment.maxExperienceYears());
    snapshot.put("leadGroupId", recruitment.leadGroupId());
    snapshot.put(
        "referenceGroupIds",
        recruitment.referenceGroupIds() == null
            ? List.of()
            : List.copyOf(recruitment.referenceGroupIds()));
    return snapshot;
  }

  // 면접관 이력 스냅샷 생성
  private Map<String, Object> toInterviewerHistorySnapshot(Set<Long> interviewerIds) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("interviewerIds", List.copyOf(interviewerIds));
    return snapshot;
  }

  // 채용 단계 변경 필드 추출
  private List<String> getStageChangedFields(
      RecruitmentStage beforeStage, RecruitmentStage afterStage) {
    return determineChangedFields(
        toStageChangeLog(beforeStage), toStageChangeLog(afterStage), Set.of("id"));
  }

  // 변경 필드 목록 추출
  private List<String> determineChangedFields(
      Map<String, Object> beforeChangeLog,
      Map<String, Object> afterChangeLog,
      Set<String> ignoredFields) {
    Set<String> fieldNames = new LinkedHashSet<>();
    if (beforeChangeLog != null) {
      fieldNames.addAll(beforeChangeLog.keySet());
    }
    if (afterChangeLog != null) {
      fieldNames.addAll(afterChangeLog.keySet());
    }
    fieldNames.removeAll(ignoredFields);

    List<String> changedFields = new ArrayList<>();
    for (String fieldName : fieldNames) {
      Object beforeValue = beforeChangeLog != null ? beforeChangeLog.get(fieldName) : null;
      Object afterValue = afterChangeLog != null ? afterChangeLog.get(fieldName) : null;
      if (!Objects.equals(beforeValue, afterValue)) {
        changedFields.add(fieldName);
      }
    }
    return changedFields;
  }

  // 면접관 반영 후 목록 생성
  private Set<Long> mergeInterviewerIds(Set<Long> baseIds, Set<Long> addedIds) {
    Set<Long> mergedIds = new LinkedHashSet<>(baseIds);
    mergedIds.addAll(addedIds);
    return mergedIds;
  }

  private record StageChange(RecruitmentStage before, RecruitmentStage after) {}

  private RecruitmentStatus resolveStatusAtCreation(
      LocalDateTime startDate, LocalDateTime endDate, LocalDateTime currentTime) {
    if (endDate != null && !endDate.isAfter(currentTime)) {
      return RecruitmentStatus.CLOSED;
    }
    if (startDate != null && !startDate.isAfter(currentTime)) {
      return RecruitmentStatus.OPEN;
    }
    return RecruitmentStatus.DRAFT;
  }

  private void validateCreatableSchedule(
      LocalDateTime startDate, LocalDateTime endDate, LocalDateTime currentTime) {
    if (endDate != null && !endDate.isAfter(currentTime)) {
      throw new IllegalArgumentException("이미 종료된 기간으로는 채용 공고를 생성할 수 없습니다.");
    }
  }

  private List<RecruitmentStage> toStagesWithRequiredFail(
      List<RecruitmentStageRequest> stageRequests) {
    List<RecruitmentStage> stages =
        stageRequests == null
            ? List.of()
            : stageRequests.stream()
                .map(s -> new RecruitmentStage(null, s.stageName(), s.stageStep(), s.stageType()))
                .toList();

    boolean hasFailStage =
        stages.stream().anyMatch(stage -> stage.stageType() == RecruitmentStageType.FAIL);
    if (hasFailStage) {
      return stages;
    }

    int nextStep = stages.stream().mapToInt(RecruitmentStage::stageStep).max().orElse(0) + 1;
    return Stream.concat(
            stages.stream(),
            Stream.of(new RecruitmentStage(null, "불합격", nextStep, RecruitmentStageType.FAIL)))
        .toList();
  }
}
