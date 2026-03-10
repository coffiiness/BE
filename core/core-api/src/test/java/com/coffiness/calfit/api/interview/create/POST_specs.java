package com.coffiness.calfit.api.interview.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.InterviewFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.core.enums.InterviewRound;
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
@DisplayName("POST /api/v1/interviews")
class POST_specs {

  @Test
  void 토큰이_없으면_생성에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired InterviewFixture interviewFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String tenantId = context.workspaceId();

    ApiResponse<InterviewResponse> response =
        interviewFixture.create(
            null,
            tenantId,
            1L,
            1L,
            InterviewRound.FIRST,
            List.of(1L),
            List.of(1L),
            1L,
            LocalDateTime.now().plusDays(1),
            60,
            "memo");

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
