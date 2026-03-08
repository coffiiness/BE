package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceReader;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceEntity;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceReaderImpl implements WorkspaceReader {

  private final WorkspaceRepository workspaceRepository;

  @Override
  public List<Workspace> getAllWorkspaces() {
    return workspaceRepository.findAll().stream().map(this::toWorkspace).toList();
  }

  private Workspace toWorkspace(WorkspaceEntity entity) {
    return new Workspace(
        entity.getId(),
        entity.getWorkspaceId(),
        entity.getName(),
        entity.getContactPhone(),
        entity.getEmployeeScale());
  }
}
