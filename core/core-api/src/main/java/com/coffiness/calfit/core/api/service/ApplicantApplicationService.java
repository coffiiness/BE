package com.coffiness.calfit.core.api.service;

import com.coffiness.calfit.core.api.dto.v1.request.CareerApplicationSubmitRequest;
import com.coffiness.calfit.core.api.dto.v1.response.ApplicantManagementItemResponse;
import com.coffiness.calfit.core.api.dto.v1.response.CareerApplicationSubmitResponse;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.UserRole;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicantApplicationService {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public CareerApplicationSubmitResponse submitCareerApplication(
      String companySlugOrWorkspaceId, Long recruitmentId, CareerApplicationSubmitRequest request) {
    String workspaceId = resolveWorkspaceId(companySlugOrWorkspaceId);
    RecruitmentInfo recruitmentInfo = resolveRecruitmentInfo(workspaceId, recruitmentId);
    Long resolvedRecruitmentId = recruitmentInfo.id();
    String tenantId = recruitmentInfo.tenantId();
    Long firstStageId = getFirstStageId(tenantId, resolvedRecruitmentId);

    Long applicantId = createApplicant(tenantId, request);

    Gender gender = parseGender(request.gender());
    String schema = buildApplicationSchema(request);

    try {
      jdbcTemplate.update(
          """
          INSERT INTO applications
            (applicant_id, recruitment_id, recruitment_process_id, template_id,
             name, gender, birth_date, phone, email, schema,
             status, tenant_id, created_at, updated_at)
          VALUES
            (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """,
          applicantId,
          resolvedRecruitmentId,
          firstStageId,
          recruitmentInfo.applicationTemplateId(),
          request.name(),
          gender.name(),
          Timestamp.valueOf(request.birthDate().atStartOfDay()),
          request.phone(),
          request.email(),
          schema,
          EntityStatus.ACTIVE.name(),
          tenantId);
    } catch (DataAccessException e) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    Long applicationId =
        jdbcTemplate.queryForObject(
            """
            SELECT id
            FROM applications
            WHERE tenant_id = ?
              AND applicant_id = ?
              AND recruitment_id = ?
              AND status = ?
            ORDER BY id DESC
            LIMIT 1
            """,
            Long.class,
            tenantId,
            applicantId,
            resolvedRecruitmentId,
            EntityStatus.ACTIVE.name());

    if (applicationId == null) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }

    return new CareerApplicationSubmitResponse(applicationId, applicantId);
  }

  @Transactional(readOnly = true)
  public List<ApplicantManagementItemResponse> getApplicantManagementItems(String keyword) {
    String tenantId = resolveTenantId();

    StringBuilder sql =
        new StringBuilder(
            """
            SELECT
              ap.id AS application_id,
              ap.applicant_id AS applicant_id,
              a.name AS applicant_name,
              a.email AS applicant_email,
              r.id AS recruitment_id,
              r.title AS recruitment_title,
              COALESCE(rs.stage_name, 'SUBMITTED') AS stage_name,
              ap.created_at AS applied_at,
              (
                SELECT MIN(s.scheduled_at)
                FROM interview_schedule_applicants isa
                JOIN interview_schedules s
                  ON s.id = isa.interview_schedule_id
                 AND s.tenant_id = ap.tenant_id
                 AND s.status = ?
                WHERE isa.tenant_id = ap.tenant_id
                  AND isa.applicant_id = ap.applicant_id
                  AND isa.status = ?
              ) AS next_interview_at
            FROM applications ap
            JOIN applicants a
              ON a.id = ap.applicant_id
             AND a.tenant_id = ap.tenant_id
             AND a.status = ?
            JOIN recruitments r
              ON r.id = ap.recruitment_id
             AND r.tenant_id = ap.tenant_id
             AND r.status = ?
            LEFT JOIN recruitment_stages rs
              ON rs.id = ap.recruitment_process_id
             AND rs.tenant_id = ap.tenant_id
             AND rs.status = ?
            WHERE ap.tenant_id = ?
              AND ap.status = ?
            """
                .trim());

    List<Object> params = new ArrayList<>();
    params.add(EntityStatus.ACTIVE.name());
    params.add(EntityStatus.ACTIVE.name());
    params.add(EntityStatus.ACTIVE.name());
    params.add(EntityStatus.ACTIVE.name());
    params.add(EntityStatus.ACTIVE.name());
    params.add(tenantId);
    params.add(EntityStatus.ACTIVE.name());

    if (keyword != null && !keyword.isBlank()) {
      String likeKeyword = "%" + keyword.toLowerCase() + "%";
      sql.append(" AND (LOWER(a.name) LIKE ? OR LOWER(a.email) LIKE ? OR LOWER(r.title) LIKE ?)");
      params.add(likeKeyword);
      params.add(likeKeyword);
      params.add(likeKeyword);
    }

    sql.append(" ORDER BY ap.created_at DESC");

    return jdbcTemplate.query(
        sql.toString(),
        params.toArray(),
        (resultSet, rowNum) -> {
          Timestamp nextInterviewAt = resultSet.getTimestamp("next_interview_at");
          Timestamp appliedAt = resultSet.getTimestamp("applied_at");

          return new ApplicantManagementItemResponse(
              resultSet.getLong("application_id"),
              resultSet.getLong("applicant_id"),
              resultSet.getString("applicant_name"),
              resultSet.getString("applicant_email"),
              resultSet.getLong("recruitment_id"),
              resultSet.getString("recruitment_title"),
              resultSet.getString("stage_name"),
              nextInterviewAt == null ? null : nextInterviewAt.toLocalDateTime(),
              appliedAt == null ? null : appliedAt.toLocalDateTime());
        });
  }

  private RecruitmentInfo resolveRecruitmentInfo(String workspaceId, Long recruitmentId) {
    if (workspaceId == null || workspaceId.isBlank()) {
      RecruitmentInfo byId = getRecruitmentInfoById(recruitmentId);
      if (byId == null) {
        throw new CoreException(ErrorType.NOT_FOUND);
      }

      return byId;
    }

    RecruitmentInfo byIdInWorkspace =
        getRecruitmentInfoByIdAndWorkspace(recruitmentId, workspaceId);
    if (byIdInWorkspace != null) {
      return byIdInWorkspace;
    }

    RecruitmentInfo byVirtualJobId =
        getRecruitmentInfoByVirtualJobIdInWorkspace(workspaceId, recruitmentId);
    if (byVirtualJobId != null) {
      return byVirtualJobId;
    }

    throw new CoreException(ErrorType.NOT_FOUND);
  }

  private RecruitmentInfo getRecruitmentInfoById(Long recruitmentId) {
    List<RecruitmentInfo> rows =
        jdbcTemplate.query(
            """
            SELECT id, tenant_id, application_template_id
            FROM recruitments
            WHERE id = ?
              AND status = ?
            """,
            (resultSet, rowNum) ->
                new RecruitmentInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("tenant_id"),
                    resultSet.getLong("application_template_id")),
            recruitmentId,
            EntityStatus.ACTIVE.name());

    return rows.isEmpty() ? null : rows.get(0);
  }

  private RecruitmentInfo getRecruitmentInfoByIdAndWorkspace(
      Long recruitmentId, String workspaceId) {
    List<RecruitmentInfo> rows =
        jdbcTemplate.query(
            """
            SELECT id, tenant_id, application_template_id
            FROM recruitments
            WHERE id = ?
              AND tenant_id = ?
              AND status = ?
            """,
            (resultSet, rowNum) ->
                new RecruitmentInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("tenant_id"),
                    resultSet.getLong("application_template_id")),
            recruitmentId,
            workspaceId,
            EntityStatus.ACTIVE.name());

    return rows.isEmpty() ? null : rows.get(0);
  }

  private RecruitmentInfo getRecruitmentInfoByVirtualJobIdInWorkspace(
      String workspaceId, Long recruitmentId) {
    if (recruitmentId == null || recruitmentId <= 0) {
      return null;
    }

    long offset = recruitmentId - 1;
    if (offset > Integer.MAX_VALUE) {
      return null;
    }

    List<RecruitmentInfo> rows =
        jdbcTemplate.query(
            """
            SELECT id, tenant_id, application_template_id
            FROM recruitments
            WHERE tenant_id = ?
              AND status = ?
            ORDER BY created_at DESC, id DESC
            LIMIT 1 OFFSET ?
            """,
            (resultSet, rowNum) ->
                new RecruitmentInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("tenant_id"),
                    resultSet.getLong("application_template_id")),
            workspaceId,
            EntityStatus.ACTIVE.name(),
            (int) offset);

    return rows.isEmpty() ? null : rows.get(0);
  }

  private String resolveWorkspaceId(String companySlugOrWorkspaceId) {
    List<String> byWorkspaceId =
        jdbcTemplate.query(
            """
            SELECT workspace_id
            FROM workspaces
            WHERE workspace_id = ?
            LIMIT 1
            """,
            (resultSet, rowNum) -> resultSet.getString("workspace_id"),
            companySlugOrWorkspaceId);

    if (!byWorkspaceId.isEmpty()) {
      return byWorkspaceId.get(0);
    }

    List<String> byNameOrSlug =
        jdbcTemplate.query(
            """
            SELECT workspace_id
            FROM workspaces
            WHERE LOWER(name) = LOWER(?)
               OR LOWER(REPLACE(name, ' ', '-')) = LOWER(?)
               OR LOWER(REPLACE(name, ' ', '')) = LOWER(REPLACE(?, '-', ''))
            LIMIT 1
            """,
            (resultSet, rowNum) -> resultSet.getString("workspace_id"),
            companySlugOrWorkspaceId,
            companySlugOrWorkspaceId,
            companySlugOrWorkspaceId);

    return byNameOrSlug.isEmpty() ? null : byNameOrSlug.get(0);
  }

  private Long getFirstStageId(String tenantId, Long recruitmentId) {
    List<Long> stageIds =
        jdbcTemplate.query(
            """
            SELECT id
            FROM recruitment_stages
            WHERE tenant_id = ?
              AND recruitment_id = ?
              AND status = ?
            ORDER BY stage_step ASC
            LIMIT 1
            """,
            (resultSet, rowNum) -> resultSet.getLong("id"),
            tenantId,
            recruitmentId,
            EntityStatus.ACTIVE.name());

    if (stageIds.isEmpty()) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }

    return stageIds.get(0);
  }

  private Long findApplicantId(String tenantId, String email) {
    List<Long> ids =
        jdbcTemplate.query(
            """
            SELECT id
            FROM applicants
            WHERE tenant_id = ?
              AND email = ?
              AND status = ?
            ORDER BY id DESC
            LIMIT 1
            """,
            (resultSet, rowNum) -> resultSet.getLong("id"),
            tenantId,
            email,
            EntityStatus.ACTIVE.name());

    return ids.isEmpty() ? null : ids.get(0);
  }

  private Long createApplicant(String tenantId, CareerApplicationSubmitRequest request) {
    String encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString());

    try {
      jdbcTemplate.update(
          """
          INSERT INTO applicants
            (email, password, name, role, status, tenant_id, created_at, updated_at)
          VALUES
            (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """,
          request.email(),
          encodedPassword,
          request.name(),
          UserRole.MEMBER.name(),
          EntityStatus.ACTIVE.name(),
          tenantId);
    } catch (DataAccessException e) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    Long applicantId = findApplicantId(tenantId, request.email());
    if (applicantId == null) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }

    return applicantId;
  }

  private String buildApplicationSchema(CareerApplicationSubmitRequest request) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("shortBio", request.shortBio());
    schema.put("portfolioUrl", request.portfolioUrl());

    try {
      return objectMapper.writeValueAsString(schema);
    } catch (JsonProcessingException e) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }
  }

  private Gender parseGender(String gender) {
    if (gender == null || gender.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    String normalized = gender.trim().toUpperCase();
    return switch (normalized) {
      case "MALE", "M", "MAN", "\uB0A8\uC131", "\uB0A8\uC790" -> Gender.MALE;
      case "FEMALE", "F", "WOMAN", "\uC5EC\uC131", "\uC5EC\uC790" -> Gender.FEMALE;
      default -> throw new CoreException(ErrorType.VALIDATION_ERROR);
    };
  }

  private String resolveTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    return tenantId;
  }

  private record RecruitmentInfo(Long id, String tenantId, Long applicationTemplateId) {}
}
