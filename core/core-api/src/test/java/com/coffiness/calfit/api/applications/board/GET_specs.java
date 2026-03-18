package com.coffiness.calfit.api.applications.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicantFixture;
import com.coffiness.calfit.api.fixture.ApplicationFixture;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.support.InterviewApplicantTestHelper;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.ApplicantLoginResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.application.KanbanBoard;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/applications/board")
class GET_specs {

  @Test
  void recruitmentId로_칸반_보드를_조회할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();

    RecruitmentContext primaryRecruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "board-main-" + System.nanoTime(),
            hrUserId,
            groupRepository,
            applicationTemplateFixture);

    RecruitmentContext secondaryRecruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "board-other-" + System.nanoTime(),
            hrUserId,
            groupRepository,
            applicationTemplateFixture);

    createPendingApplication(
        tenantId,
        applicationRepository,
        primaryRecruitment.recruitmentId(),
        primaryRecruitment.documentStageId(),
        primaryRecruitment.applicationTemplateId(),
        "Kim Document");
    createPendingApplication(
        tenantId,
        applicationRepository,
        primaryRecruitment.recruitmentId(),
        primaryRecruitment.interviewStageId(),
        primaryRecruitment.applicationTemplateId(),
        "Lee Interview");
    createPendingApplication(
        tenantId,
        applicationRepository,
        secondaryRecruitment.recruitmentId(),
        secondaryRecruitment.documentStageId(),
        secondaryRecruitment.applicationTemplateId(),
        "Park Other");

    ApiResponse<KanbanBoard> response =
        applicationFixture.getKanbanBoard(hrToken, tenantId, primaryRecruitment.recruitmentId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().columns())
        .extracting(column -> column.stageType())
        .contains(
            RecruitmentStageType.DOCUMENT,
            RecruitmentStageType.INTERVIEW,
            RecruitmentStageType.PASS,
            RecruitmentStageType.FAIL);

    assertThat(response.getData().columns())
        .anySatisfy(
            column -> {
              if (column.recruitmentProcessId().equals(primaryRecruitment.documentStageId())) {
                assertThat(column.stageType()).isEqualTo(RecruitmentStageType.DOCUMENT);
                assertThat(column.applications())
                    .extracting(app -> app.name())
                    .containsExactly("Kim Document");
              }
            })
        .anySatisfy(
            column -> {
              if (column.recruitmentProcessId().equals(primaryRecruitment.interviewStageId())) {
                assertThat(column.stageType()).isEqualTo(RecruitmentStageType.INTERVIEW);
                assertThat(column.applications())
                    .extracting(app -> app.name())
                    .containsExactly("Lee Interview");
              }
            })
        .anySatisfy(
            column -> {
              if (column.recruitmentProcessId().equals(primaryRecruitment.passStageId())) {
                assertThat(column.stageType()).isEqualTo(RecruitmentStageType.PASS);
                assertThat(column.applications()).isEmpty();
              }
            });
  }

  @Test
  void 지원자는_칸반_보드를_조회할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicantFixture applicantFixture,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "board-auth-" + System.nanoTime(),
            hrUserId,
            groupRepository,
            applicationTemplateFixture);

    String email = applicantFixture.randomEmail();
    String password = applicantFixture.randomPassword();
    applicantFixture.signUp(tenantId, email, password, applicantFixture.randomName());
    ApiResponse<ApplicantLoginResponse> loginResponse =
        applicantFixture.login(tenantId, email, password);

    ApiResponse<KanbanBoard> response =
        applicationFixture.getKanbanBoard(
            loginResponse.getData().accessToken(), tenantId, recruitment.recruitmentId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  private RecruitmentContext createRecruitment(
      RecruitmentFixture recruitmentFixture,
      String token,
      String tenantId,
      String title,
      Long interviewerUserId,
      GroupRepository groupRepository,
      ApplicationTemplateFixture applicationTemplateFixture) {
    Long leadGroupId = findLeadGroupId(tenantId, groupRepository);
    Long applicationTemplateId =
        applicationTemplateFixture.createUsedTemplateId(token, tenantId, "applications-board");

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            title,
            2,
            applicationTemplateId,
            "board test recruitment",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            3,
            7,
            leadGroupId,
            List.of(),
            List.of(interviewerUserId),
            List.of(
                new RecruitmentStageRequest("Document", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("Interview", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("Pass", RecruitmentStageType.PASS, 3)));

    ApiResponse<Void> createResponse =
        recruitmentFixture.createRecruitment(token, tenantId, request);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    RecruitmentListResponse recruitment =
        recruitmentFixture.getRecruitmentList(token, tenantId).getData().stream()
            .filter(item -> title.equals(item.title()))
            .findFirst()
            .orElseThrow();

    return new RecruitmentContext(
        recruitment.id(),
        applicationTemplateId,
        recruitment.stages().get(0).id(),
        recruitment.stages().get(1).id(),
        recruitment.stages().get(recruitment.stages().size() - 1).id());
  }

  private Long createPendingApplication(
      String tenantId,
      ApplicationRepository applicationRepository,
      Long recruitmentId,
      Long recruitmentStageId,
      Long templateId,
      String name) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    return InterviewApplicantTestHelper.createPendingApplication(
        tenantId,
        applicationRepository,
        recruitmentId,
        recruitmentStageId,
        templateId,
        System.nanoTime(),
        name,
        "app-" + suffix + "@test.com");
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(
              () -> groupRepository.save(GroupEntity.create("applications-board", "#3B82F6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private record RecruitmentContext(
      Long recruitmentId,
      Long applicationTemplateId,
      Long documentStageId,
      Long interviewStageId,
      Long passStageId) {}
}
