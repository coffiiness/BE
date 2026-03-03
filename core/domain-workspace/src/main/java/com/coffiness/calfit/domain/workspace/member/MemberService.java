package com.coffiness.calfit.domain.workspace.member;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.group.GroupReader;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
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
  private final GroupReader groupReader;

  @Transactional
  public Member registerMember(Long userId, MemberType memberType) {
    return memberStore.save(userId, memberType);
  }

  @Transactional(readOnly = true)
  public MemberInfo getMember(String workspaceId, Long userId) {
    UserInfo user = userReader.getUser(userId);
    Member member = memberReader.getMember(workspaceId, userId);

    Map<Long, String> groupNameMap =
            member.groupId() == null
                    ? Map.of()
                    : groupReader.getGroupNameMap(List.of(member.groupId()));

    return toMemberInfo(user, member, groupNameMap);
  }

  @Transactional(readOnly = true)
  public List<MemberInfo> getMembers() {
    List<Member> members = memberReader.getMembers();

    List<Long> userIds = members.stream().map(Member::userId).toList();
    Map<Long, UserInfo> userMap =
            userReader.getUsers(userIds).stream().collect(Collectors.toMap(UserInfo::id, u -> u));

    List<Long> groupIds =
            members.stream().map(Member::groupId).filter(gid -> gid != null).distinct().toList();
    Map<Long, String> groupNameMap =
            groupIds.isEmpty() ? Map.of() : groupReader.getGroupNameMap(groupIds);

    return members.stream()
            .map(member -> toMemberInfo(userMap.get(member.userId()), member, groupNameMap))
            .toList();
  }

  @Transactional
  public void updateMemberType(Long memberId, MemberType memberType) {
    memberStore.updateMemberType(memberId, memberType);
  }

  @Transactional
  public void removeMember(Long userId, Long memberId) {
    Member member = memberReader.getMemberById(memberId);
    if (userId.equals(member.userId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    memberStore.remove(memberId);
  }

  @Transactional
  public void assignGroup(Long memberId, Long groupId) {
    memberStore.assignGroup(memberId, groupId);
  }

  @Transactional
  public void leaveGroup(Long memberId) {
    memberStore.assignGroup(memberId, null);
  }

  private MemberInfo toMemberInfo(UserInfo user, Member member, Map<Long, String> groupNameMap) {
    String groupName = member.groupId() != null ? groupNameMap.get(member.groupId()) : null;
    return new MemberInfo(
            member.id(),
            user.name(),
            user.createdAt(),
            user.recentAt(),
            groupName,
            member.memberType());
  }
}