package com.coffiness.calfit.api.applications.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.ApplicationSummaryResponse;
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
@DisplayName("GET /api/v1/applications")
class GET_specs {

  @Test
  void recruitmentId로_지원서_목록을_조회할_수_있다(
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
    Long templateId = applicationTemplateFixture.createUsedTemplateId(token, tenantId, "list");

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "목록 조회 테스트",
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
                new RecruitmentStageRequest("합격", RecruitmentStageType.PASS, 2)));
    recruitmentFixture.createRecruitment(token, tenantId, request);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> "목록 조회 테스트".equals(item.title()))
            .findFirst()
            .orElseThrow();

    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        recruitment.stages().get(0).id(),
        templateId,
        701L,
        "목록지원자1",
        "list1@test.com");
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        recruitment.stages().get(0).id(),
        templateId,
        702L,
        "목록지원자2",
        "list2@test.com");

    ApiResponse<ApplicationSummaryResponse[]> response =
        getList(memberFixture, token, tenantId, recruitment.id());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).hasSize(2);
  }

  @Test
  void all_파라미터로_전체_지원서를_조회할_수_있다(
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
    Long templateId = applicationTemplateFixture.createUsedTemplateId(token, tenantId, "list-all");

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "전체 조회 테스트",
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
                new RecruitmentStageRequest("합격", RecruitmentStageType.PASS, 2)));
    recruitmentFixture.createRecruitment(token, tenantId, request);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> "전체 조회 테스트".equals(item.title()))
            .findFirst()
            .orElseThrow();

    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        recruitment.stages().get(0).id(),
        templateId,
        703L,
        "전체지원자",
        "list-all@test.com");

    ApiResponse<ApplicationSummaryResponse[]> response = getAllList(memberFixture, token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotEmpty();
  }

  @SuppressWarnings("unchecked")
  private ApiResponse<ApplicationSummaryResponse[]> getList(
      MemberFixture memberFixture, String token, String tenantId, Long recruitmentId) {
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
                "/api/v1/applications?recruitmentId=" + recruitmentId,
                HttpMethod.GET,
                entity,
                ApiResponse.class);
    return convertResponse(memberFixture, response.getBody());
  }

  @SuppressWarnings("unchecked")
  private ApiResponse<ApplicationSummaryResponse[]> getAllList(
      MemberFixture memberFixture, String token, String tenantId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    headers.set("X-Tenant-ID", tenantId);
    HttpEntity<?> entity = new HttpEntity<>(headers);
    ResponseEntity<ApiResponse> response =
        memberFixture
            .base()
            .client()
            .exchange("/api/v1/applications?all=true", HttpMethod.GET, entity, ApiResponse.class);
    return convertResponse(memberFixture, response.getBody());
  }

  @SuppressWarnings("unchecked")
  private ApiResponse<ApplicationSummaryResponse[]> convertResponse(
      MemberFixture memberFixture, ApiResponse<?> response) {
    if (response == null
        || response.getResult() == ResultType.ERROR
        || response.getData() == null) {
      return (ApiResponse<ApplicationSummaryResponse[]>) (ApiResponse<?>) response;
    }
    ApplicationSummaryResponse[] data =
        memberFixture
            .base()
            .objectMapper()
            .convertValue(response.getData(), ApplicationSummaryResponse[].class);
    return ApiResponse.success(data);
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("list-test", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }
}
