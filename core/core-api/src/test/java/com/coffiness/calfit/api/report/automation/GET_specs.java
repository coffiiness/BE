package com.coffiness.calfit.api.report.automation;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.core.api.facade.report.ReportFacade.AutomationReportResult;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/reports/automation")
class GET_specs {

  @Test
  void 자동화_리포트_조회에_성공한다(@Autowired MemberFixture memberFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<AutomationReportResult> response =
        memberFixture
            .base()
            .get("/api/v1/reports/automation", context.hrToken(), AutomationReportResult.class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    AutomationReportResult data = response.getData();
    assertThat(data).isNotNull();
    assertThat(data.eventStatusDistribution()).isNotEmpty();
    assertThat(data.actionTypeDistribution()).isNotEmpty();
    assertThat(data.failureRate()).isGreaterThanOrEqualTo(0.0);
    assertThat(data.adoptionRate()).isGreaterThanOrEqualTo(0.0);
  }
}
