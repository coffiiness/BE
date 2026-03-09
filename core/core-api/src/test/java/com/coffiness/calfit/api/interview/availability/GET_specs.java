package com.coffiness.calfit.api.interview.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.response.InterviewAvailabilityResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/interviews/availability")
class GET_specs {

  @Test
  void 토큰이_없으면_조회에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String tenantId = context.workspaceId();

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availability(
            null, tenantId, LocalDateTime.now().plusDays(1), List.of(), List.of());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void from_파라미터가_없으면_조회에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<InterviewAvailabilityResponse> response =
        interviewFixture.availabilityWithoutFrom(token, tenantId);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
