package com.coffiness.calfit.api.report.recruitment_performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.core.api.facade.report.ReportFacade.RecruitmentPerformanceItem;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/reports/recruitment-performance")
class GET_specs {

  @Test
  void 채용공고_성과_조회에_성공한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture,
      @Autowired ObjectMapper objectMapper) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long templateId = applicationTemplateFixture.createUsedTemplateId(token, tenantId, "perf");

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "성과 테스트 채용",
            3,
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
                new RecruitmentStageRequest("합격", RecruitmentStageType.PASS, 2),
                new RecruitmentStageRequest("불합격", RecruitmentStageType.FAIL, 3)));
    recruitmentFixture.createRecruitment(token, tenantId, request);

    ApiResponse<RecruitmentPerformanceItem[]> response =
        memberFixture
            .base()
            .get(
                "/api/v1/reports/recruitment-performance",
                token,
                RecruitmentPerformanceItem[].class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().length).isGreaterThanOrEqualTo(1);
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("perf-test", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }
}
