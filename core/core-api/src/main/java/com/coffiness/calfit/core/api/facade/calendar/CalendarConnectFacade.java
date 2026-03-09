package com.coffiness.calfit.core.api.facade.calendar;

import com.coffiness.calfit.domain.CalendarConnectService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * 구글 캘린더 연동 요청의 워크스페이스 멤버 검증과 도메인 서비스 호출을 담당
 * */
@Component
@RequiredArgsConstructor
public class CalendarConnectFacade {

  private final MemberReader memberReader;
  private final CalendarConnectService calendarConnectService;

  @Transactional
  public String connectGoogleCalendar(long userId, String authCode, String redirectUri) {
    validateAndGetMember(userId);

    return calendarConnectService.connectGoogleCalendar(authCode, redirectUri, userId);
  }

  @Transactional
  public void syncGoogleCalendar(long userId) {
    Member member = validateAndGetMember(userId);

    calendarConnectService.syncGoogleCalendar(userId, member.id());
  }

  private Member validateAndGetMember(long userId) {
    String currentWorkspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(currentWorkspaceId, userId);

    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버가 아닙니다.");
    }

    return member;
  }
}
