package com.coffiness.calfit.api.v1.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ApplicationTemplateCreateRequestValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void shouldFailWhenNameIsBlank() {
    ApplicationTemplateCreateRequest request =
        new ApplicationTemplateCreateRequest(" ", "{}", false);

    var violations = validator.validate(request);

    assertThat(violations).anyMatch(v -> "name".equals(v.getPropertyPath().toString()));
  }

  @Test
  void shouldPassWhenRequiredFieldsExist() {
    ApplicationTemplateCreateRequest request =
        new ApplicationTemplateCreateRequest("Screening Template", "{\"fields\":[]}", true);

    var violations = validator.validate(request);

    assertThat(violations).isEmpty();
  }
}
