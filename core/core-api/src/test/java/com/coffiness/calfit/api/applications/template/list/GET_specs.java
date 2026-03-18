package com.coffiness.calfit.api.applications.template.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationTemplateFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.MemberFixture.WorkspaceContext;
import com.coffiness.calfit.api.v1.response.ApplicationTemplateListResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/application-templates")
class GET_specs {

  @Test
  void 템플릿_목록을_조회할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {
    WorkspaceContext workspace = memberFixture.setupWorkspace();
    applicationTemplateFixture.create(
        workspace.hrToken(),
        workspace.workspaceId(),
        applicationTemplateFixture.newCreateRequest(
            "list-test-template",
            List.of(Map.of("key", "url", "label", "URL", "type", "TEXT")),
            true));

    ApiResponse<List<ApplicationTemplateListResponse>> response =
        applicationTemplateFixture.list(workspace.hrToken(), workspace.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotEmpty();
    assertThat(response.getData().stream().anyMatch(t -> t.name().equals("list-test-template")))
        .isTrue();
  }
}
