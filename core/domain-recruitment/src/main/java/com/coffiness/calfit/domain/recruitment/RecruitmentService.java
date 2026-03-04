package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.core.enums.RecruitmentActionType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.interview.InterviewScheduleCalendarItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitmentService {

  private final RecruitmentStore recruitmentStore;
  private final RecruitmentHistoryAppender recruitmentHistoryAppender;
  private final RecruitmentReader recruitmentReader;

  public Long createRecruitment(long userId, RecruitmentCreateRequest request) {

    // TODO : 에러처리 등 각종 검증 로직

    List<RecruitmentStage> stages =
        request.stages() == null
            ? List.of()
            : request.stages().stream()
                .map(s -> new RecruitmentStage(null, s.stageName(), s.stageStep(), s.stageType()))
                .toList();

    /*
     * TODO : 도메인에 정적 생성 메소드를 둘까 vs 지금처럼? vs DTO에?
     * 지금처럼 : 서비스 계층 코드가 어떻게 동작하는지 알기 쉽지만, 서비스 코드가 뚱뚱해보임
     * 도메인 내 정적 메소드 : 반대로 서비스 코드가 간결해지지만, 개발자가 내부를 타고 들어가봐야 알 수 있음
     * DTO : API 계층은 도메인을 쓰기 위해 존재하므로 도메인을 import 해서 쓰는 것이 맞지만 도메인이 API에 의존해서는 되는가?
     */
    Recruitment newRecruitment =
        new Recruitment(
            null,
            userId,
            request.title(),
            request.contents(),
            RecruitmentStatus.DRAFT,
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

    recruitmentHistoryAppender.append(
        saveRecruitment.id(),
        saveRecruitment.creatorId(),
        RecruitmentActionType.RECRUITMENT_CREATED,
        "공고 생성",
        saveRecruitment);

    return saveRecruitment.id();
  }

  @Transactional(readOnly = true)
  public List<RecruitmentListInfo> getRecruitmentList(
      long userId, RecruitmentStatus recruitmentStatus, Pageable pageable) {

    return recruitmentReader.readList(recruitmentStatus, pageable);
  }

  @Transactional(readOnly = true)
  public RecruitmentDetailInfo getRecruitmentDetail(long userId, Long recruitmentId) {

    // TODO : 이 멤버가 이 채용 공고를 볼 권한이 있는지 + 멤버를 매개변수로 받고 user로 변환하는 로직
    return recruitmentReader.readDetail(recruitmentId);
  }

  @Transactional(readOnly = true)
  public List<InterviewScheduleCalendarItem> getInterviewSchedules(
      long userId, Long recruitmentId, String yearMonth) {
    // TODO: domain-interview 모듈의 InterviewReader 가 완성되면 연결
    // return interviewReader.getSchedulesByRecruitmentId(recruitmentId, yearMonth);
    return List.of();
  }
}
