package com.coffiness.calfit.domain.workspace.group;

import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.domain.workspace.member.MemberStore;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {
  private final GroupReader groupReader;
  private final GroupStore groupStore;
  private final MemberReader memberReader;
  private final MemberStore memberStore;

  @Transactional(readOnly = true)
  public List<GroupInfo> getGroups() {
    List<Group> groups = groupReader.getGroups();
    if (groups.isEmpty()) return List.of();

    List<Long> groupIds = groups.stream().map(Group::id).toList();
    Map<Long, Long> countMap = memberReader.getGroupMemberCountMap(groupIds);

    return groups.stream()
        .map(g -> new GroupInfo(g.id(), g.name(), g.color(), countMap.getOrDefault(g.id(), 0L)))
        .toList();
  }

  @Transactional(readOnly = true)
  public GroupInfo getGroup(Long groupId) {
    Group group = groupReader.getGroup(groupId);
    Map<Long, Long> countMap = memberReader.getGroupMemberCountMap(List.of(groupId));
    return new GroupInfo(
        group.id(), group.name(), group.color(), countMap.getOrDefault(groupId, 0L));
  }

  @Transactional
  public GroupInfo createGroup(String name, String color) {
    if (groupReader.existsByName(name)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    Group group = groupStore.save(name, color);
    return new GroupInfo(group.id(), group.name(), group.color(), 0L);
  }

  @Transactional
  public void updateGroup(Long groupId, String name, String color) {
    groupStore.updateInfo(groupId, name, color);
  }

  @Transactional
  public void removeGroup(Long groupId) {
    memberStore.clearGroupFromMembers(groupId);
    groupStore.remove(groupId);
  }
}
