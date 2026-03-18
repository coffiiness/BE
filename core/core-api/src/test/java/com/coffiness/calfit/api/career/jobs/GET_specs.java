package com.coffiness.calfit.api.career.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.CareerRecruitmentResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/careers/{workspaceId}")
public class GET_specs {

  @Test
  void 워크스페이스의_채용공고_목록을_조회한다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long templateId = applicationTemplateFixture.createUsedTemplateId(token, tenantId, "jobs");

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "Jobs 테스트 채용",
            2,
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
    recruitmentFixture.publishRecruitment(
        token,
        tenantId,
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(r -> "Jobs 테스트 채용".equals(r.title()))
            .findFirst()
            .orElseThrow()
            .id());

    ApiResponse<CareerRecruitmentResponse[]> response =
        memberFixture.base().get("/api/v1/careers/" + tenantId, CareerRecruitmentResponse[].class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().length).isGreaterThanOrEqualTo(1);
  }

  @Test
  void search_파라미터로_채용공고를_검색할_수_있다(@Autowired MemberFixture memberFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<CareerRecruitmentResponse[]> response =
        memberFixture
            .base()
            .get(
                "/api/v1/careers/" + context.workspaceId() + "?search=nonexistent999",
                CareerRecruitmentResponse[].class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEmpty();
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("jobs-test", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }
}
