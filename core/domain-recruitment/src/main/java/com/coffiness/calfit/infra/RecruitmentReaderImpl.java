package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.interview.InterviewerInfo;
import com.coffiness.calfit.domain.recruitment.*;
import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.group.GroupReader;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.recruitment.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentReaderImpl implements RecruitmentReader {

  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final RecruitmentInterviewerRepository recruitmentInterviewerRepository;
  private final RecruitmentReferenceGroupRepository recruitmentReferenceGroupRepository;

  private final UserReader userReader;
  private final GroupReader groupReader;
  private final MemberReader memberReader;

  @Override
  public List<RecruitmentListInfo> readList(
      RecruitmentStatus recruitmentStatus, Pageable pageable) {
    Page<RecruitmentEntity> recruitments;

    if (recruitmentStatus == null) {
      recruitments = recruitmentRepository.findByStatus(EntityStatus.ACTIVE, pageable);
    } else {
      recruitments =
          recruitmentRepository.findByRecruitmentStatusAndStatus(
              recruitmentStatus, EntityStatus.ACTIVE, pageable);
    }

    if (recruitments.isEmpty()) {}

    List<Long> recruitmentIds = recruitments.stream().map(RecruitmentEntity::getId).toList();

    // 단계
    Map<Long, List<RecruitmentStageEntity>> stageMap =
        recruitmentStageRepository.findByRecruitmentIdIn(recruitmentIds).stream()
            .collect(Collectors.groupingBy(RecruitmentStageEntity::getRecruitmentId));

    // 면접관
    List<RecruitmentInterviewerEntity> allInterviewers =
        recruitmentInterviewerRepository.findByRecruitmentIdIn(recruitmentIds);
    Map<Long, List<RecruitmentInterviewerEntity>> interviewerMap =
        allInterviewers.stream()
            .collect(Collectors.groupingBy(RecruitmentInterviewerEntity::getRecruitmentId));

    // 외부 유저 도메인 정보 가져오기
    List<Long> memberIds =
        allInterviewers.stream()
            .map(RecruitmentInterviewerEntity::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<Long, String> userNameMap = getInterviewerNameMap(memberIds);

    // 그룹 도메인 정보 외부에서 가져오기
    List<Long> leadGroupIds =
        recruitments.stream()
            .map(RecruitmentEntity::getLeadGroupId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    Map<Long, String> groupNameMap =
        leadGroupIds.isEmpty() ? Map.of() : groupReader.getGroupNameMap(leadGroupIds);

    return recruitments.stream()
        .map(
            entity -> {
              Long recruitmentId = entity.getId();

              String groupName =
                  entity.getLeadGroupId() != null
                      ? groupNameMap.getOrDefault(entity.getLeadGroupId(), "부서 미지정")
                      : "부서 미지정";

              // TODO : 현재 단계의 지원자 수 가져오기 추가 (0 대신)
              List<RecruitmentListInfo.StageSummaryInfo> stageInfos =
                  stageMap.getOrDefault(recruitmentId, List.of()).stream()
                      .sorted(Comparator.comparing(RecruitmentStageEntity::getStageStep))
                      .map(
                          s ->
                              new RecruitmentListInfo.StageSummaryInfo(
                                  s.getId(), s.getStageName(), 0))
                      .toList();

              List<RecruitmentListInfo.AssigneeSummaryInfo> assigneeInfos =
                  interviewerMap.getOrDefault(recruitmentId, List.of()).stream()
                      .map(
                          i -> {
                            Long memberId = i.getUserId();
                            String name = userNameMap.getOrDefault(memberId, "알 수 없는 사용자");
                            return new RecruitmentListInfo.AssigneeSummaryInfo(memberId, name);
                          })
                      .filter(assignee -> assignee.userId() != null)
                      .toList();

              return new RecruitmentListInfo(
                  recruitmentId,
                  entity.getTitle(),
                  groupName,
                  entity.getCareerType(),
                  entity.getMinExperienceYears(),
                  entity.getMaxExperienceYears(),
                  entity.getStartDate(),
                  entity.getEndDate(),
                  entity.getRecruitmentStatus(),
                  stageInfos,
                  assigneeInfos);
            })
        .toList();
  }

  @Override
  public RecruitmentDetailInfo readDetail(Long recruitmentId) {
    // TODO: 에러 처리 (CoreException) 추후 삽입
    RecruitmentEntity entity =
        recruitmentRepository
            .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채용 공고입니다."));

    // Group Name 가져오기
    String groupName = "부서 미지정";
    if (entity.getLeadGroupId() != null) {
      Map<Long, String> groupNameMap =
          groupReader.getGroupNameMap(List.of(entity.getLeadGroupId()));
      groupName = groupNameMap.getOrDefault(entity.getLeadGroupId(), "부서 미지정");
    }

    // 면접관 정보 매핑
    List<RecruitmentInterviewerEntity> interviewers =
        recruitmentInterviewerRepository.findByRecruitmentId(recruitmentId);
    List<Long> memberIds =
        interviewers.stream()
            .map(RecruitmentInterviewerEntity::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    Map<Long, String> userNameMap = getInterviewerNameMap(memberIds);

    List<InterviewerInfo> interviewerInfos =
        interviewers.stream()
            .filter(i -> i.getUserId() != null)
            .map(
                i -> {
                  Long memberId = i.getUserId();
                  String name = userNameMap.getOrDefault(memberId, "알 수 없는 사용자");
                  // 체크박스는 우선 true
                  return new InterviewerInfo(memberId, name, true);
                })
            .filter(info -> info.userId() != null)
            .toList();

    // D-Day 및 링크 URL
    int dDay =
        (int)
            java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), entity.getEndDate().toLocalDate());
    String shareUrl = "https://careers.nexus.ai/jobs/" + recruitmentId;

    return new RecruitmentDetailInfo(
        entity.getId(),
        entity.getTitle(),
        groupName,
        entity.getCareerType(),
        entity.getMinExperienceYears(),
        entity.getMaxExperienceYears(),
        entity.getStartDate(),
        entity.getEndDate(),
        0, // TODO: 지원자 모듈 완성 시 연동
        0, // TODO: processingInterview
        dDay,
        shareUrl,
        entity.getRecruitmentStatus(),
        interviewerInfos);
  }

  @Override
  public Recruitment readById(Long recruitmentId) {
    RecruitmentEntity entity =
        recruitmentRepository
            .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채용 공고입니다."));

    List<RecruitmentStage> stages =
        recruitmentStageRepository.findByRecruitmentId(recruitmentId).stream()
            .map(
                s ->
                    new RecruitmentStage(
                        s.getId(), s.getStageName(), s.getStageStep(), s.getStageType()))
            .toList();

    List<Long> interviewerIds =
        recruitmentInterviewerRepository.findByRecruitmentId(recruitmentId).stream()
            .map(RecruitmentInterviewerEntity::getUserId)
            .filter(Objects::nonNull)
            .toList();

    List<Long> referenceGroupIds =
        recruitmentReferenceGroupRepository.findByRecruitmentId(recruitmentId).stream()
            .map(RecruitmentReferenceGroupEntity::getGroupId)
            .filter(Objects::nonNull)
            .toList();

    return new Recruitment(
        entity.getId(),
        entity.getCreatorId(),
        entity.getTitle(),
        entity.getContents(),
        entity.getRecruitmentStatus(),
        entity.getTargetCount(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getApplicationTemplateId(),
        entity.getCareerType(),
        entity.getMinExperienceYears(),
        entity.getMaxExperienceYears(),
        entity.getLeadGroupId(),
        referenceGroupIds,
        interviewerIds,
        stages);
  }

  @Override
  public Map<String, Long> countOpenRecruitmentsByTenant() {
    List<Object[]> counts = recruitmentRepository.countOpenRecruitmentsByTenant();
    return counts.stream()
        .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));
  }

  @Override
  public List<OpenRecruitmentInfo> readOpenByTenantId(String tenantId) {
    return recruitmentRepository.findOpenRecruitmentsByTenantId(tenantId).stream()
        .map(
            e ->
                new OpenRecruitmentInfo(
                    e.getId(),
                    e.getTitle(),
                    e.getContents(),
                    e.getCareerType(),
                    e.getTargetCount(),
                    e.getMinExperienceYears(),
                    e.getMaxExperienceYears(),
                    e.getStartDate(),
                    e.getEndDate()))
        .toList();
  }

  @Override
  public List<OpenRecruitmentInfo> readOpenByTenantIdAndSearch(String tenantId, String search) {
    return recruitmentRepository.findOpenRecruitmentsByTenantIdAndSearch(tenantId, search).stream()
        .map(
            e ->
                new OpenRecruitmentInfo(
                    e.getId(),
                    e.getTitle(),
                    e.getContents(),
                    e.getCareerType(),
                    e.getTargetCount(),
                    e.getMinExperienceYears(),
                    e.getMaxExperienceYears(),
                    e.getStartDate(),
                    e.getEndDate()))
        .toList();
  }

  private Map<Long, String> getUserNameMap(List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }

    return userReader.getUsers(userIds).stream()
        .collect(Collectors.toMap(UserInfo::id, UserInfo::name, (left, right) -> left));
  }

  private Map<Long, String> getInterviewerNameMap(List<Long> memberIds) {
    if (memberIds == null || memberIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, Member> memberMap =
        memberReader.getMembersByIds(memberIds).stream()
            .collect(Collectors.toMap(Member::id, member -> member, (left, right) -> left));

    List<Long> userIds =
        memberMap.values().stream()
            .map(Member::userId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    Map<Long, String> userNameMap = getUserNameMap(userIds);

    return memberMap.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> userNameMap.getOrDefault(entry.getValue().userId(), "알 수 없는 사용자"),
                (left, right) -> left));
  }
}
