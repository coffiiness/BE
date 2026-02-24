package com.coffiness.calfit.domain.workspace.member;

import com.coffiness.calfit.core.enums.MemberType;

public interface MemberStore {

  /**
   * 멤버 저장
   *
   * @param workspaceId 워크스페이스ID
   * @param userId 사용자 ID
   * @param memberType 멤버 타입
   * @return Member 리턴
   */
  Member save(String workspaceId, Long userId, MemberType memberType);

  /**
   * 멤버 타입 수정
   *
   * @param memberId 멤버 ID (MemberEntity PK)
   * @param memberType 멤버 타입
   */
  void updateMemberType(Long memberId, MemberType memberType);

  /**
   * 멤버 삭제
   *
   * @param memberId 멤버 ID (MemberEntity PK)
   */
  void remove(Long memberId);
}
