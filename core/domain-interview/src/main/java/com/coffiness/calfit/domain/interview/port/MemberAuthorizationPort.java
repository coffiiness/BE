package com.coffiness.calfit.domain.interview.port;

// HR 권한 확인 포트
public interface MemberAuthorizationPort {
  boolean isHrMember(String tenantId, Long userId);
}
