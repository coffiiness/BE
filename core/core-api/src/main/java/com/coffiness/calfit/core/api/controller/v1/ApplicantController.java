package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.core.api.dto.v1.response.ApplicantManagementItemResponse;
import com.coffiness.calfit.core.api.service.ApplicantApplicationService;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicantController {

  private static final String DEFAULT_TENANT = "default";

  private final ApplicantApplicationService applicantApplicationService;
  private final JdbcTemplate jdbcTemplate;

  @GetMapping("/api/v1/applicants")
  public ApiResponse<List<ApplicantManagementItemResponse>> getApplicants(
      @AuthenticationPrincipal SecurityUser user, @RequestParam(required = false) String keyword) {

    String tenantId = resolveTenantId(user);

    try {
      TenantContext.setTenantId(tenantId);
      List<ApplicantManagementItemResponse> items =
          applicantApplicationService.getApplicantManagementItems(keyword);
      return ApiResponse.success(items);
    } finally {
      TenantContext.clear();
    }
  }

  @GetMapping("/api/v1/workspaces/{workspaceId}/applicants")
  public ApiResponse<List<ApplicantManagementItemResponse>> getApplicantsByWorkspace(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable String workspaceId,
      @RequestParam(required = false) String keyword) {

    try {
      TenantContext.setTenantId(workspaceId);
      List<ApplicantManagementItemResponse> items =
          applicantApplicationService.getApplicantManagementItems(keyword);
      return ApiResponse.success(items);
    } finally {
      TenantContext.clear();
    }
  }

  private String resolveTenantId(SecurityUser user) {
    String currentTenantId = TenantContext.getTenantId();
    if (currentTenantId != null
        && !currentTenantId.isBlank()
        && !DEFAULT_TENANT.equalsIgnoreCase(currentTenantId)) {
      return currentTenantId;
    }

    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    List<String> tenantIds =
        jdbcTemplate.query(
            """
            SELECT tenant_id
            FROM workspace_members
            WHERE user_id = ?
              AND status = ?
            ORDER BY id ASC
            LIMIT 1
            """,
            (resultSet, rowNum) -> resultSet.getString("tenant_id"),
            user.userId(),
            EntityStatus.ACTIVE.name());

    if (tenantIds.isEmpty()) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }

    return tenantIds.get(0);
  }
}
