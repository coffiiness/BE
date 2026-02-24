package com.coffiness.calfit.domain.workspace.member;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
  private final MemberReader memberReader;
  private final MemberStore memberStore;

  private final UserReader userReader;

  @Transactional
  public Member registerMember(String workspaceId, Long userId, MemberType memberType) {
    return memberStore.save(workspaceId, userId, memberType);
  }

  @Transactional(readOnly = true)
  public MemberInfo getMember(String workspaceId, Long userId) {
    UserInfo user = userReader.getUser(userId);
    Member member = memberReader.getMember(workspaceId, userId);
    return toMemberInfo(user, member);
  }

  @Transactional(readOnly = true)
  public List<MemberInfo> getMembers(String workspaceId) {
    List<Member> members = memberReader.getMembers(workspaceId);

    List<Long> userIds = members.stream().map(Member::userId).toList();
    Map<Long, UserInfo> userMap =
        userReader.getUsers(userIds).stream().collect(Collectors.toMap(UserInfo::id, u -> u));

    return members.stream()
        .map(member -> toMemberInfo(userMap.get(member.userId()), member))
        .toList();
  }

  @Transactional
  public void updateMemberType(Long memberId, MemberType memberType) {
    memberStore.updateMemberType(memberId, memberType);
  }

  @Transactional
  public void removeMember(Long memberId) {
    memberStore.remove(memberId);
  }

  private MemberInfo toMemberInfo(UserInfo user, Member member) {
    return new MemberInfo(
        member.id(), user.name(), user.createdAt(), user.recentAt(), "group", member.memberType());
  }
}
