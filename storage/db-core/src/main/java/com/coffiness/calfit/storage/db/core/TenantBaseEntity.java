package com.coffiness.calfit.storage.db.core;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class TenantBaseEntity extends BaseEntity {

    // 워크스페이스 ID
    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    protected TenantBaseEntity() {
    }

    protected TenantBaseEntity(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

}
