package com.coffiness.calfit.api.report.kpi;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.api.facade.report.ReportFacade.KpiResult;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/reports/kpi")
class GET_specs {

  @Test
  void 데이터가_없을_때_KPI_조회에_성공한다(@Autowired MemberFixture memberFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<KpiResult> response = get(memberFixture, context.hrToken(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().totalApplicants()).isZero();
    assertThat(response.getData().conversionRate()).isEqualTo(0.0);
  }

  @Test
  void 지원자가_있을_때_KPI_조회에_성공한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long templateId = applicationTemplateFixture.createUsedTemplateId(token, tenantId, "kpi");

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "KPI 테스트 채용",
            5,
            templateId,
            "desc",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(30),
            CareerType.NEW,
            null,
            null,
            leadGroupId,
            List.of(),
            List.of(hrUserId),
            List.of(
                new RecruitmentStageRequest("서류", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("면접", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("합격", RecruitmentStageType.PASS, 3),
                new RecruitmentStageRequest("불합격", RecruitmentStageType.FAIL, 4)));

    recruitmentFixture.createRecruitment(token, tenantId, request);
    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> "KPI 테스트 채용".equals(item.title()))
            .findFirst()
            .orElseThrow();

    Long documentStageId = recruitment.stages().get(0).id();
    Long passStageId = recruitment.stages().get(2).id();

    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        documentStageId,
        templateId,
        901L,
        "지원자A",
        "kpi-a@test.com");
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        passStageId,
        templateId,
        902L,
        "지원자B",
        "kpi-b@test.com");

    ApiResponse<KpiResult> response = get(memberFixture, token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    KpiResult data = response.getData();
    assertThat(data.totalApplicants()).isGreaterThanOrEqualTo(2);
    assertThat(data.passCount()).isGreaterThanOrEqualTo(1);
    assertThat(data.totalRecruitments()).isGreaterThanOrEqualTo(1);
  }

  private ApiResponse<KpiResult> get(MemberFixture memberFixture, String token, String tenantId) {
    return memberFixture.base().get("/api/v1/reports/kpi", token, KpiResult.class);
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("kpi-test", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }
}
