package com.coffiness.calfit.core.api.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.coffiness.calfit.core.api.dto.v1.response.ApplicantDetailResponse;
import com.coffiness.calfit.core.api.dto.v1.response.ApplicantManagementItemResponse;
import com.coffiness.calfit.core.api.service.ApplicantApplicationService;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ApplicantControllerTest {

  @Mock private ApplicantApplicationService applicantApplicationService;

  @Mock private JdbcTemplate jdbcTemplate;

  @InjectMocks private ApplicantController applicantController;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void getApplicants_withWorkspaceIdQuery_usesSearchKeywordAndReturnsItems() {
    SecurityUser user = new SecurityUser(1L, "user@test.com", "USER");
    ApplicantManagementItemResponse row =
        new ApplicantManagementItemResponse(
            1L, 1L, "tester", "tester@test.com", 1L, "backend", "SUBMITTED", null, null);

    given(
            jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                eq(1L),
                eq("workspace-1"),
                eq(EntityStatus.ACTIVE.name())))
        .willReturn(1);
    given(applicantApplicationService.getApplicantManagementItems("tester"))
        .willReturn(List.of(row));

    ApiResponse<List<ApplicantManagementItemResponse>> response =
        applicantController.getApplicants(user, null, "tester", "workspace-1");

    assertThat(response.getData()).hasSize(1);
    assertThat(response.getData().get(0).applicantEmail()).isEqualTo("tester@test.com");
  }

  @Test
  void getApplicants_withWorkspaceIdQueryWithoutMembership_throwsUnauthorized() {
    SecurityUser user = new SecurityUser(1L, "user@test.com", "USER");

    given(
            jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                eq(1L),
                eq("workspace-1"),
                eq(EntityStatus.ACTIVE.name())))
        .willReturn(0);

    assertThatThrownBy(() -> applicantController.getApplicants(user, null, "tester", "workspace-1"))
        .isInstanceOf(CoreException.class)
        .extracting("errorType")
        .isEqualTo(ErrorType.UNAUTHORIZED);
  }

  @Test
  void getApplicantDetail_withWorkspaceIdQuery_returnsDetail() {
    SecurityUser user = new SecurityUser(1L, "user@test.com", "USER");
    ApplicantDetailResponse detail =
        new ApplicantDetailResponse(
            11L,
            7L,
            3L,
            "backend",
            "SUBMITTED",
            "tester",
            "MALE",
            LocalDate.of(2000, 1, 1),
            "010-1234-5678",
            "tester@test.com",
            "short-bio",
            "https://github.com/tester",
            LocalDateTime.now());

    given(
            jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                eq(1L),
                eq("workspace-1"),
                eq(EntityStatus.ACTIVE.name())))
        .willReturn(1);
    given(applicantApplicationService.getApplicantDetail(11L)).willReturn(detail);

    ApiResponse<ApplicantDetailResponse> response =
        applicantController.getApplicantDetail(user, 11L, "workspace-1");

    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().applicationId()).isEqualTo(11L);
    assertThat(response.getData().shortBio()).isEqualTo("short-bio");
  }
}
