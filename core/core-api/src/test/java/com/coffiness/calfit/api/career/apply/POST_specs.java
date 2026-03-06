package com.coffiness.calfit.api.career.apply;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.BaseFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.api.dto.v1.request.CareerApplicationSubmitRequest;
import com.coffiness.calfit.core.api.dto.v1.response.ApplicantDetailResponse;
import com.coffiness.calfit.core.api.dto.v1.response.ApplicantManagementItemResponse;
import com.coffiness.calfit.core.api.dto.v1.response.CareerApplicationSubmitResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@CalfitApiTest
@DisplayName("POST /api/v1/careers/{workspaceId}/recruitments/{recruitmentId}/apply")
class POST_specs {

  private final Environment environment;
  private final ObjectMapper objectMapper;

  POST_specs(Environment environment, ObjectMapper objectMapper) {
    this.environment = environment;
    this.objectMapper = objectMapper;
  }

  @Test
  void submitApplicationThenVisibleInApplicantManagement(
      @Autowired MemberFixture memberFixture, @Autowired RecruitmentFixture recruitmentFixture) {
    BaseFixture baseFixture = BaseFixture.create(environment, objectMapper);

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String workspaceId = context.workspaceId();

    String recruitmentTitle = "Backend Hiring " + UUID.randomUUID();
    RecruitmentCreateRequest recruitmentCreateRequest =
        new RecruitmentCreateRequest(
            recruitmentTitle,
            2,
            1L,
            "Application flow verification",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(14),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L),
            List.of(1L),
            List.of(
                new RecruitmentStageRequest("Document", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("Interview", RecruitmentStageType.INTERVIEW, 2)));

    ApiResponse<Void> createRecruitmentResponse =
        recruitmentFixture.createRecruitment(token, workspaceId, recruitmentCreateRequest);
    assertThat(createRecruitmentResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    Long recruitmentId =
        findRecruitmentId(recruitmentFixture, token, workspaceId, recruitmentTitle);

    String applicantEmail = "cand-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    CareerApplicationSubmitRequest submitRequest =
        new CareerApplicationSubmitRequest(
            "Candidate Test",
            "MALE",
            LocalDate.of(2000, 1, 1),
            "010-1234-5678",
            applicantEmail,
            "Self introduction",
            "https://github.com/test");

    ApiResponse<CareerApplicationSubmitResponse> submitResponse =
        baseFixture.post(
            "/api/v1/careers/{workspaceId}/recruitments/{recruitmentId}/apply",
            submitRequest,
            CareerApplicationSubmitResponse.class,
            workspaceId,
            recruitmentId);

    assertThat(submitResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(submitResponse.getData()).isNotNull();
    assertThat(submitResponse.getData().applicationId()).isNotNull();

    ApiResponse<ApplicantManagementItemResponse[]> applicantsResponse =
        baseFixture.get(
            "/api/v1/workspaces/{workspaceId}/applicants",
            token,
            ApplicantManagementItemResponse[].class,
            workspaceId);

    assertThat(applicantsResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(applicantsResponse.getData()).isNotNull();
    assertThat(
            Arrays.stream(applicantsResponse.getData())
                .anyMatch(row -> applicantEmail.equals(row.applicantEmail())))
        .isTrue();
  }

  @Test
  void submitByCompanySlugAliasPathWithVirtualJobIdThenVisibleWithoutTenantHeader(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired RecruitmentFixture recruitmentFixture) {
    BaseFixture baseFixture = BaseFixture.create(environment, objectMapper);

    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace =
        workspaceFixture.createWorkspace(token, "NAVER", "010-1111-2222", "11-50").getData();
    String workspaceId = workspace.workspaceId();

    String recruitmentTitle = "Platform Engineer " + UUID.randomUUID();
    RecruitmentCreateRequest recruitmentCreateRequest =
        new RecruitmentCreateRequest(
            recruitmentTitle,
            2,
            1L,
            "Slug/tenant compatibility verification",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(21),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L),
            List.of(1L),
            List.of(
                new RecruitmentStageRequest("Document", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("Interview", RecruitmentStageType.INTERVIEW, 2)));

    ApiResponse<Void> createRecruitmentResponse =
        recruitmentFixture.createRecruitment(token, workspaceId, recruitmentCreateRequest);
    assertThat(createRecruitmentResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    String applicantEmail = "slug-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    CareerApplicationSubmitRequest submitRequest =
        new CareerApplicationSubmitRequest(
            "Slug Candidate",
            "\uB0A8\uC131",
            LocalDate.of(1999, 10, 1),
            "010-2222-3333",
            applicantEmail,
            "Submitted via company slug path",
            "https://github.com/slug-test");

    String companySlug = "NAVER".toLowerCase(Locale.ROOT);
    Long virtualJobId = 1L;
    ApiResponse<CareerApplicationSubmitResponse> submitResponse =
        baseFixture.post(
            "/api/v1/careers/{companySlug}/{recruitmentId}/apply",
            submitRequest,
            CareerApplicationSubmitResponse.class,
            companySlug,
            virtualJobId);

    assertThat(submitResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(submitResponse.getData()).isNotNull();
    assertThat(submitResponse.getData().applicantId()).isNotNull();

    ApiResponse<ApplicantManagementItemResponse[]> applicantsResponse =
        baseFixture.get("/api/v1/applicants", token, ApplicantManagementItemResponse[].class);

    assertThat(applicantsResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(applicantsResponse.getData()).isNotNull();
    assertThat(
            Arrays.stream(applicantsResponse.getData())
                .anyMatch(row -> applicantEmail.equals(row.applicantEmail())))
        .isTrue();
  }

  @Test
  void getApplicantsByWorkspaceShouldFailWhenUserIsNotWorkspaceMember(
      @Autowired UserFixture userFixture, @Autowired MemberFixture memberFixture) {
    BaseFixture baseFixture = BaseFixture.create(environment, objectMapper);

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String workspaceId = context.workspaceId();
    String outsiderToken = userFixture.createUserAndGetToken();

    ApiResponse<ApplicantManagementItemResponse[]> applicantsResponse =
        baseFixture.get(
            "/api/v1/workspaces/{workspaceId}/applicants",
            outsiderToken,
            ApplicantManagementItemResponse[].class,
            workspaceId);

    assertThat(applicantsResponse.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(applicantsResponse.getError()).isNotNull();
    assertThat(applicantsResponse.getError().getCode()).isEqualTo("E401");
  }

  @Test
  void submitTwiceForSameRecruitmentThenBothVisibleInApplicantManagement(
      @Autowired MemberFixture memberFixture, @Autowired RecruitmentFixture recruitmentFixture) {
    BaseFixture baseFixture = BaseFixture.create(environment, objectMapper);

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String workspaceId = context.workspaceId();

    String recruitmentTitle = "Repeated Apply Hiring " + UUID.randomUUID();
    RecruitmentCreateRequest recruitmentCreateRequest =
        new RecruitmentCreateRequest(
            recruitmentTitle,
            2,
            1L,
            "Duplicate apply visibility verification",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(14),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L),
            List.of(1L),
            List.of(
                new RecruitmentStageRequest("Document", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("Interview", RecruitmentStageType.INTERVIEW, 2)));

    ApiResponse<Void> createRecruitmentResponse =
        recruitmentFixture.createRecruitment(token, workspaceId, recruitmentCreateRequest);
    assertThat(createRecruitmentResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    Long recruitmentId =
        findRecruitmentId(recruitmentFixture, token, workspaceId, recruitmentTitle);

    String applicantEmail = "repeat-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    CareerApplicationSubmitRequest firstRequest =
        new CareerApplicationSubmitRequest(
            "test",
            "MALE",
            LocalDate.of(2000, 1, 1),
            "010-0000-1111",
            applicantEmail,
            "first submit",
            "https://github.com/test");

    CareerApplicationSubmitRequest secondRequest =
        new CareerApplicationSubmitRequest(
            "test2",
            "MALE",
            LocalDate.of(2000, 1, 1),
            "010-0000-2222",
            applicantEmail,
            "second submit",
            "https://github.com/test2");

    ApiResponse<CareerApplicationSubmitResponse> firstSubmitResponse =
        baseFixture.post(
            "/api/v1/careers/{workspaceId}/recruitments/{recruitmentId}/apply",
            firstRequest,
            CareerApplicationSubmitResponse.class,
            workspaceId,
            recruitmentId);

    ApiResponse<CareerApplicationSubmitResponse> secondSubmitResponse =
        baseFixture.post(
            "/api/v1/careers/{workspaceId}/recruitments/{recruitmentId}/apply",
            secondRequest,
            CareerApplicationSubmitResponse.class,
            workspaceId,
            recruitmentId);

    assertThat(firstSubmitResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondSubmitResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ApplicantManagementItemResponse[]> applicantsResponse =
        baseFixture.get(
            "/api/v1/workspaces/{workspaceId}/applicants",
            token,
            ApplicantManagementItemResponse[].class,
            workspaceId);

    assertThat(applicantsResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(applicantsResponse.getData()).isNotNull();

    long sameEmailCount =
        Arrays.stream(applicantsResponse.getData())
            .filter(row -> applicantEmail.equals(row.applicantEmail()))
            .count();

    assertThat(sameEmailCount).isEqualTo(2L);
    assertThat(
            Arrays.stream(applicantsResponse.getData())
                .filter(row -> applicantEmail.equals(row.applicantEmail()))
                .map(ApplicantManagementItemResponse::applicantName)
                .toList())
        .contains("test", "test2");
  }

  @Test
  void getApplicantsAliasPathShouldReturnSubmittedApplicant(
      @Autowired MemberFixture memberFixture, @Autowired RecruitmentFixture recruitmentFixture) {
    BaseFixture baseFixture = BaseFixture.create(environment, objectMapper);

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String workspaceId = context.workspaceId();

    String recruitmentTitle = "Alias Path Hiring " + UUID.randomUUID();
    RecruitmentCreateRequest recruitmentCreateRequest =
        new RecruitmentCreateRequest(
            recruitmentTitle,
            2,
            1L,
            "Applicant list alias verification",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(14),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L),
            List.of(1L),
            List.of(
                new RecruitmentStageRequest("Document", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("Interview", RecruitmentStageType.INTERVIEW, 2)));

    ApiResponse<Void> createRecruitmentResponse =
        recruitmentFixture.createRecruitment(token, workspaceId, recruitmentCreateRequest);
    assertThat(createRecruitmentResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    Long recruitmentId =
        findRecruitmentId(recruitmentFixture, token, workspaceId, recruitmentTitle);

    String applicantEmail = "alias-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    CareerApplicationSubmitRequest submitRequest =
        new CareerApplicationSubmitRequest(
            "Alias Candidate",
            "MALE",
            LocalDate.of(2001, 3, 1),
            "010-7777-8888",
            applicantEmail,
            "Submitted for alias list endpoint",
            "https://github.com/alias-test");

    ApiResponse<CareerApplicationSubmitResponse> submitResponse =
        baseFixture.post(
            "/api/v1/careers/{workspaceId}/recruitments/{recruitmentId}/apply",
            submitRequest,
            CareerApplicationSubmitResponse.class,
            workspaceId,
            recruitmentId);

    assertThat(submitResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ApplicantManagementItemResponse[]> applicantsResponse =
        baseFixture.get(
            "/api/v1/recruitment/applicants?search={search}",
            token,
            ApplicantManagementItemResponse[].class,
            "alias");

    assertThat(applicantsResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(applicantsResponse.getData()).isNotNull();
    assertThat(
            Arrays.stream(applicantsResponse.getData())
                .anyMatch(row -> applicantEmail.equals(row.applicantEmail())))
        .isTrue();
  }

  @Test
  void submitApplicationThenDetailContainsSubmittedFields(
      @Autowired MemberFixture memberFixture, @Autowired RecruitmentFixture recruitmentFixture) {
    BaseFixture baseFixture = BaseFixture.create(environment, objectMapper);

    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String workspaceId = context.workspaceId();

    String recruitmentTitle = "Detail Hiring " + UUID.randomUUID();
    RecruitmentCreateRequest recruitmentCreateRequest =
        new RecruitmentCreateRequest(
            recruitmentTitle,
            2,
            1L,
            "Applicant detail verification",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(14),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(1L),
            List.of(1L),
            List.of(
                new RecruitmentStageRequest("Document", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("Interview", RecruitmentStageType.INTERVIEW, 2)));

    ApiResponse<Void> createRecruitmentResponse =
        recruitmentFixture.createRecruitment(token, workspaceId, recruitmentCreateRequest);
    assertThat(createRecruitmentResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    Long recruitmentId =
        findRecruitmentId(recruitmentFixture, token, workspaceId, recruitmentTitle);

    String applicantEmail = "detail-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    CareerApplicationSubmitRequest submitRequest =
        new CareerApplicationSubmitRequest(
            "Detail Candidate",
            "MALE",
            LocalDate.of(2000, 5, 20),
            "010-5555-6666",
            applicantEmail,
            "detail-short-bio",
            "https://github.com/detail-test");

    ApiResponse<CareerApplicationSubmitResponse> submitResponse =
        baseFixture.post(
            "/api/v1/careers/{workspaceId}/recruitments/{recruitmentId}/apply",
            submitRequest,
            CareerApplicationSubmitResponse.class,
            workspaceId,
            recruitmentId);

    assertThat(submitResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(submitResponse.getData()).isNotNull();

    Long applicationId = submitResponse.getData().applicationId();
    Long applicantId = submitResponse.getData().applicantId();

    ApiResponse<ApplicantDetailResponse> detailByApplicationResponse =
        baseFixture.get(
            "/api/v1/applicants/{detailId}", token, ApplicantDetailResponse.class, applicationId);

    assertThat(detailByApplicationResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(detailByApplicationResponse.getData()).isNotNull();
    assertThat(detailByApplicationResponse.getData().email()).isEqualTo(applicantEmail);
    assertThat(detailByApplicationResponse.getData().name()).isEqualTo("Detail Candidate");
    assertThat(detailByApplicationResponse.getData().gender()).isEqualTo("MALE");
    assertThat(detailByApplicationResponse.getData().shortBio()).isEqualTo("detail-short-bio");
    assertThat(detailByApplicationResponse.getData().portfolioUrl())
        .isEqualTo("https://github.com/detail-test");

    ApiResponse<ApplicantDetailResponse> detailByApplicantResponse =
        baseFixture.get(
            "/api/v1/applicants/{detailId}", token, ApplicantDetailResponse.class, applicantId);

    assertThat(detailByApplicantResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(detailByApplicantResponse.getData()).isNotNull();
    assertThat(detailByApplicantResponse.getData().email()).isEqualTo(applicantEmail);
  }

  private Long findRecruitmentId(
      RecruitmentFixture recruitmentFixture, String token, String workspaceId, String title) {
    ApiResponse<List<RecruitmentListResponse>> recruitmentListResponse =
        recruitmentFixture.getRecruitmentList(token, workspaceId);

    return recruitmentListResponse.getData().stream()
        .filter(row -> title.equals(row.title()))
        .map(RecruitmentListResponse::id)
        .findFirst()
        .orElseThrow();
  }
}
