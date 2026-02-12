package com.coffiness.calfit.storage.db.core.user;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "user_groups")
public class GroupEntity extends TenantBaseEntity {

    // 이름
    @Column(name = "name", nullable = false)
    private String name;

    // 설명
    @Column(name = "description")
    private String description;

    protected GroupEntity() {
        super();
    }

    private GroupEntity(String name, String description) {
        this.name = name;
        this.description = description;
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