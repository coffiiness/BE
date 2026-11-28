package com.coffiness.calfit.storage.db.core.workspace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, Long> {

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select w from WorkspaceEntity w where w.workspaceId = :workspaceId")
  Optional<WorkspaceEntity> findByWorkspaceIdForUpdate(
      @org.springframework.data.repository.query.Param("workspaceId") String workspaceId);

  Optional<WorkspaceEntity> findByWorkspaceId(String workspaceId);
}
