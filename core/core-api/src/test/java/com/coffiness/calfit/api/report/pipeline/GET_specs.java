package com.coffiness.calfit.api.report.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.core.api.facade.report.ReportFacade.PipelineResult;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/reports/pipeline")
class GET_specs {

  @Test
  void 파이프라인_조회에_성공한다(@Autowired MemberFixture memberFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<PipelineResult> response =
        memberFixture
            .base()
            .get("/api/v1/reports/pipeline", context.hrToken(), PipelineResult.class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().stageDistribution()).isNotEmpty();
    assertThat(response.getData().monthlyTrend()).isNotEmpty();
    assertThat(response.getData().monthlyTrend()).hasSize(6);
  }

  @Test
  void months_파라미터로_기간을_지정할_수_있다(@Autowired MemberFixture memberFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<PipelineResult> response =
        memberFixture
            .base()
            .get("/api/v1/reports/pipeline?months=3", context.hrToken(), PipelineResult.class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().monthlyTrend()).hasSize(3);
  }
}
