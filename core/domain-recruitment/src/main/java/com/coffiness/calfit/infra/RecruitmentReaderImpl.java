package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.interview.InterviewerInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentDetailInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentListInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentReader;
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

  private final UserReader userReader;
  private final GroupReader groupReader;
  private final MemberReader memberReader;

  @Override
  public List<RecruitmentListInfo> readList(
      RecruitmentStatus recruitmentStatus, Pageable pageable) {
    Page<RecruitmentEntity> recruitments;

    if (recruitmentStatus == null) {
      recruitments = recruitmentRepository.findAll(pageable);
    } else {
      recruitments = recruitmentRepository.findByRecruitmentStatus(recruitmentStatus, pageable);
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

    // member와 user 맵핑 로직
    List<Long> memberIds =
        allInterviewers.stream()
            .map(RecruitmentInterviewerEntity::getMemberId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    Map<Long, Long> memberIdToUserIdMap =
        memberReader.getMembersByIds(memberIds).stream()
            .filter(m -> m.userId() != null)
            .collect(Collectors.toMap(Member::id, Member::userId));

    // 외부 유저 도메인 정보 가져오기
    List<Long> userIds = memberIdToUserIdMap.values().stream().distinct().toList();
    Map<Long, String> userNameMap =
        userReader.getUsers(userIds).stream()
            .collect(Collectors.toMap(UserInfo::id, UserInfo::name));

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
                            Long memberId = i.getMemberId(); // 원본 Member ID
                            Long userId = memberIdToUserIdMap.get(memberId); // 매핑된 User ID (이름 찾기용)
                            String name =
                                userId != null
                                    ? userNameMap.getOrDefault(userId, "알 수 없는 사용자")
                                    : "알 수 없는 사용자";

                            return new RecruitmentListInfo.AssigneeSummaryInfo(memberId, name);
                          })
                      .toList();

              return new RecruitmentListInfo(
                  recruitmentId,
                  entity.getTitle(),
                  groupName,
                  entity.getCareerType(),
                  entity.getMinExperienceYears(),
                  entity.getMaxExperienceYears(),
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
            .findById(recruitmentId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채용 공고입니다."));

    // Group Name  가져오기
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
            .map(RecruitmentInterviewerEntity::getMemberId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    Map<Long, Long> memberIdToUserIdMap =
        memberReader.getMembersByIds(memberIds).stream()
            .filter(m -> m.userId() != null)
            .collect(Collectors.toMap(Member::id, Member::userId));

    List<Long> userIds = memberIdToUserIdMap.values().stream().distinct().toList();
    Map<Long, String> userNameMap =
        userReader.getUsers(userIds).stream()
            .collect(Collectors.toMap(UserInfo::id, UserInfo::name));

    List<InterviewerInfo> interviewerInfos =
        interviewers.stream()
            .filter(i -> i.getMemberId() != null)
            .map(
                i -> {
                  Long memberId = i.getMemberId();
                  Long userId = memberIdToUserIdMap.get(memberId);
                  String name =
                      userId != null
                          ? userNameMap.getOrDefault(userId, "알 수 없는 사용자")
                          : "알 수 없는 사용자";
                  // 체크박스는 우선 true
                  return new InterviewerInfo(memberId, name, true);
                })
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
}
