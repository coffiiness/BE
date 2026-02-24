package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberStore;
import com.coffiness.calfit.storage.db.core.member.MemberEntity;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberStoreImpl implements MemberStore {
  private final MemberRepository memberRepository;

  @Override
  public Member save(String workspaceId, Long userId, MemberType memberType) {
    MemberEntity entity = MemberEntity.create(userId, memberType);
    MemberEntity saved = memberRepository.save(entity);

    return new Member(saved.getId(), workspaceId, saved.getUserId(), saved.getMemberType());
  }

  @Override
  public void updateMemberType(Long memberId, MemberType memberType) {
    MemberEntity entity = memberRepository.findById(memberId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.updateMemberType(memberType);
  }

  @Override
  public void remove(Long memberId) {
    MemberEntity entity = memberRepository.findById(memberId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.deleted();
  }
}
