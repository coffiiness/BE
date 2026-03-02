package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitmentService {

  private final RecruitmentStore recruitmentStore;

  public Long createRecruitment(long userId, RecruitmentCreateRequest request) {

    List<RecruitmentStage> stages =
        request.stages() == null
            ? List.of()
            : request.stages().stream()
                .map(s -> new RecruitmentStage(null, s.stageName(), s.stageStep(), s.stageType()))
                .toList();

    /*
     * TODO : 도메인에 정적 생성 메소드를 둘까 vs 지금처럼? vs DTO에?
     *  지금처럼 : 서비스 계층 코드가 어떻게 동작하는지 알기 쉽지만, 서비스 코드가 뚱뚱해보임
     *  도메인 내 정적 메소드 : 반대로 서비스 코드가 간결해지지만, 개발자가 내부를 타고 들어가봐야 알 수 있음
     *  DTO : API 계층은 도메인을 쓰기 위해 존재하므로 도메인을 import 해서 쓰는 것이 맞지만 도메인이 API에 의존해서는 되는가?
     * */
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

    recruitmentStore.store(newRecruitment);

    return newRecruitment.id();
  }
}
