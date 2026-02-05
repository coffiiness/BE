package com.coffiness.calfit.storage.db.core.user;

import com.coffiness.calfit.storage.db.core.TenancyEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "user_groups")
public class UserGroupEntity extends TenancyEntity {

    // 이름
    @Column(name = "name", nullable = false)
    private String name;

    // 설명
    @Column(name = "description")
    private String description;

    protected UserGroupEntity() {
        super();
    }

    private UserGroupEntity(Long workspaceId, String name, String description) {
        super(workspaceId);
        this.name = name;
        this.description = description;
    }

    public static UserGroupEntity create(Long workspaceId, String name, String description) {
        return new UserGroupEntity(workspaceId, name, description);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

}