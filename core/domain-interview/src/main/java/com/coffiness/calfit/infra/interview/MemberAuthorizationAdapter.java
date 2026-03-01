package com.coffiness.calfit.infra.interview;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.interview.port.MemberAuthorizationPort;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAuthorizationAdapter implements MemberAuthorizationPort {

  private final MemberReader memberReader;

  @Override
  public boolean isHrMember(String tenantId, Long userId) {
    Member member = memberReader.getMember(tenantId, userId);
    return member != null && member.memberType() == MemberType.HR;
  }
}
