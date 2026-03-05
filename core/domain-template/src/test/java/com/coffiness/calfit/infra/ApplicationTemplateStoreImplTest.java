package com.coffiness.calfit.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.template.ApplicationTemplate;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApplicationTemplateStoreImplTest {

  @Mock private ApplicationFormTemplateRepository applicationFormTemplateRepository;

  @InjectMocks private ApplicationTemplateStoreImpl applicationTemplateStore;

  @Test
  void shouldStoreTemplate() {
    ApplicationTemplate template = new ApplicationTemplate(null, "Template", "{}", false);

    ApplicationFormTemplateEntity savedEntity =
        ApplicationFormTemplateEntity.builder()
            .name("Template")
            .schema("{}")
            .isDefault(false)
            .build();
    ReflectionTestUtils.setField(savedEntity, "id", 101L);

    given(applicationFormTemplateRepository.save(any(ApplicationFormTemplateEntity.class)))
        .willReturn(savedEntity);

    ApplicationTemplate result = applicationTemplateStore.store(template);

    ArgumentCaptor<ApplicationFormTemplateEntity> captor =
        ArgumentCaptor.forClass(ApplicationFormTemplateEntity.class);
    verify(applicationFormTemplateRepository).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("Template");
    assertThat(result.id()).isEqualTo(101L);
  }

  @Test
  void shouldUpdateTemplate() {
    ApplicationFormTemplateEntity entity =
        ApplicationFormTemplateEntity.builder().name("Old").schema("{}").isDefault(false).build();
    ReflectionTestUtils.setField(entity, "id", 3L);

    given(applicationFormTemplateRepository.findByIdAndStatus(3L, EntityStatus.ACTIVE))
        .willReturn(java.util.Optional.of(entity));

    ApplicationTemplate result =
        applicationTemplateStore.update(new ApplicationTemplate(3L, "New", "{1}", true));

    assertThat(entity.getName()).isEqualTo("New");
    assertThat(entity.getSchema()).isEqualTo("{1}");
    assertThat(result.isDefault()).isTrue();
  }

  @Test
  void shouldSoftDeleteTemplate() {
    ApplicationFormTemplateEntity entity =
        ApplicationFormTemplateEntity.builder().name("N").schema("{}").isDefault(false).build();
    ReflectionTestUtils.setField(entity, "id", 9L);

    given(applicationFormTemplateRepository.findByIdAndStatus(9L, EntityStatus.ACTIVE))
        .willReturn(java.util.Optional.of(entity));

    applicationTemplateStore.delete(9L);

    assertThat(entity.isActive()).isFalse();
  }
}
