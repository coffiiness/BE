package com.coffiness.calfit.api.recruitment.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.InterviewScheduleResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/recruitments/{recruitmentId}/interview-schedules")
public class GET_specs {

  @Test
  void 채용공고의_월별_면접_일정_조회에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    LocalDateTime now = LocalDateTime.now();

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "2026년 상반기 백엔드 신입 모집",
            3,
            1L,
            "구합니다. 개발자",
            now.plusDays(1),
            now.plusDays(4),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L, 2L),
            List.of(101L, 102L),
            stages);

    recruitmentFixture.createRecruitment(token, tenantId, request);

    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().get(0).id();

    String yearMonth = "2026-03";

    // Act
    ApiResponse<List<InterviewScheduleResponse>> response =
        recruitmentFixture.getRecruitmentSchedule(token, tenantId, recruitmentId, yearMonth);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
  }
}
