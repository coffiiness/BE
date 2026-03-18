package com.coffiness.calfit.api.applications.stage;

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
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.application.KanbanBoard;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.automation.ApplicationProcessHistoryEntity;
import com.coffiness.calfit.storage.db.core.automation.ApplicationProcessHistoryRepository;
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
@DisplayName("PATCH /api/v1/applications/{applicationId}/stage")
class PATCH_specs {

  @Test
  void HR는_지원자의_단계를_변경할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired ApplicationProcessHistoryRepository applicationProcessHistoryRepository,
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
            "stage-move-" + System.nanoTime(),
            hrUserId,
            groupRepository,
            applicationTemplateFixture);

    Long applicationId =
        createPendingApplication(
            tenantId,
            applicationRepository,
            recruitment.recruitmentId(),
            recruitment.documentStageId(),
            recruitment.applicationTemplateId(),
            "Move Candidate");

    ApiResponse<ApplicationDetailResponse> response =
        applicationFixture.updateApplicationStage(
            hrToken, tenantId, applicationId, recruitment.interviewStageId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().recruitmentProcessId()).isEqualTo(recruitment.interviewStageId());
    assertThat(
            readApplication(tenantId, applicationRepository, applicationId)
                .getRecruitmentProcessId())
        .isEqualTo(recruitment.interviewStageId());

    List<ApplicationProcessHistoryEntity> histories =
        readHistories(tenantId, applicationProcessHistoryRepository, applicationId);
    assertThat(histories)
        .singleElement()
        .satisfies(
            history -> {
              assertThat(history.getFromRecruitmentProcessId())
                  .isEqualTo(recruitment.documentStageId());
              assertThat(history.getToRecruitmentProcessId())
                  .isEqualTo(recruitment.interviewStageId());
              assertThat(history.getActorId()).isEqualTo(hrUserId);
            });

    ApiResponse<KanbanBoard> boardResponse =
        applicationFixture.getKanbanBoard(hrToken, tenantId, recruitment.recruitmentId());
    assertThat(boardResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(boardResponse.getData().columns())
        .anySatisfy(
            column -> {
              if (column.recruitmentProcessId().equals(recruitment.documentStageId())) {
                assertThat(column.applications()).isEmpty();
              }
            })
        .anySatisfy(
            column -> {
              if (column.recruitmentProcessId().equals(recruitment.interviewStageId())) {
                assertThat(column.applications())
                    .extracting(application -> application.applicationId())
                    .containsExactly(applicationId);
              }
            });
  }

  @Test
  void 면접관은_지원자의_단계를_변경할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired ApplicationProcessHistoryRepository applicationProcessHistoryRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();
    MemberAccessContext interviewer =
        inviteInterviewer(memberFixture, userFixture, hrToken, tenantId, "stage-interviewer");

    RecruitmentContext recruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "stage-auth-" + System.nanoTime(),
            hrUserId,
            groupRepository,
            applicationTemplateFixture);

    Long applicationId =
        createPendingApplication(
            tenantId,
            applicationRepository,
            recruitment.recruitmentId(),
            recruitment.documentStageId(),
            recruitment.applicationTemplateId(),
            "Unauthorized Candidate");

    ApiResponse<ApplicationDetailResponse> response =
        applicationFixture.updateApplicationStage(
            interviewer.token(), tenantId, applicationId, recruitment.interviewStageId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(
            readApplication(tenantId, applicationRepository, applicationId)
                .getRecruitmentProcessId())
        .isEqualTo(recruitment.documentStageId());
    assertThat(readHistories(tenantId, applicationProcessHistoryRepository, applicationId))
        .isEmpty();
  }

  @Test
  void 다른_채용의_단계로는_지원자를_이동할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired ApplicationProcessHistoryRepository applicationProcessHistoryRepository,
      @Autowired GroupRepository groupRepository,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String hrToken = context.hrToken();
    String tenantId = context.workspaceId();
    Long hrUserId = userFixture.me(hrToken).getData().id();

    RecruitmentContext firstRecruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "stage-source-" + System.nanoTime(),
            hrUserId,
            groupRepository,
            applicationTemplateFixture);
    RecruitmentContext secondRecruitment =
        createRecruitment(
            recruitmentFixture,
            hrToken,
            tenantId,
            "stage-target-" + System.nanoTime(),
            hrUserId,
            groupRepository,
            applicationTemplateFixture);

    Long applicationId =
        createPendingApplication(
            tenantId,
            applicationRepository,
            firstRecruitment.recruitmentId(),
            firstRecruitment.documentStageId(),
            firstRecruitment.applicationTemplateId(),
            "Cross Recruitment Candidate");

    ApiResponse<ApplicationDetailResponse> response =
        applicationFixture.updateApplicationStage(
            hrToken, tenantId, applicationId, secondRecruitment.interviewStageId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(
            readApplication(tenantId, applicationRepository, applicationId)
                .getRecruitmentProcessId())
        .isEqualTo(firstRecruitment.documentStageId());
    assertThat(readHistories(tenantId, applicationProcessHistoryRepository, applicationId))
        .isEmpty();
  }

  @Test
  void 같은_단계로_이동하면_히스토리가_쌓이지_않는다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired RecruitmentFixture recruitmentFixture,
      @Autowired ApplicationFixture applicationFixture,
      @Autowired ApplicationRepository applicationRepository,
      @Autowired ApplicationProcessHistoryRepository applicationProcessHistoryRepository,
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
            "stage-noop-" + System.nanoTime(),
            hrUserId,
            groupRepository,
            applicationTemplateFixture);

    Long applicationId =
        createPendingApplication(
            tenantId,
            applicationRepository,
            recruitment.recruitmentId(),
            recruitment.documentStageId(),
            recruitment.applicationTemplateId(),
            "Noop Candidate");

    ApiResponse<ApplicationDetailResponse> response =
        applicationFixture.updateApplicationStage(
            hrToken, tenantId, applicationId, recruitment.documentStageId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().recruitmentProcessId()).isEqualTo(recruitment.documentStageId());
    assertThat(readHistories(tenantId, applicationProcessHistoryRepository, applicationId))
        .isEmpty();
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
        applicationTemplateFixture.createUsedTemplateId(token, tenantId, "applications-stage");

    RecruitmentCreateRequest request =
        new RecruitmentCreateRequest(
            title,
            2,
            applicationTemplateId,
            "stage test recruitment",
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
        recruitment.stages().get(1).id());
  }

  private MemberAccessContext inviteInterviewer(
      MemberFixture memberFixture,
      UserFixture userFixture,
      String hrToken,
      String tenantId,
      String name) {
    String email = userFixture.randomEmail();
    String password = userFixture.randomPassword();
    userFixture.signUp(email, password, name);
    String token = userFixture.login(email, password).getData().accessToken();

    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(hrToken, tenantId, email, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), token);

    return new MemberAccessContext(token);
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

  private ApplicationEntity readApplication(
      String tenantId, ApplicationRepository applicationRepository, Long applicationId) {
    TenantContext.setTenantId(tenantId);
    try {
      return applicationRepository
          .findByIdAndStatus(applicationId, EntityStatus.ACTIVE)
          .orElseThrow();
    } finally {
      TenantContext.clear();
    }
  }

  private List<ApplicationProcessHistoryEntity> readHistories(
      String tenantId,
      ApplicationProcessHistoryRepository applicationProcessHistoryRepository,
      Long applicationId) {
    TenantContext.setTenantId(tenantId);
    try {
      return applicationProcessHistoryRepository.findByApplicationId(applicationId);
    } finally {
      TenantContext.clear();
    }
  }

  private Long findLeadGroupId(String tenantId, GroupRepository groupRepository) {
    TenantContext.setTenantId(tenantId);
    try {
      return groupRepository.findByStatus(EntityStatus.ACTIVE).stream()
          .findFirst()
          .orElseGet(
              () -> groupRepository.save(GroupEntity.create("applications-stage", "#14B8A6")))
          .getId();
    } finally {
      TenantContext.clear();
    }
  }

  private record RecruitmentContext(
      Long recruitmentId,
      Long applicationTemplateId,
      Long documentStageId,
      Long interviewStageId) {}

  private record MemberAccessContext(String token) {}
}
