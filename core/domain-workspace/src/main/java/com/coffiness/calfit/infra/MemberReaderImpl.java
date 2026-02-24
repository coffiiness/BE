package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.member.MemberEntity;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberReaderImpl implements MemberReader {
  private final MemberRepository memberRepository;

  @Override
  public Member getMember(String workspaceId, Long userId) {
    MemberEntity entity = memberRepository.findByTenantIdAndUserId(workspaceId, userId);
    return toWorkspaceMember(entity);
  }

  @Override
  public List<Member> getMembers(String workspaceId) {
    List<MemberEntity> entities =
        memberRepository.findByTenantIdAndStatus(workspaceId, EntityStatus.ACTIVE);
    return entities.stream().map(this::toWorkspaceMember).toList();
  }

  @Override
  public boolean exists(String workspaceId, Long userId) {
    return memberRepository.existsByTenantIdAndUserId(workspaceId, userId);
  }

  private Member toWorkspaceMember(MemberEntity entity) {
    return new Member(
        entity.getId(), entity.getTenantId(), entity.getUserId(), entity.getMemberType());
  }
}
