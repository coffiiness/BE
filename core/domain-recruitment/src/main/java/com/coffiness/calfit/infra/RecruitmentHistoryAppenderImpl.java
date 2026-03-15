package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.RecruitmentActionType;
import com.coffiness.calfit.domain.recruitment.RecruitmentHistoryAppender;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentHistoryEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentHistoryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentHistoryAppenderImpl implements RecruitmentHistoryAppender {

  private static final String STAGE_SCOPE = "STAGE";

  private final RecruitmentHistoryRepository recruitmentHistoryRepository;

  // 채용 공고 이력 적재
  @Override
  public void append(
      Long recruitmentId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      String scope,
      Map<String, Object> beforeChangeLog,
      Map<String, Object> afterChangeLog,
      List<String> changedFields) {
    saveHistory(
        recruitmentId,
        null,
        actorId,
        actionType,
        reason,
        buildChangeLog(scope, beforeChangeLog, afterChangeLog, changedFields));
  }

  // 채용 단계 이력 적재
  @Override
  public void appendStage(
      Long recruitmentId,
      Long stageId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      Map<String, Object> beforeChangeLog,
      Map<String, Object> afterChangeLog,
      List<String> changedFields) {
    saveHistory(
        recruitmentId,
        stageId,
        actorId,
        actionType,
        reason,
        buildChangeLog(STAGE_SCOPE, beforeChangeLog, afterChangeLog, changedFields));
  }

  // 표준 채용 이력 변경 로그 생성
  private Map<String, Object> buildChangeLog(
      String scope,
      Map<String, Object> beforeChangeLog,
      Map<String, Object> afterChangeLog,
      List<String> changedFields) {
    Map<String, Object> changeLog = new LinkedHashMap<>();
    changeLog.put("scope", scope);
    changeLog.put("before", beforeChangeLog);
    changeLog.put("after", afterChangeLog);
    changeLog.put("changedFields", changedFields == null ? List.of() : List.copyOf(changedFields));
    return changeLog;
  }

  // 채용 이력 저장
  private void saveHistory(
      Long recruitmentId,
      Long stageId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      Map<String, Object> changeLog) {
    recruitmentHistoryRepository.save(
        RecruitmentHistoryEntity.builder()
            .actorId(actorId)
            .recruitmentId(recruitmentId)
            .reason(reason)
            .recruitmentActionType(actionType)
            .changeLog(changeLog)
            .stageId(stageId)
            .build());
  }
}
