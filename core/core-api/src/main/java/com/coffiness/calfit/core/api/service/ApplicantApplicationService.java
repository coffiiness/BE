package com.coffiness.calfit.core.api.service;

import com.coffiness.calfit.core.api.dto.v1.request.CareerApplicationSubmitRequest;
import com.coffiness.calfit.core.api.dto.v1.response.ApplicantDetailResponse;
import com.coffiness.calfit.core.api.dto.v1.response.ApplicantManagementItemResponse;
import com.coffiness.calfit.core.api.dto.v1.response.CareerApplicationSubmitResponse;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.UserRole;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
    String workspaceId = resolveWorkspaceId(companySlugOrWorkspaceId, recruitmentId);
    RecruitmentInfo recruitmentInfo = resolveRecruitmentInfo(workspaceId, recruitmentId);
    Long resolvedRecruitmentId = recruitmentInfo.id();
    String tenantId = recruitmentInfo.tenantId();
    Long firstStageId = getFirstStageId(tenantId, resolvedRecruitmentId);

    Long applicantId = createApplicant(tenantId, request);
    Gender gender = parseGender(request.gender());
    String schema = buildApplicationSchema(request);

    Long applicationId =
        createApplication(
            tenantId,
            applicantId,
            resolvedRecruitmentId,
            firstStageId,
            recruitmentInfo.applicationTemplateId(),
            request,
            gender,
            schema);

    return new CareerApplicationSubmitResponse(applicationId, applicantId);
  }

  private Long createApplication(
      String tenantId,
      Long applicantId,
      Long recruitmentId,
      Long recruitmentProcessId,
      Long templateId,
      CareerApplicationSubmitRequest request,
      Gender gender,
      String schema) {
    KeyHolder keyHolder = new GeneratedKeyHolder();

    try {
      jdbcTemplate.update(
          connection -> {
            PreparedStatement statement =
                connection.prepareStatement(
                    """
                    INSERT INTO applications
                      (applicant_id, recruitment_id, recruitment_process_id, template_id,
                       name, gender, birth_date, phone, email, schema,
                       status, tenant_id, created_at, updated_at)
                    VALUES
                      (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, applicantId);
            statement.setLong(2, recruitmentId);
            statement.setLong(3, recruitmentProcessId);
            statement.setLong(4, templateId);
            statement.setString(5, request.name());
            statement.setString(6, gender.name());
            statement.setTimestamp(7, Timestamp.valueOf(request.birthDate().atStartOfDay()));
            statement.setString(8, request.phone());
            statement.setString(9, request.email());
            statement.setString(10, schema);
            statement.setString(11, EntityStatus.ACTIVE.name());
            statement.setString(12, tenantId);
            return statement;
          },
          keyHolder);
    } catch (DataIntegrityViolationException e) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    } catch (DataAccessException e) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }

    Number generatedId = keyHolder.getKey();
    if (generatedId == null) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }

    return generatedId.longValue();
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

  @Transactional(readOnly = true)
  public ApplicantDetailResponse getApplicantDetail(Long detailId) {
    String tenantId = resolveTenantId();

    ApplicantDetailRow detail = findApplicantDetailByApplicationId(tenantId, detailId);
    if (detail == null) {
      detail = findLatestApplicantDetailByApplicantId(tenantId, detailId);
    }

    if (detail == null) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }

    SchemaFields schemaFields = parseSchemaFields(detail.schema());

    return new ApplicantDetailResponse(
        detail.applicationId(),
        detail.applicantId(),
        detail.recruitmentId(),
        detail.recruitmentTitle(),
        detail.stageName(),
        detail.name(),
        detail.gender(),
        detail.birthDate() == null ? null : detail.birthDate().toLocalDateTime().toLocalDate(),
        detail.phone(),
        detail.email(),
        schemaFields.shortBio(),
        schemaFields.portfolioUrl(),
        detail.appliedAt() == null ? null : detail.appliedAt().toLocalDateTime());
  }

  private ApplicantDetailRow findApplicantDetailByApplicationId(
      String tenantId, Long applicationId) {
    List<ApplicantDetailRow> rows =
        jdbcTemplate.query(
            """
            SELECT
              ap.id AS application_id,
              ap.applicant_id AS applicant_id,
              ap.recruitment_id AS recruitment_id,
              r.title AS recruitment_title,
              COALESCE(rs.stage_name, 'SUBMITTED') AS stage_name,
              ap.name AS applicant_name,
              ap.gender AS gender,
              ap.birth_date AS birth_date,
              ap.phone AS phone,
              ap.email AS email,
              ap.schema AS schema,
              ap.created_at AS applied_at
            FROM applications ap
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
              AND ap.id = ?
            LIMIT 1
            """,
            (resultSet, rowNum) ->
                new ApplicantDetailRow(
                    resultSet.getLong("application_id"),
                    resultSet.getLong("applicant_id"),
                    resultSet.getLong("recruitment_id"),
                    resultSet.getString("recruitment_title"),
                    resultSet.getString("stage_name"),
                    resultSet.getString("applicant_name"),
                    resultSet.getString("gender"),
                    resultSet.getTimestamp("birth_date"),
                    resultSet.getString("phone"),
                    resultSet.getString("email"),
                    resultSet.getString("schema"),
                    resultSet.getTimestamp("applied_at")),
            EntityStatus.ACTIVE.name(),
            EntityStatus.ACTIVE.name(),
            tenantId,
            EntityStatus.ACTIVE.name(),
            applicationId);

    return rows.isEmpty() ? null : rows.get(0);
  }

  private ApplicantDetailRow findLatestApplicantDetailByApplicantId(
      String tenantId, Long applicantId) {
    List<ApplicantDetailRow> rows =
        jdbcTemplate.query(
            """
            SELECT
              ap.id AS application_id,
              ap.applicant_id AS applicant_id,
              ap.recruitment_id AS recruitment_id,
              r.title AS recruitment_title,
              COALESCE(rs.stage_name, 'SUBMITTED') AS stage_name,
              ap.name AS applicant_name,
              ap.gender AS gender,
              ap.birth_date AS birth_date,
              ap.phone AS phone,
              ap.email AS email,
              ap.schema AS schema,
              ap.created_at AS applied_at
            FROM applications ap
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
              AND ap.applicant_id = ?
            ORDER BY ap.created_at DESC, ap.id DESC
            LIMIT 1
            """,
            (resultSet, rowNum) ->
                new ApplicantDetailRow(
                    resultSet.getLong("application_id"),
                    resultSet.getLong("applicant_id"),
                    resultSet.getLong("recruitment_id"),
                    resultSet.getString("recruitment_title"),
                    resultSet.getString("stage_name"),
                    resultSet.getString("applicant_name"),
                    resultSet.getString("gender"),
                    resultSet.getTimestamp("birth_date"),
                    resultSet.getString("phone"),
                    resultSet.getString("email"),
                    resultSet.getString("schema"),
                    resultSet.getTimestamp("applied_at")),
            EntityStatus.ACTIVE.name(),
            EntityStatus.ACTIVE.name(),
            tenantId,
            EntityStatus.ACTIVE.name(),
            applicantId);

    return rows.isEmpty() ? null : rows.get(0);
  }

  private SchemaFields parseSchemaFields(String schema) {
    if (schema == null || schema.isBlank()) {
      return new SchemaFields(null, null);
    }

    try {
      JsonNode node = objectMapper.readTree(schema);
      if (node != null && node.isTextual()) {
        node = objectMapper.readTree(node.asText());
      }

      return new SchemaFields(readText(node, "shortBio"), readText(node, "portfolioUrl"));
    } catch (JsonProcessingException e) {
      return new SchemaFields(null, null);
    }
  }

  private String readText(JsonNode node, String fieldName) {
    JsonNode value = node == null ? null : node.get(fieldName);
    if (value == null || value.isNull()) {
      return null;
    }

    String text = value.asText();
    return text == null || text.isBlank() ? null : text;
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

  private String resolveWorkspaceId(String companySlugOrWorkspaceId, Long recruitmentId) {
    String tenantIdFromContext = TenantContext.getTenantId();
    if (isUsableTenantId(tenantIdFromContext) && existsWorkspaceId(tenantIdFromContext)) {
      return tenantIdFromContext;
    }

    String workspaceByDirectId = findWorkspaceByDirectId(companySlugOrWorkspaceId);
    if (workspaceByDirectId != null) {
      return workspaceByDirectId;
    }

    List<String> workspaceCandidates = findWorkspaceCandidatesBySlug(companySlugOrWorkspaceId);
    if (workspaceCandidates.isEmpty()) {
      return findWorkspaceIdByRecruitmentId(recruitmentId);
    }

    if (workspaceCandidates.size() == 1) {
      return workspaceCandidates.get(0);
    }

    if (isUsableTenantId(tenantIdFromContext)
        && workspaceCandidates.contains(tenantIdFromContext)) {
      return tenantIdFromContext;
    }

    String workspaceByRecruitmentId = findWorkspaceIdByRecruitmentId(recruitmentId);
    if (workspaceByRecruitmentId != null
        && workspaceCandidates.contains(workspaceByRecruitmentId)) {
      return workspaceByRecruitmentId;
    }

    return workspaceCandidates.get(0);
  }

  private String findWorkspaceByDirectId(String workspaceId) {
    List<String> rows =
        jdbcTemplate.query(
            """
            SELECT workspace_id
            FROM workspaces
            WHERE workspace_id = ?
              AND status = ?
            LIMIT 1
            """,
            (resultSet, rowNum) -> resultSet.getString("workspace_id"),
            workspaceId,
            EntityStatus.ACTIVE.name());

    return rows.isEmpty() ? null : rows.get(0);
  }

  private List<String> findWorkspaceCandidatesBySlug(String companySlugOrWorkspaceId) {
    return jdbcTemplate.query(
        """
        SELECT workspace_id
        FROM workspaces
        WHERE status = ?
          AND (
            LOWER(name) = LOWER(?)
             OR LOWER(REPLACE(name, ' ', '-')) = LOWER(?)
             OR LOWER(REPLACE(name, ' ', '')) = LOWER(REPLACE(?, '-', ''))
          )
        ORDER BY id DESC
        """,
        (resultSet, rowNum) -> resultSet.getString("workspace_id"),
        EntityStatus.ACTIVE.name(),
        companySlugOrWorkspaceId,
        companySlugOrWorkspaceId,
        companySlugOrWorkspaceId);
  }

  private String findWorkspaceIdByRecruitmentId(Long recruitmentId) {
    if (recruitmentId != null && recruitmentId > 0) {
      List<String> rows =
          jdbcTemplate.query(
              """
              SELECT tenant_id
              FROM recruitments
              WHERE id = ?
                AND status = ?
              LIMIT 1
              """,
              (resultSet, rowNum) -> resultSet.getString("tenant_id"),
              recruitmentId,
              EntityStatus.ACTIVE.name());

      if (!rows.isEmpty()) {
        return rows.get(0);
      }
    }

    return findSingleWorkspaceId();
  }

  private String findSingleWorkspaceId() {
    List<String> rows =
        jdbcTemplate.query(
            """
            SELECT workspace_id
            FROM workspaces
            WHERE status = ?
            ORDER BY id DESC
            LIMIT 2
            """,
            (resultSet, rowNum) -> resultSet.getString("workspace_id"),
            EntityStatus.ACTIVE.name());

    if (rows.size() == 1) {
      return rows.get(0);
    }

    return null;
  }

  private boolean existsWorkspaceId(String workspaceId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(1)
            FROM workspaces
            WHERE workspace_id = ?
            """,
            Integer.class,
            workspaceId);

    return count != null && count > 0;
  }

  private boolean isUsableTenantId(String tenantId) {
    return tenantId != null && !tenantId.isBlank() && !"default".equalsIgnoreCase(tenantId);
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

  private Long createApplicant(String tenantId, CareerApplicationSubmitRequest request) {
    String encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
    KeyHolder keyHolder = new GeneratedKeyHolder();

    try {
      jdbcTemplate.update(
          connection -> {
            PreparedStatement statement =
                connection.prepareStatement(
                    """
                    INSERT INTO applicants
                      (email, password, name, role, status, tenant_id, created_at, updated_at)
                    VALUES
                      (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.email());
            statement.setString(2, encodedPassword);
            statement.setString(3, request.name());
            statement.setString(4, UserRole.MEMBER.name());
            statement.setString(5, EntityStatus.ACTIVE.name());
            statement.setString(6, tenantId);
            return statement;
          },
          keyHolder);
    } catch (DataIntegrityViolationException e) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    } catch (DataAccessException e) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }

    Number generatedId = keyHolder.getKey();
    if (generatedId == null) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }

    return generatedId.longValue();
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

  private record ApplicantDetailRow(
      Long applicationId,
      Long applicantId,
      Long recruitmentId,
      String recruitmentTitle,
      String stageName,
      String name,
      String gender,
      Timestamp birthDate,
      String phone,
      String email,
      String schema,
      Timestamp appliedAt) {}

  private record SchemaFields(String shortBio, String portfolioUrl) {}
}
