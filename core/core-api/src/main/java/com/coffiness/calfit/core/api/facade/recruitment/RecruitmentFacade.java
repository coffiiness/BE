package com.coffiness.calfit.core.api.facade.recruitment;

import com.coffiness.calfit.api.v1.request.RecruitmentUpdateRequest;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.interview.InterviewReader;
import com.coffiness.calfit.domain.interview.InterviewScheduleCalendarItem;
import com.coffiness.calfit.domain.recruitment.RecruitmentDetailInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentReader;
import com.coffiness.calfit.domain.recruitment.RecruitmentService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RecruitmentFacade {

  private final InterviewReader interviewReader;
  private final MemberReader memberReader;
  private final RecruitmentService recruitmentService;
  private final RecruitmentReader recruitmentReader;

  @Transactional(readOnly = true)
  public List<InterviewScheduleCalendarItem> getInterviewSchedule(
      long userId, Long recruitmentId, String yearMonth) {

    // Member 정보 확인
    String currentWorkspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(currentWorkspaceId, userId);

    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버가 아닙니다.");
    }

    // 권한 검증: HR이 아닌 경우에만 면접관 여부 확인
    if (member.memberType() != MemberType.HR) {
      recruitmentService.assertCanAccess(userId, recruitmentId);
    }

    // 기간 파싱 및 면접 일정 조회
    YearMonth ym;
    try {
      ym = YearMonth.parse(yearMonth);
    } catch (java.time.format.DateTimeParseException e) {
      throw new IllegalArgumentException("잘못된 연월 형식입니다. (예: 2026-03)");
    }
    LocalDateTime from = ym.atDay(1).atStartOfDay();
    LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59, 999999);

    return interviewReader.getSchedulesByRecruitmentId(recruitmentId, from, to);
  }

  @Transactional
  public RecruitmentDetailInfo updateRecruitment(
      long userId, Long recruitmentId, RecruitmentUpdateRequest request) {
    // Member 정보 확인
    String currentWorkspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(currentWorkspaceId, userId);

    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버가 아닙니다.");
    }

    if (member.memberType() != MemberType.HR) {
      throw new IllegalArgumentException("채용 담당자만 수정이 가능합니다.");
    }

    recruitmentService.updateRecruitment(userId, recruitmentId, request);

    return recruitmentReader.readDetail(recruitmentId);
  }

  public void deleteRecruitment(long userId, Long recruitmentId) {
    String currentWorkspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(currentWorkspaceId, userId);

    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버가 아닙니다.");
    }

    if (member.memberType() != MemberType.HR) {
      throw new IllegalArgumentException("채용 담당자만 채용 공고를 삭제할 수 있습니다.");
    }

    // TODO : 지원자나 진행중인 면접이 있을 경우의 검증 로직 (지원자 모듈, 면접 모듈)

    recruitmentService.deleteRecruitment(userId, recruitmentId);
  }
}
