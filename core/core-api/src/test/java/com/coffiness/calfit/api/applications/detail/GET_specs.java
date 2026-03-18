package com.coffiness.calfit.api.applications.detail;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationFixture;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.ApplicationDetailResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@CalfitApiTest
@DisplayName("GET /api/v1/applications/{applicationId}")
class GET_specs {

  @Test
  void HR은_지원서_상세를_조회할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long templateId = applicationTemplateFixture.createUsedTemplateId(token, tenantId, "detail");

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "상세 조회 테스트",
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
                new RecruitmentStageRequest("합격", RecruitmentStageType.PASS, 2)));
    recruitmentFixture.createRecruitment(token, tenantId, request);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> "상세 조회 테스트".equals(item.title()))
            .findFirst()
            .orElseThrow();

    Long applicationId =
        InterviewApplicantTestHelper.createPendingApplication(
            tenantId,
            applicationRepository,
            recruitment.id(),
            recruitment.stages().get(0).id(),
            templateId,
            801L,
            "상세지원자",
            "detail@test.com");

    ApiResponse<ApplicationDetailResponse> response =
        getDetail(memberFixture, token, tenantId, applicationId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    ApplicationDetailResponse data = response.getData();
    assertThat(data).isNotNull();
    assertThat(data.name()).isEqualTo("상세지원자");
    assertThat(data.email()).isEqualTo("detail@test.com");
    assertThat(data.applicationId()).isEqualTo(applicationId);
  }

  @SuppressWarnings("unchecked")
  private ApiResponse<ApplicationDetailResponse> getDetail(
      MemberFixture memberFixture, String token, String tenantId, Long applicationId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    headers.set("X-Tenant-ID", tenantId);
    HttpEntity<?> entity = new HttpEntity<>(headers);
    ResponseEntity<ApiResponse> response =
        memberFixture
            .base()
            .client()
            .exchange(
                "/api/v1/applications/" + applicationId, HttpMethod.GET, entity, ApiResponse.class);
    if (response.getBody() == null
        || response.getBody().getResult() == ResultType.ERROR
        || response.getBody().getData() == null) {
      return (ApiResponse<ApplicationDetailResponse>) (ApiResponse<?>) response.getBody();
    }
    ApplicationDetailResponse data =
        memberFixture
            .base()
            .objectMapper()
            .convertValue(response.getBody().getData(), ApplicationDetailResponse.class);
    return ApiResponse.success(data);
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("detail-test", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }
}
