package com.coffiness.calfit.core.api.service;

import com.coffiness.calfit.core.api.dto.v1.response.ApplicantManagementItemResponse;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicantExcelExportService {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final ApplicantApplicationService applicantApplicationService;

  @Transactional(readOnly = true)
  public byte[] exportApplicantManagementExcel(String keyword) {
    List<ApplicantManagementItemResponse> applicants =
        applicantApplicationService.getApplicantManagementItems(keyword);

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Applicants");
      writeHeaderRow(workbook, sheet);
      writeDataRows(sheet, applicants);
      applyColumnWidths(sheet);

      workbook.write(outputStream);
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }
  }

  private void writeHeaderRow(Workbook workbook, Sheet sheet) {
    String[] headers = {
      "Applicant Name", "Email", "Recruitment", "Stage", "Next Interview", "Applied At"
    };

    Row headerRow = sheet.createRow(0);
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);

    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }
  }

  private void writeDataRows(Sheet sheet, List<ApplicantManagementItemResponse> applicants) {
    int rowIndex = 1;

    for (ApplicantManagementItemResponse applicant : applicants) {
      Row row = sheet.createRow(rowIndex++);
      row.createCell(0).setCellValue(defaultValue(applicant.applicantName()));
      row.createCell(1).setCellValue(defaultValue(applicant.applicantEmail()));
      row.createCell(2).setCellValue(defaultValue(applicant.recruitmentTitle()));
      row.createCell(3).setCellValue(defaultValue(applicant.stageName()));
      row.createCell(4)
          .setCellValue(
              applicant.nextInterviewAt() == null
                  ? "-"
                  : applicant.nextInterviewAt().format(DATE_TIME_FORMATTER));
      row.createCell(5)
          .setCellValue(
              applicant.appliedAt() == null ? "-" : applicant.appliedAt().format(DATE_FORMATTER));
    }
  }

  private void applyColumnWidths(Sheet sheet) {
    sheet.setColumnWidth(0, 20 * 256);
    sheet.setColumnWidth(1, 30 * 256);
    sheet.setColumnWidth(2, 24 * 256);
    sheet.setColumnWidth(3, 18 * 256);
    sheet.setColumnWidth(4, 22 * 256);
    sheet.setColumnWidth(5, 14 * 256);
  }

  private String defaultValue(String value) {
    return value == null ? "" : value;
  }
}
