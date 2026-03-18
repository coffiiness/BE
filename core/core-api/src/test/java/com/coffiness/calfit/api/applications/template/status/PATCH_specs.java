package com.coffiness.calfit.api.applications.template.status;

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
@DisplayName("PATCH /api/v1/workspaces/{workspaceId}/application-templates/{templateId}/status")
class PATCH_specs {

  @Test
  void 템플릿_사용_상태를_변경할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {
    WorkspaceContext workspace = memberFixture.setupWorkspace();

    ApiResponse<ApplicationTemplateListResponse> created =
        applicationTemplateFixture.create(
            workspace.hrToken(),
            workspace.workspaceId(),
            applicationTemplateFixture.newCreateRequest(
                "status-test",
                List.of(Map.of("key", "url", "label", "URL", "type", "TEXT")),
                true));
    Long templateId = created.getData().id();
    assertThat(created.getData().used()).isTrue();

    ApiResponse<ApplicationTemplateListResponse> response =
        applicationTemplateFixture.updateStatus(
            workspace.hrToken(),
            workspace.workspaceId(),
            templateId,
            applicationTemplateFixture.newStatusRequest(false));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().used()).isFalse();
    assertThat(response.getData().status()).isEqualTo("미사용");
  }

  @Test
  void 사용중으로_변경하면_상태가_사용중이_된다(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {
    WorkspaceContext workspace = memberFixture.setupWorkspace();

    ApiResponse<ApplicationTemplateListResponse> created =
        applicationTemplateFixture.create(
            workspace.hrToken(),
            workspace.workspaceId(),
            applicationTemplateFixture.newCreateRequest(
                "status-test2",
                List.of(Map.of("key", "url", "label", "URL", "type", "TEXT")),
                false));
    Long templateId = created.getData().id();

    ApiResponse<ApplicationTemplateListResponse> response =
        applicationTemplateFixture.updateStatus(
            workspace.hrToken(),
            workspace.workspaceId(),
            templateId,
            applicationTemplateFixture.newStatusRequest(true));

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().used()).isTrue();
    assertThat(response.getData().status()).isEqualTo("사용중");
  }
}
