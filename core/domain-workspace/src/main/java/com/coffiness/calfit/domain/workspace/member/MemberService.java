package com.coffiness.calfit.domain.workspace.member;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.member.ActiveMembershipPolicy;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
  private final MemberReader memberReader;
  private final MemberStore memberStore;
  private final ActiveMembershipPolicy membershipPolicy;
  private final MemberDepartureHandler departureHandler;

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Member registerMember(Long userId, MemberType memberType) {
    return memberStore.save(userId, memberType);
  }

  @Transactional(readOnly = true)
  public Member getMember(Long userId) {
    String workspaceId = TenantContext.getTenantId();
    return memberReader.getMember(workspaceId, userId);
  }

  @Transactional(readOnly = true)
  public List<Member> getMembers() {
    return memberReader.getMembers();
  }

  @Transactional
  public void updateMemberType(Long memberId, MemberType memberType) {
    membershipPolicy.lockCurrentWorkspace();
    Member member = memberReader.getMemberById(memberId);
    membershipPolicy.lockUser(member.userId());
    if (memberType != MemberType.HR) ensureHrCanLeave(member);
    memberStore.updateMemberType(memberId, memberType);
  }

  @Transactional
  public void removeMember(Long userId, Long memberId) {
    membershipPolicy.lockCurrentWorkspace();
    Member member = memberReader.getMemberById(memberId);
    if (userId.equals(member.userId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    membershipPolicy.lockUser(member.userId());
    ensureHrCanLeave(member);
    departureHandler.disconnectUser(member.userId());
    memberStore.remove(memberId);
  }

  @Transactional
  public void leaveWorkspace(Long userId) {
    membershipPolicy.lockCurrentWorkspace();
    membershipPolicy.lockUser(userId);
    String workspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(workspaceId, userId);
    ensureHrCanLeave(member);
    departureHandler.disconnectUser(userId);
    memberStore.remove(member.id());
  }

  private void ensureHrCanLeave(Member member) {
    if (member.memberType() == MemberType.HR) {
      long hrCount = memberReader.countActiveHrMembers(TenantContext.getTenantId());
      if (hrCount <= 1) {
        throw new CoreException(
            ErrorType.VALIDATION_ERROR,
            "LAST_HR_CANNOT_LEAVE",
            "워크스페이스에 최소 1명의 인사담당자가 있어야 합니다. 다른 멤버에게 인사담당자 권한을 부여한 후 탈퇴해주세요.");
      }
    }
  }

  @Transactional
  public void assignGroup(Long memberId, Long groupId) {
    memberStore.assignGroup(memberId, groupId);
  }

  @Transactional
  public void leaveGroup(Long memberId) {
    memberStore.assignGroup(memberId, null);
  }

  @Transactional(readOnly = true)
  public void validateHrRole(Long userId) {
    String workspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(workspaceId, userId);
    if (member.memberType() != MemberType.HR) {
      throw new CoreException(ErrorType.FORBIDDEN);
    }
  }

  @Transactional(readOnly = true)
  public Map<Long, Long> getGroupMemberCountMap(List<Long> groupIds) {
    return memberReader.getGroupMemberCountMap(groupIds);
  }

  @Transactional
  public void clearGroupFromMembers(Long groupId) {
    memberStore.clearGroupFromMembers(groupId);
  }
}
