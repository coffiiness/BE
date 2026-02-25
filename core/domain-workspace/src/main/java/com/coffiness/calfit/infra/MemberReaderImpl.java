package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.member.MemberEntity;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
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
  public Member getMemberById(Long memberId) {
    return memberRepository
        .findById(memberId)
        .map(this::toWorkspaceMember)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  @Override
  public List<Member> getMembers() {
    List<MemberEntity> entities = memberRepository.findByStatus(EntityStatus.ACTIVE);
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
