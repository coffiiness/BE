package com.coffiness.calfit.api.recruitment.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/recruitments")
public class GET_specs {

  @Test
  void 채용공고_목록_조회에_성공한다(
      @Autowired MemberFixture memberFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired UserFixture userFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long interviewerId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    List<RecruitmentStageRequest> stages1 =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    List<RecruitmentStageRequest> stages2 =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("처우 협의", RecruitmentStageType.OFFER, 3),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 4));

    RecruitmentCreateRequest request1 =
        new RecruitmentCreateRequest(
            "시니어 백엔드 채용",
            2,
            applicationTemplateId,
            "경력직 모집합니다",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            5,
            10,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            stages1);
    RecruitmentCreateRequest request2 =
        new RecruitmentCreateRequest(
            "신입 프론트엔드 채용",
            3,
            applicationTemplateId,
            "신입 모집합니다.",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.NEW,
            null,
            null,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            stages2);

    ApiResponse<Void> createResponse1 =
        recruitmentFixture.createRecruitment(token, tenantId, request1);
    ApiResponse<Void> createResponse2 =
        recruitmentFixture.createRecruitment(token, tenantId, request2);

    assertThat(createResponse1.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(createResponse2.getResult()).isEqualTo(ResultType.SUCCESS);

    // Act
    ApiResponse<List<RecruitmentListResponse>> response =
        recruitmentFixture.getRecruitmentList(token, tenantId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData())
        .extracting(RecruitmentListResponse::title)
        .contains("시니어 백엔드 채용", "신입 프론트엔드 채용");
  }

  @Test
  void 채용공고_목록_조회시_전형_단계별_지원자_수가_함께_조회된다(
      @Autowired MemberFixture memberFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired UserFixture userFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateRepository applicationTemplateRepository) {

    // Arrange
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();
    Long interviewerId = userFixture.me(token).getData().id();
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId = createApplicationTemplate(tenantId, applicationTemplateRepository);

    List<RecruitmentStageRequest> stages =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            "단계별 지원자 수 확인용 채용",
            2,
            applicationTemplateId,
            "경력직 모집합니다",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            List.of(),
            List.of(interviewerId),
            stages);

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> item.title().equals("단계별 지원자 수 확인용 채용"))
            .findFirst()
            .orElseThrow();

    Long documentStageId = recruitment.stages().get(0).id();
    Long interviewStageId = recruitment.stages().get(1).id();

    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        documentStageId,
        applicationTemplateId,
        1001L,
        "지원자 하나",
        "applicant1001@coffiness.com");
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        documentStageId,
        applicationTemplateId,
        1002L,
        "지원자 둘",
        "applicant1002@coffiness.com");
    InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitment.id(),
        interviewStageId,
        applicationTemplateId,
        1003L,
        "지원자 셋",
        "applicant1003@coffiness.com");

    // Act
    RecruitmentListResponse response =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> item.id().equals(recruitment.id()))
            .findFirst()
            .orElseThrow();

    // Assert
    assertThat(response.stages())
        .anySatisfy(
            stage -> {
              if ("서류 전형".equals(stage.stageName())) {
                assertThat(stage.applicantCount()).isEqualTo(2);
              }
            })
        .anySatisfy(
            stage -> {
              if ("실무 면접".equals(stage.stageName())) {
                assertThat(stage.applicantCount()).isEqualTo(1);
              }
            })
        .anySatisfy(
            stage -> {
              if ("최종 합격".equals(stage.stageName())) {
                assertThat(stage.applicantCount()).isEqualTo(0);
              }
            });
  }

  // 현재 tenant에서 사용할 채용 담당 그룹 ID를 조회
  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(() -> groupRepository.save(GroupEntity.create("목록 조회 테스트 그룹", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  // 현재 tenant에서 사용할 지원서 템플릿을 생성
  private Long createApplicationTemplate(
      String tenantId, ApplicationTemplateRepository applicationTemplateRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return applicationTemplateRepository
          .save(ApplicationTemplateEntity.create("목록 조회 테스트 템플릿", "{}", true))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }
}
