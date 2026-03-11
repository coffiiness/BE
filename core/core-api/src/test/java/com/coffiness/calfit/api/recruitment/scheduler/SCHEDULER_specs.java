package com.coffiness.calfit.api.recruitment.scheduler;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.api.scheduler.recruitment.RecruitmentStatusScheduler;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@CalfitApiTest
@DisplayName("Recruitment Status Scheduler")
public class SCHEDULER_specs {

  @Test
  void 시작_시간이_지나면_스케줄러가_채용_공고를_OPEN으로_변경한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired RecruitmentStatusScheduler recruitmentStatusScheduler) {

    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    String openTargetTitle = "scheduler-open-target-" + System.nanoTime();
    String draftTargetTitle = "scheduler-draft-target-" + System.nanoTime();
    LocalDateTime now = LocalDateTime.now();

    recruitmentFixture.createRecruitment(
        token, tenantId, createRequest(openTargetTitle, now.minusMinutes(10), now.plusDays(2)));
    recruitmentFixture.createRecruitment(
        token, tenantId, createRequest(draftTargetTitle, now.plusDays(1), now.plusDays(3)));

    recruitmentStatusScheduler.updateRecruitmentStatuses();

    ApiResponse<List<RecruitmentListResponse>> response =
        recruitmentFixture.getRecruitmentList(token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    Map<String, RecruitmentStatus> statusByTitle =
        response.getData().stream()
            .collect(
                Collectors.toMap(RecruitmentListResponse::title, RecruitmentListResponse::status));

    assertThat(statusByTitle.get(openTargetTitle)).isEqualTo(RecruitmentStatus.OPEN);
    assertThat(statusByTitle.get(draftTargetTitle)).isEqualTo(RecruitmentStatus.DRAFT);
  }

  @Test
  void 종료_시간이_지나면_스케줄러가_채용_공고를_ClOSED로_변경한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired RecruitmentStatusScheduler recruitmentStatusScheduler) {

    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();

    String closedTargetTitle = "scheduler-closed-target-" + System.nanoTime();
    LocalDateTime now = LocalDateTime.now();

    recruitmentFixture.createRecruitment(
        token, tenantId, createRequest(closedTargetTitle, now.minusDays(2), now.minusMinutes(1)));

    recruitmentStatusScheduler.updateRecruitmentStatuses();

    ApiResponse<List<RecruitmentListResponse>> response =
        recruitmentFixture.getRecruitmentList(token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentStatus closedStatus =
        response.getData().stream()
            .filter(item -> item.title().equals(closedTargetTitle))
            .map(RecruitmentListResponse::status)
            .findFirst()
            .orElseThrow();

    assertThat(closedStatus).isEqualTo(RecruitmentStatus.CLOSED);
  }

  private RecruitmentCreateRequest createRequest(
      String title, LocalDateTime startDate, LocalDateTime endDate) {
    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("Document", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("Interview", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("Pass", RecruitmentStageType.PASS, 3));

    return new RecruitmentCreateRequest(
        title,
        3,
        1L,
        "recruitment description",
        startDate,
        endDate,
        CareerType.NEW,
        null,
        null,
        1L,
        List.of(1L),
        List.of(101L),
        stages);
  }
}
