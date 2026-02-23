package com.coffiness.calfit.domain.workspace.member;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.storage.db.core.member.WorkspaceMemberEntity;
import com.coffiness.calfit.storage.db.core.member.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {
  private final WorkspaceMemberRepository memberRepository;

  @Transactional
  public WorkspaceMember registerMember(String workspaceId, Long userId, MemberType memberType) {
    WorkspaceMemberEntity entity = WorkspaceMemberEntity.create(userId, memberType);
    WorkspaceMemberEntity saved = memberRepository.save(entity);

    return new WorkspaceMember(
        saved.getId(), workspaceId, saved.getUserId(), saved.getMemberType());
  }
}
