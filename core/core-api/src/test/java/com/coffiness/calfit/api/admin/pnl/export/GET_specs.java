package com.coffiness.calfit.api.admin.pnl.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BillingFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

@CalfitApiTest
@DisplayName("GET /api/v1/admin/pnl/export")
public class GET_specs {

  @Test
  void 올바르게_요청하면_Excel_파일을_반환한다(@Autowired BillingFixture fixture) {
    String token = fixture.adminToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<byte[]> response =
        fixture
            .base()
            .client()
            .exchange("/api/v1/admin/pnl/export?months=3", HttpMethod.GET, entity, byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType().toString()).contains("spreadsheetml.sheet");
    assertThat(response.getHeaders().getContentDisposition().getFilename())
        .contains("PnL_report.xlsx");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThan(0);
  }

  @Test
  void 인증되지_않은_사용자는_에러_응답을_반환한다(@Autowired BillingFixture fixture) {
    HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

    ResponseEntity<byte[]> response =
        fixture
            .base()
            .client()
            .exchange("/api/v1/admin/pnl/export", HttpMethod.GET, entity, byte[].class);

    assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
  }

  @Test
  void 관리자가_아닌_사용자는_403을_반환한다(@Autowired BillingFixture fixture) {
    String token = fixture.memberToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<byte[]> response =
        fixture
            .base()
            .client()
            .exchange("/api/v1/admin/pnl/export", HttpMethod.GET, entity, byte[].class);

    assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
  }
}
