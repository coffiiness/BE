package com.coffiness.calfit.api.applicants.signup;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicantFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/workspaces/{workspaceId}/applicants/signup")
public class POST_specs {

  @Test
  void 지원자_회원가입에_성공한다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    String email = applicantFixture.randomEmail();
    String password = applicantFixture.randomPassword();
    String name = applicantFixture.randomName();

    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(workspaceId, email, password, name);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().email()).isEqualTo(email);
    assertThat(response.getData().workspaceId()).isEqualTo(workspaceId);
  }

  @Test
  void duplicateEmailSignupFails(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    String email = "dup@test.com";
    String password = applicantFixture.randomPassword();

    ApiResponse<ApplicantResponse> firstResponse =
        applicantFixture.signUp(workspaceId, email, password, "first");
    ApiResponse<ApplicantResponse> secondResponse =
        applicantFixture.signUp(workspaceId, email, password, "second");

    assertThat(firstResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondResponse.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(secondResponse.getError()).isNotNull();
  }

  @Test
  void invalidEmailSignupFails(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(
            workspaceId, "invalid-email", applicantFixture.randomPassword(), "applicant");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void blankPasswordSignupFails(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(workspaceId, "blank-pass@test.com", "", "applicant");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void blankNameSignupFails(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired ApplicantFixture applicantFixture) {
    String hrToken = userFixture.createUserAndGetToken();
    ApiResponse<WorkspaceResponse> workspace = workspaceFixture.createWorkspace(hrToken);
    String workspaceId = workspace.getData().workspaceId();

    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(
            workspaceId, "blank-name@test.com", applicantFixture.randomPassword(), "");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }

  @Test
  void signupFailsWhenWorkspaceDoesNotExist(@Autowired ApplicantFixture applicantFixture) {
    ApiResponse<ApplicantResponse> response =
        applicantFixture.signUp(
            "workspace-not-found",
            "missing-workspace@test.com",
            applicantFixture.randomPassword(),
            "applicant");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
  }
}
