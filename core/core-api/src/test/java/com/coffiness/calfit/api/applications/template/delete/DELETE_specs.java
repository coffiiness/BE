package com.coffiness.calfit.api.applications.template.delete;

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
@DisplayName("DELETE /api/v1/workspaces/{workspaceId}/application-templates/{templateId}")
class DELETE_specs {

  @Test
  void 채용공고에_사용되지_않는_템플릿을_삭제할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {
    WorkspaceContext workspace = memberFixture.setupWorkspace();

    ApiResponse<ApplicationTemplateListResponse> created =
        applicationTemplateFixture.create(
            workspace.hrToken(),
            workspace.workspaceId(),
            applicationTemplateFixture.newCreateRequest(
                "delete-test",
                List.of(Map.of("key", "url", "label", "URL", "type", "TEXT")),
                false));
    Long templateId = created.getData().id();

    ApiResponse<Void> response =
        applicationTemplateFixture.delete(workspace.hrToken(), workspace.workspaceId(), templateId);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    // 삭제 후 목록에서 사라지는지 확인
    ApiResponse<List<ApplicationTemplateListResponse>> list =
        applicationTemplateFixture.list(workspace.hrToken(), workspace.workspaceId());
    assertThat(list.getData().stream().noneMatch(t -> t.id().equals(templateId))).isTrue();
  }

  @Test
  void 존재하지_않는_템플릿을_삭제하면_에러를_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {
    WorkspaceContext workspace = memberFixture.setupWorkspace();

    ApiResponse<Void> response =
        applicationTemplateFixture.delete(workspace.hrToken(), workspace.workspaceId(), 999999L);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
