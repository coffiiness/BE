package com.coffiness.calfit.domain.workspace.member;

import java.util.List;

public interface MemberReader {
  /**
   * 멤버 ID로 멤버 정보 조회
   *
   * @param workspaceId 워크스페이스 ID
   * @param userId 사용자 ID
   * @return 멤버 정보 (없으면 null)
   */
  Member getMember(String workspaceId, Long userId);

  /**
   * @param workspaceId 워크스페이스 ID
   * @return 멤버 정보 리스트 (없으면 null)
   */
  List<Member> getMembers(String workspaceId);

  /**
   * 멤버 존재 여부 확인
   *
   * @param workspaceId 워크스페이스 Id
   * @param userId 사용자 ID
   * @return 존재 여부
   */
  boolean exists(String workspaceId, Long userId);
}
