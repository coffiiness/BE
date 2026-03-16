package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicationExcelController {

  private static final String EXCEL_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

  private final ApplicationRepository applicationRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;

  @GetMapping({"/api/v1/applications/excel", "/api/v1/applications/export"})
  public ResponseEntity<byte[]> exportApplicantsExcel(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String job) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if ("APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    List<ApplicationEntity> entities = applicationRepository.findByStatus(EntityStatus.ACTIVE);

    String searchKeyword = keyword != null ? keyword : search;
    if (searchKeyword != null && !searchKeyword.isBlank()) {
      String kw = searchKeyword.trim().toLowerCase();
      entities =
          entities.stream()
              .filter(
                  e ->
                      (e.getName() != null && e.getName().toLowerCase().contains(kw))
                          || (e.getEmail() != null && e.getEmail().toLowerCase().contains(kw)))
              .collect(Collectors.toList());
    }

    Map<Long, String> stageNameMap = buildStageNameMap(entities);

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("지원자 목록");

      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      String[] headers = {"지원자명", "이메일", "연락처", "성별", "생년월일", "진행 상태", "지원일"};
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowIdx = 1;
      for (ApplicationEntity entity : entities) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(entity.getName() != null ? entity.getName() : "");
        row.createCell(1).setCellValue(entity.getEmail() != null ? entity.getEmail() : "");
        row.createCell(2).setCellValue(entity.getPhone() != null ? entity.getPhone() : "");
        row.createCell(3).setCellValue(entity.getGender() != null ? entity.getGender().name() : "");
        row.createCell(4).setCellValue(formatDate(entity.getBirthDate()));
        row.createCell(5)
            .setCellValue(stageNameMap.getOrDefault(entity.getRecruitmentProcessId(), ""));
        row.createCell(6).setCellValue(formatDate(entity.getCreatedAt()));
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);

      String filename =
          "applicants-"
              + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
              + "-"
              + System.currentTimeMillis() % 1_000_000
              + ".xlsx";
      String encodedFilename =
          URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
          .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
          .body(out.toByteArray());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  private Map<Long, String> buildStageNameMap(List<ApplicationEntity> entities) {
    List<Long> stageIds =
        entities.stream()
            .map(ApplicationEntity::getRecruitmentProcessId)
            .distinct()
            .collect(Collectors.toList());
    if (stageIds.isEmpty()) {
      return Map.of();
    }
    return recruitmentStageRepository.findAllById(stageIds).stream()
        .collect(
            Collectors.toMap(
                RecruitmentStageEntity::getId, RecruitmentStageEntity::getStageName, (a, b) -> a));
  }

  private String formatDate(LocalDateTime dateTime) {
    if (dateTime == null) {
      return "";
    }
    return dateTime.format(DATE_FMT);
  }
}
