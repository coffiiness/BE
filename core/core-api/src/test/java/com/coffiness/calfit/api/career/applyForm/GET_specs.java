package com.coffiness.calfit.api.career.applyForm;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.CareerApplyFormResponse;
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
@DisplayName("GET /api/v1/careers/{workspaceId}/recruitments/{recruitmentId}/apply-form")
public class GET_specs {

  @Test
  void OPEN_상태인_채용공고의_지원서_양식을_조회한다(
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
    Long templateId = applicationTemplateFixture.createUsedTemplateId(token, tenantId, "form");

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "지원서 테스트",
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

    Long recruitmentId =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(r -> "지원서 테스트".equals(r.title()))
            .findFirst()
            .orElseThrow()
            .id();
    recruitmentFixture.publishRecruitment(token, tenantId, recruitmentId);

    ApiResponse<CareerApplyFormResponse> response =
        memberFixture
            .base()
            .get(
                "/api/v1/careers/" + tenantId + "/recruitments/" + recruitmentId + "/apply-form",
                CareerApplyFormResponse.class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    CareerApplyFormResponse data = response.getData();
    assertThat(data).isNotNull();
    assertThat(data.recruitmentId()).isEqualTo(recruitmentId);
    assertThat(data.title()).isEqualTo("지원서 테스트");
    assertThat(data.templateId()).isEqualTo(templateId);
    assertThat(data.firstStageId()).isNotNull();
    assertThat(data.customFields()).isNotNull();
  }

  @Test
  void 존재하지_않는_채용공고_ID로_조회하면_에러를_반환한다(@Autowired MemberFixture memberFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();

    ApiResponse<CareerApplyFormResponse> response =
        memberFixture
            .base()
            .get(
                "/api/v1/careers/" + context.workspaceId() + "/recruitments/999999/apply-form",
                CareerApplyFormResponse.class);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("form-test", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }
}
