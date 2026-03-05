package com.coffiness.calfit.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.template.ApplicationTemplate;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationFormTemplateRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApplicationTemplateReaderImplTest {

  @Mock private ApplicationFormTemplateRepository applicationFormTemplateRepository;

  @InjectMocks private ApplicationTemplateReaderImpl applicationTemplateReader;

  @Test
  void shouldReadAllActiveTemplates() {
    ApplicationFormTemplateEntity first =
        ApplicationFormTemplateEntity.builder().name("A").schema("{}").isDefault(false).build();
    ReflectionTestUtils.setField(first, "id", 1L);

    given(
            applicationFormTemplateRepository.findAllByStatusOrderByCreatedAtDesc(
                EntityStatus.ACTIVE))
        .willReturn(List.of(first));

    List<ApplicationTemplate> result = applicationTemplateReader.readAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(1L);
  }

  @Test
  void shouldReadTemplateById() {
    ApplicationFormTemplateEntity entity =
        ApplicationFormTemplateEntity.builder().name("A").schema("{}").isDefault(true).build();
    ReflectionTestUtils.setField(entity, "id", 7L);

    given(applicationFormTemplateRepository.findByIdAndStatus(7L, EntityStatus.ACTIVE))
        .willReturn(Optional.of(entity));

    ApplicationTemplate result = applicationTemplateReader.read(7L);

    assertThat(result.name()).isEqualTo("A");
    assertThat(result.isDefault()).isTrue();
  }
}
