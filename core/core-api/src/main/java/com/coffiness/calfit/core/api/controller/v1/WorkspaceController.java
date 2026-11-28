package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.CreateWorkspaceRequest;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.api.facade.workspace.WorkspaceCreationFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WorkspaceController {
  private final WorkspaceCreationFacade workspaceCreationFacade;

  @PostMapping("/api/v1/workspaces")
  public ApiResponse<WorkspaceResponse> createWorkspace(
      @Valid @RequestBody CreateWorkspaceRequest request,
      @AuthenticationPrincipal SecurityUser securityUser) {

    Workspace workspace =
        workspaceCreationFacade.create(
            securityUser.userId(), request.name(), request.contactPhone(), request.employeeScale());

    return ApiResponse.success(WorkspaceResponse.from(workspace));
  }
}
