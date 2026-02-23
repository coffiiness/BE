package com.coffiness.calfit.storage.db.core.member;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_members")
public class WorkspaceMemberEntity extends TenantBaseEntity {

  @Column(nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MemberType memberType;

  protected WorkspaceMemberEntity() {}

  private WorkspaceMemberEntity(Long userId, MemberType memberType) {
    this.userId = userId;
    this.memberType = memberType;
  }

  public static WorkspaceMemberEntity create(Long userId, MemberType memberType) {
    return new WorkspaceMemberEntity(userId, memberType);
  }

  public Long getUserId() {
    return userId;
  }

  public MemberType getMemberType() {
    return memberType;
  }
}
