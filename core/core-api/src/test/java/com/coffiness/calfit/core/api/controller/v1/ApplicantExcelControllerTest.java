package com.coffiness.calfit.core.api.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.coffiness.calfit.core.api.service.ApplicantExcelExportService;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class ApplicantExcelControllerTest {

  @Mock private ApplicantExcelExportService applicantExcelExportService;

  @Mock private JdbcTemplate jdbcTemplate;

  @InjectMocks private ApplicantExcelController applicantExcelController;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void downloadApplicantsExcel_usesSearchWhenKeywordMissing() {
    SecurityUser user = new SecurityUser(1L, "user@test.com", "USER");
    byte[] excelBytes = "excel".getBytes();

    given(
            jdbcTemplate.query(
                anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(1L),
                eq(EntityStatus.ACTIVE.name())))
        .willReturn(List.of("workspace-1"));
    given(applicantExcelExportService.exportApplicantManagementExcel("test-keyword"))
        .willReturn(excelBytes);

    var response =
        applicantExcelController.downloadApplicantsExcel(user, null, "test-keyword", null);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getHeaders().getContentDisposition().getFilename())
        .startsWith("coffiness-applicants-exported-at-")
        .endsWith(".xlsx");
    assertThat(response.getBody()).isEqualTo(excelBytes);
  }

  @Test
  void downloadApplicantsExcel_withWorkspaceQuery_usesRequestedWorkspace() {
    SecurityUser user = new SecurityUser(1L, "user@test.com", "USER");
    byte[] excelBytes = "excel".getBytes();

    given(
            jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                eq(1L),
                eq("workspace-2"),
                eq(EntityStatus.ACTIVE.name())))
        .willReturn(1);
    given(applicantExcelExportService.exportApplicantManagementExcel(null)).willReturn(excelBytes);

    var response =
        applicantExcelController.downloadApplicantsExcel(user, null, null, "workspace-2");

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isEqualTo(excelBytes);
  }

  @Test
  void downloadApplicantsExcelByWorkspace_withoutUser_throwsUnauthorized() {
    assertThatThrownBy(
            () ->
                applicantExcelController.downloadApplicantsExcelByWorkspace(
                    null, "workspace-1", null, "keyword"))
        .isInstanceOf(CoreException.class)
        .extracting("errorType")
        .isEqualTo(ErrorType.UNAUTHORIZED);
  }

  @Test
  void downloadApplicantsExcelByWorkspace_withoutMembership_throwsUnauthorized() {
    SecurityUser user = new SecurityUser(1L, "user@test.com", "USER");

    given(
            jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                eq(1L),
                eq("workspace-1"),
                eq(EntityStatus.ACTIVE.name())))
        .willReturn(0);

    assertThatThrownBy(
            () ->
                applicantExcelController.downloadApplicantsExcelByWorkspace(
                    user, "workspace-1", null, "keyword"))
        .isInstanceOf(CoreException.class)
        .extracting("errorType")
        .isEqualTo(ErrorType.UNAUTHORIZED);
  }
}
