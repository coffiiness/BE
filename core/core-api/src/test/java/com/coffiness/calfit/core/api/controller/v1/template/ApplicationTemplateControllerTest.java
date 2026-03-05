package com.coffiness.calfit.core.api.controller.v1.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.coffiness.calfit.api.v1.request.ApplicationTemplateCreateRequest;
import com.coffiness.calfit.api.v1.response.ApplicationTemplateResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.template.ApplicationTemplate;
import com.coffiness.calfit.domain.template.ApplicationTemplateService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationTemplateControllerTest {

  @Mock private ApplicationTemplateService applicationTemplateService;

  @InjectMocks private ApplicationTemplateController applicationTemplateController;

  @Test
  void shouldCreateTemplate() {
    SecurityUser user = new SecurityUser(1L, "user@test.com", "USER");
    ApplicationTemplateCreateRequest request =
        new ApplicationTemplateCreateRequest("Resume Template", "{}", false);

    given(applicationTemplateService.createTemplate(1L, request))
        .willReturn(new ApplicationTemplate(5L, "Resume Template", "{}", false));

    ApiResponse<ApplicationTemplateResponse> response =
        applicationTemplateController.createTemplate(user, request);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().id()).isEqualTo(5L);
  }

  @Test
  void shouldGetTemplates() {
    SecurityUser user = new SecurityUser(1L, "user@test.com", "USER");
    given(applicationTemplateService.getTemplates(1L))
        .willReturn(List.of(new ApplicationTemplate(3L, "Form", "{}", true)));

    ApiResponse<List<ApplicationTemplateResponse>> response =
        applicationTemplateController.getTemplates(user);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).hasSize(1);
    assertThat(response.getData().get(0).name()).isEqualTo("Form");
  }
}
