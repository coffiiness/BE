package com.coffiness.calfit.storage.db.core.user;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.UserRole;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity extends TenantBaseEntity {

  // 이메일
  @Column(nullable = false, unique = true)
  private String email;

  // 비밀번호
  @Column(nullable = false)
  private String password;

  // 이름
  @Column(name = "name", length = 50, nullable = false)
  private String name;

  // 사용자 역할
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MemberType type;

  protected UserEntity() {}

  private UserEntity(String email, String password, String name, UserRole role, MemberType type) {
    this.email = email;
    this.password = password;
    this.name = name;
    this.role = role;
    this.type = type;
  }

  public static UserEntity create(String email, String password, String name) {
    return new UserEntity(email, password, name, UserRole.MEMBER, MemberType.HR);
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getName() {
    return name;
  }

  public UserRole getRole() {
    return role;
  }

  public MemberType getType() {
    return type;
  }
}
