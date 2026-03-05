package com.coffiness.calfit.domain.template;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import org.junit.jupiter.api.Test;

class TemplateSchemaValidatorTest {

  private final TemplateSchemaValidator templateSchemaValidator = new TemplateSchemaValidator();

  @Test
  void shouldPassWhenSchemaIsValid() {
    String schema =
        "{\"fields\":[{\"key\":\"resume\",\"label\":\"Resume\",\"type\":\"FILE\",\"required\":true,\"order\":1},{\"key\":\"careerLevel\",\"label\":\"Career Level\",\"type\":\"SELECT\",\"required\":true,\"order\":2,\"options\":[{\"value\":\"junior\",\"label\":\"Junior\"},{\"value\":\"senior\",\"label\":\"Senior\"}]}]}";

    templateSchemaValidator.validate(schema);
  }

  @Test
  void shouldFailWhenFieldsArrayMissing() {
    assertValidationError("{}", "fields array");
  }

  @Test
  void shouldFailWhenFieldKeyIsDuplicated() {
    String schema =
        "{\"fields\":[{\"key\":\"resume\",\"label\":\"Resume\",\"type\":\"FILE\",\"required\":true,\"order\":1},{\"key\":\"resume\",\"label\":\"Resume 2\",\"type\":\"TEXT\",\"required\":false,\"order\":2}]}";

    assertValidationError(schema, "Duplicate field key");
  }

  @Test
  void shouldFailWhenChoiceFieldHasNoOptions() {
    String schema =
        "{\"fields\":[{\"key\":\"careerLevel\",\"label\":\"Career Level\",\"type\":\"SELECT\",\"required\":true,\"order\":1}]}";

    assertValidationError(schema, "options must be a non-empty array");
  }

  @Test
  void shouldFailWhenNonChoiceFieldHasOptions() {
    String schema =
        "{\"fields\":[{\"key\":\"resume\",\"label\":\"Resume\",\"type\":\"FILE\",\"required\":true,\"order\":1,\"options\":[\"A\"]}]}";

    assertValidationError(schema, "options are only allowed for choice fields");
  }

  private void assertValidationError(String schema, String containsMessage) {
    assertThatThrownBy(() -> templateSchemaValidator.validate(schema))
        .isInstanceOf(CoreException.class)
        .extracting(error -> ((CoreException) error).getErrorType())
        .isEqualTo(ErrorType.VALIDATION_ERROR);

    assertThatThrownBy(() -> templateSchemaValidator.validate(schema))
        .isInstanceOf(CoreException.class)
        .hasMessageContaining("Invalid request.")
        .extracting(error -> ((CoreException) error).getData())
        .asString()
        .contains(containsMessage);
  }
}
