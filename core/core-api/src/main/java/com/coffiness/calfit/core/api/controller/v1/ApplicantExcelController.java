package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.core.api.service.ApplicantExcelExportService;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicantExcelController {

  private static final String DEFAULT_TENANT = "default";
  private static final String EXCEL_MEDIA_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final String FILE_NAME_PREFIX = "coffiness-applicants-exported-at-";
  private static final ZoneId FILE_NAME_ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter FILE_NAME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final ApplicantExcelExportService applicantExcelExportService;
  private final JdbcTemplate jdbcTemplate;

  @GetMapping({
    "/api/v1/recruitment/applicants/export",
    "/api/v1/recruitment/applicants/excel",
    "/api/v1/recruitments/applicants/export",
    "/api/v1/recruitments/applicants/excel",
    "/api/v1/applicants/export",
    "/api/v1/applicants/excel"
  })
  public ResponseEntity<byte[]> downloadApplicantsExcel(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String workspaceId) {

    String tenantId = resolveTenantId(user, workspaceId);
    String resolvedKeyword = resolveKeyword(keyword, search);

    try {
      TenantContext.setTenantId(tenantId);
      byte[] excelBytes =
          applicantExcelExportService.exportApplicantManagementExcel(resolvedKeyword);
      return buildExcelResponse(excelBytes);
    } finally {
      TenantContext.clear();
    }
  }

  @GetMapping({
    "/api/v1/workspaces/{workspaceId}/applicants/export",
    "/api/v1/workspaces/{workspaceId}/applicants/excel"
  })
  public ResponseEntity<byte[]> downloadApplicantsExcelByWorkspace(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable String workspaceId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String search) {

    assertWorkspaceAccess(user, workspaceId);
    String resolvedKeyword = resolveKeyword(keyword, search);

    try {
      TenantContext.setTenantId(workspaceId);
      byte[] excelBytes =
          applicantExcelExportService.exportApplicantManagementExcel(resolvedKeyword);
      return buildExcelResponse(excelBytes);
    } finally {
      TenantContext.clear();
    }
  }

  private ResponseEntity<byte[]> buildExcelResponse(byte[] excelBytes) {
    String fileName =
        FILE_NAME_PREFIX
            + ZonedDateTime.now(FILE_NAME_ZONE_ID).format(FILE_NAME_FORMATTER)
            + ".xlsx";

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString())
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
        .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
        .contentLength(excelBytes.length)
        .body(excelBytes);
  }

  private String resolveKeyword(String keyword, String search) {
    if (keyword != null && !keyword.isBlank()) {
      return keyword;
    }

    if (search != null && !search.isBlank()) {
      return search;
    }

    return null;
  }

  private String resolveTenantId(SecurityUser user, String requestedWorkspaceId) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    if (isTenantValueUsable(requestedWorkspaceId)) {
      assertWorkspaceAccess(user, requestedWorkspaceId);
      return requestedWorkspaceId;
    }

    String currentTenantId = TenantContext.getTenantId();
    if (isTenantValueUsable(currentTenantId)
        && hasWorkspaceAccess(user.userId(), currentTenantId)) {
      return currentTenantId;
    }

    List<String> tenantIds =
        jdbcTemplate.query(
            """
            SELECT tenant_id
            FROM workspace_members
            WHERE user_id = ?
              AND status = ?
            ORDER BY id DESC
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

  private void assertWorkspaceAccess(SecurityUser user, String workspaceId) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    if (!hasWorkspaceAccess(user.userId(), workspaceId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
  }

  private boolean hasWorkspaceAccess(Long userId, String tenantId) {
    if (!isTenantValueUsable(tenantId)) {
      return false;
    }

    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(1)
            FROM workspace_members
            WHERE user_id = ?
              AND tenant_id = ?
              AND status = ?
            """,
            Integer.class,
            userId,
            tenantId,
            EntityStatus.ACTIVE.name());

    return count != null && count > 0;
  }

  private boolean isTenantValueUsable(String tenantId) {
    return tenantId != null && !tenantId.isBlank() && !DEFAULT_TENANT.equalsIgnoreCase(tenantId);
  }
}
