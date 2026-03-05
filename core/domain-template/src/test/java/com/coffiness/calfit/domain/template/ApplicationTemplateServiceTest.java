package com.coffiness.calfit.domain.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.coffiness.calfit.api.v1.request.ApplicationTemplateCreateRequest;
import com.coffiness.calfit.api.v1.request.ApplicationTemplateUpdateRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationTemplateServiceTest {

  @Mock private ApplicationTemplateReader applicationTemplateReader;
  @Mock private ApplicationTemplateStore applicationTemplateStore;
  @Mock private TemplateSchemaValidator templateSchemaValidator;

  @InjectMocks private ApplicationTemplateService applicationTemplateService;

  @Test
  void shouldCreateTemplate() {
    ApplicationTemplateCreateRequest request =
        new ApplicationTemplateCreateRequest("Backend Template", "{\"fields\":[]}", true);
    ApplicationTemplate saved = new ApplicationTemplate(1L, request.name(), request.schema(), true);

    given(applicationTemplateStore.store(any(ApplicationTemplate.class))).willReturn(saved);

    ApplicationTemplate result = applicationTemplateService.createTemplate(10L, request);

    verify(templateSchemaValidator).validate(request.schema());
    ArgumentCaptor<ApplicationTemplate> captor = ArgumentCaptor.forClass(ApplicationTemplate.class);
    verify(applicationTemplateStore).store(captor.capture());
    assertThat(captor.getValue().name()).isEqualTo(request.name());
    assertThat(captor.getValue().schema()).isEqualTo(request.schema());
    assertThat(captor.getValue().isDefault()).isTrue();
    assertThat(result.id()).isEqualTo(1L);
  }

  @Test
  void shouldUpdateTemplate() {
    Long templateId = 3L;
    ApplicationTemplate current = new ApplicationTemplate(templateId, "Old", "{}", false);
    ApplicationTemplateUpdateRequest request =
        new ApplicationTemplateUpdateRequest("New", "{\"fields\":[1]}", true);
    ApplicationTemplate updated =
        new ApplicationTemplate(templateId, request.name(), request.schema(), request.isDefault());

    given(applicationTemplateReader.read(templateId)).willReturn(current);
    given(applicationTemplateStore.update(any(ApplicationTemplate.class))).willReturn(updated);

    ApplicationTemplate result =
        applicationTemplateService.updateTemplate(templateId, 10L, request);

    verify(templateSchemaValidator).validate(request.schema());
    ArgumentCaptor<ApplicationTemplate> captor = ArgumentCaptor.forClass(ApplicationTemplate.class);
    verify(applicationTemplateStore).update(captor.capture());
    assertThat(captor.getValue().id()).isEqualTo(templateId);
    assertThat(captor.getValue().name()).isEqualTo("New");
    assertThat(result.schema()).isEqualTo("{\"fields\":[1]}");
  }

  @Test
  void shouldReadTemplates() {
    List<ApplicationTemplate> templates = List.of(new ApplicationTemplate(1L, "A", "{}", false));
    given(applicationTemplateReader.readAll()).willReturn(templates);

    List<ApplicationTemplate> result = applicationTemplateService.getTemplates(10L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("A");
  }

  @Test
  void shouldDeleteTemplate() {
    applicationTemplateService.deleteTemplate(99L, 10L);

    verify(applicationTemplateStore).delete(99L);
  }
}
