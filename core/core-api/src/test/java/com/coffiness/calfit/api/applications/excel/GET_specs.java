package com.coffiness.calfit.api.applications.excel;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@CalfitApiTest
@DisplayName("GET /api/v1/applications/excel")
class GET_specs {

  private static final String EXCEL_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @Test
  void hr_account_request_returns_applicant_excel_file(
      @Autowired TestRestTemplate restTemplate,
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentRepository recruitmentRepository,
      @Autowired RecruitmentStageRepository recruitmentStageRepository,
      @Autowired ApplicationRepository applicationRepository)
      throws Exception {
    ExcelTestFixture fixture =
        createFixture(
            userFixture, workspaceFixture, recruitmentRepository, recruitmentStageRepository);

    TenantContext.setTenantId(fixture.tenantId());
    try {
      applicationRepository.save(
          ApplicationEntity.create(
              999L,
              fixture.recruitmentId(),
              fixture.stageId(),
              1L,
              "Export Candidate",
              Gender.FEMALE,
              LocalDateTime.of(1998, 3, 1, 0, 0),
              "010-1111-2222",
              "export-candidate@test.com",
              "{}"));
    } finally {
      TenantContext.clear();
    }

    ResponseEntity<byte[]> response =
        requestExcel(restTemplate, fixture.tenantId(), fixture.token(), null);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.parseMediaType(EXCEL_CONTENT_TYPE));
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("attachment; filename=\"")
        .contains(".xlsx");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).isNotEmpty();

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getBody()))) {
      Sheet sheet = workbook.getSheetAt(0);

      assertThat(sheet.getSheetName()).isEqualTo("\uC9C0\uC6D0\uC790 \uBAA9\uB85D");
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("\uC9C0\uC6D0\uC790\uBA85");
      assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("\uC774\uBA54\uC77C");
      assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("\uACF5\uACE0");
      assertThat(sheet.getRow(0).getCell(3).getStringCellValue())
          .isEqualTo("\uC9C4\uD589 \uC0C1\uD0DC");
      assertThat(sheet.getRow(0).getCell(4).getStringCellValue())
          .isEqualTo("\uB2E4\uC74C \uC77C\uC815");
      assertThat(sheet.getRow(0).getCell(5).getStringCellValue()).isEqualTo("\uC9C0\uC6D0\uC77C");

      assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Export Candidate");
      assertThat(sheet.getRow(1).getCell(1).getStringCellValue())
          .isEqualTo("export-candidate@test.com");
      assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Backend Hiring");
      assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Document Review");
      assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("-");
      assertThat(sheet.getRow(1).getCell(5).getStringCellValue())
          .matches("\\d{4}\\.\\d{2}\\.\\d{2}");
    }
  }

  @Test
  void hr_account_request_returns_header_only_excel_even_when_search_result_is_empty(
      @Autowired TestRestTemplate restTemplate,
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentRepository recruitmentRepository,
      @Autowired RecruitmentStageRepository recruitmentStageRepository,
      @Autowired ApplicationRepository applicationRepository)
      throws Exception {
    ExcelTestFixture fixture =
        createFixture(
            userFixture, workspaceFixture, recruitmentRepository, recruitmentStageRepository);

    TenantContext.setTenantId(fixture.tenantId());
    try {
      applicationRepository.save(
          ApplicationEntity.create(
              1000L,
              fixture.recruitmentId(),
              fixture.stageId(),
              1L,
              "Existing Candidate",
              Gender.MALE,
              LocalDateTime.of(1997, 5, 1, 0, 0),
              "010-2222-3333",
              "existing-candidate@test.com",
              "{}"));
    } finally {
      TenantContext.clear();
    }

    ResponseEntity<byte[]> response =
        requestExcel(
            restTemplate, fixture.tenantId(), fixture.token(), "?search=no-such-applicant");

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).isNotEmpty();

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getBody()))) {
      Sheet sheet = workbook.getSheetAt(0);

      assertThat(sheet.getSheetName()).isEqualTo("\uC9C0\uC6D0\uC790 \uBAA9\uB85D");
      assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1);
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("\uC9C0\uC6D0\uC790\uBA85");
      assertThat(sheet.getRow(1)).isNull();
    }
  }

  private ExcelTestFixture createFixture(
      UserFixture userFixture,
      WorkspaceFixture workspaceFixture,
      RecruitmentRepository recruitmentRepository,
      RecruitmentStageRepository recruitmentStageRepository) {
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    TenantContext.setTenantId(tenantId);
    try {
      RecruitmentEntity recruitment =
          recruitmentRepository.save(
              RecruitmentEntity.builder()
                  .creatorId(1L)
                  .title("Backend Hiring")
                  .recruitmentStatus(RecruitmentStatus.OPEN)
                  .targetCount(1)
                  .startDate(LocalDateTime.now().minusDays(1))
                  .endDate(LocalDateTime.now().plusDays(7))
                  .applicationTemplateId(1L)
                  .contents("Backend hiring post")
                  .careerType(CareerType.NEW)
                  .leadGroupId(1L)
                  .build());

      RecruitmentStageEntity stage =
          recruitmentStageRepository.save(
              RecruitmentStageEntity.builder()
                  .recruitmentId(recruitment.getId())
                  .stageName("Document Review")
                  .stageStep(1)
                  .stageType(RecruitmentStageType.DOCUMENT)
                  .build());

      return new ExcelTestFixture(token, tenantId, recruitment.getId(), stage.getId());
    } finally {
      TenantContext.clear();
    }
  }

  private ResponseEntity<byte[]> requestExcel(
      TestRestTemplate restTemplate, String tenantId, String token, String queryString) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.set("X-Tenant-ID", tenantId);

    String path = "/api/v1/applications/excel" + (queryString == null ? "" : queryString);
    return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<Void>(headers), byte[].class);
  }

  private record ExcelTestFixture(
      String token, String tenantId, Long recruitmentId, Long stageId) {}
}
