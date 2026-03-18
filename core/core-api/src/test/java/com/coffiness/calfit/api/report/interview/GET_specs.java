package com.coffiness.calfit.api.report.interview;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.core.api.facade.report.ReportFacade.InterviewReportResult;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/reports/interview")
class GET_specs {

  @Test
  void 면접_리포트_조회에_성공한다(@Autowired MemberFixture memberFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<InterviewReportResult> response =
        memberFixture
            .base()
            .get("/api/v1/reports/interview", context.hrToken(), InterviewReportResult.class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    InterviewReportResult data = response.getData();
    assertThat(data).isNotNull();
    assertThat(data.statusDistribution()).isNotEmpty();
    assertThat(data.cancelRate()).isGreaterThanOrEqualTo(0.0);
    assertThat(data.avgDurationMinutes()).isGreaterThanOrEqualTo(0.0);
  }
}
