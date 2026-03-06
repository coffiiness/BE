package com.coffiness.calfit.core.api.facade.calendar;

import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleInfo;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleFacade {

  private final MemberReader memberReader;
  private final ScheduleService scheduleService;

  private Member validateAndGetMember(long userId) {
    String currentWorkspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(currentWorkspaceId, userId);
    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버가 아닙니다.");
    }
    return member;
  }

  @Transactional
  public void createSchedule(long userId, ScheduleCreateRequest request) {
    Member member = validateAndGetMember(userId);

    scheduleService.createSchedule(member.id(), request);
  }

  @Transactional(readOnly = true)
  public List<ScheduleInfo> getSchedules(
      long userId, LocalDateTime startDate, LocalDateTime endDate) {
    Member member = validateAndGetMember(userId);

    return scheduleService.getSchedules(member.id(), startDate, endDate);
  }

  @Transactional(readOnly = true)
  public ScheduleDetailInfo getDetailSchedule(long userId, Long scheduleId) {
    Member member = validateAndGetMember(userId);

    return scheduleService.getDetailSchedule(member.id(), scheduleId);
  }

  @Transactional
  public ScheduleDetailInfo updateSchedule(
      long userId, Long scheduleId, ScheduleUpdateRequest request) {
    Member member = validateAndGetMember(userId);

    return scheduleService.updateSchedule(member.id(), scheduleId, request);
  }

  @Transactional
  public void deleteSchedule(long userId, Long scheduleId) {
    Member member = validateAndGetMember(userId);

    scheduleService.deleteSchedule(member.id(), scheduleId);
  }
}
