package com.coffiness.calfit.core.api.facade.recruitment;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.interview.InterviewReader;
import com.coffiness.calfit.domain.interview.InterviewScheduleCalendarItem;
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
    YearMonth ym = YearMonth.parse(yearMonth);
    LocalDateTime from = ym.atDay(1).atStartOfDay();
    LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59, 999999);

    return interviewReader.getSchedulesByRecruitmentId(recruitmentId, from, to);
  }
}
