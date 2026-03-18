package com.coffiness.calfit.api.applications.template.update;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@CalfitApiTest
@DisplayName("PUT /api/v1/application-templates/{templateId}")
class PUT_specs {

  @Test
  void 인사담당자는_템플릿을_수정할_수_있다(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {
    WorkspaceContext workspace = memberFixture.setupWorkspace();

    ApiResponse<ApplicationTemplateListResponse> created =
        applicationTemplateFixture.create(
            workspace.hrToken(),
            workspace.workspaceId(),
            applicationTemplateFixture.newCreateRequest(
                "before-update",
                List.of(Map.of("key", "url", "label", "URL", "type", "TEXT")),
                true));
    Long templateId = created.getData().id();

    Map<String, Object> updateRequest =
        applicationTemplateFixture.newCreateRequest(
            "after-update",
            List.of(Map.of("key", "github", "label", "GitHub", "type", "TEXT")),
            true);

    ApiResponse<ApplicationTemplateListResponse> response =
        exchangeWithTenant(
            memberFixture,
            "/api/v1/application-templates/" + templateId,
            HttpMethod.PUT,
            updateRequest,
            workspace.hrToken(),
            workspace.workspaceId(),
            ApplicationTemplateListResponse.class);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().name()).isEqualTo("after-update");
    assertThat(response.getData().customFields()).contains("github");
  }

  @Test
  void 존재하지_않는_템플릿을_수정하면_에러를_반환한다(
      @Autowired MemberFixture memberFixture,
      @Autowired ApplicationTemplateFixture applicationTemplateFixture) {
    WorkspaceContext workspace = memberFixture.setupWorkspace();

    Map<String, Object> updateRequest =
        applicationTemplateFixture.newCreateRequest("non-existent", List.of(), true);

    ApiResponse<ApplicationTemplateListResponse> response =
        exchangeWithTenant(
            memberFixture,
            "/api/v1/application-templates/999999",
            HttpMethod.PUT,
            updateRequest,
            workspace.hrToken(),
            workspace.workspaceId(),
            ApplicationTemplateListResponse.class);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> exchangeWithTenant(
      MemberFixture memberFixture,
      String url,
      HttpMethod method,
      Object body,
      String token,
      String tenantId,
      Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    headers.set("X-Tenant-ID", tenantId);
    HttpEntity<?> entity =
        body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

    ResponseEntity<ApiResponse> response =
        memberFixture.base().client().exchange(url, method, entity, ApiResponse.class);

    ApiResponse<?> apiResponse = response.getBody();
    if (apiResponse == null) return null;
    if (apiResponse.getResult() == ResultType.ERROR || apiResponse.getData() == null) {
      return (ApiResponse<T>) apiResponse;
    }
    T converted =
        memberFixture.base().objectMapper().convertValue(apiResponse.getData(), responseType);
    return ApiResponse.success(converted);
  }
}
