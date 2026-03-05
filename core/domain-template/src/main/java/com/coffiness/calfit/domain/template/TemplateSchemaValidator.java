package com.coffiness.calfit.domain.template;

import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TemplateSchemaValidator {

  private static final Set<String> SUPPORTED_TYPES =
      Set.of(
          "TEXT",
          "TEXTAREA",
          "NUMBER",
          "EMAIL",
          "PHONE",
          "DATE",
          "FILE",
          "SELECT",
          "RADIO",
          "CHECKBOX");
  private static final Set<String> CHOICE_TYPES = Set.of("SELECT", "RADIO", "CHECKBOX");
  private static final int MAX_FIELDS = 50;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public void validate(String schema) {
    if (schema == null || schema.isBlank()) {
      throwValidation("Template schema is required.");
    }

    JsonNode root = parse(schema);
    if (!root.isObject()) {
      throwValidation("Template schema root must be a JSON object.");
    }

    JsonNode fieldsNode = root.get("fields");
    if (fieldsNode == null || !fieldsNode.isArray()) {
      throwValidation("Template schema must include a fields array.");
    }
    if (fieldsNode.isEmpty()) {
      throwValidation("Template schema must include at least one field.");
    }
    if (fieldsNode.size() > MAX_FIELDS) {
      throwValidation("Template schema supports up to 50 fields.");
    }

    Set<String> fieldKeys = new HashSet<>();
    Set<Integer> fieldOrders = new HashSet<>();

    for (int i = 0; i < fieldsNode.size(); i++) {
      JsonNode fieldNode = fieldsNode.get(i);
      validateField(fieldNode, i, fieldKeys, fieldOrders);
    }
  }

  private void validateField(
      JsonNode fieldNode, int fieldIndex, Set<String> fieldKeys, Set<Integer> fieldOrders) {
    int index = fieldIndex + 1;

    if (fieldNode == null || !fieldNode.isObject()) {
      throwValidation("Field " + index + " must be a JSON object.");
    }

    String key = readRequiredText(fieldNode, "key", index);
    if (!key.matches("^[A-Za-z][A-Za-z0-9_]{1,49}$")) {
      throwValidation("Field " + index + " key format is invalid.");
    }
    if (!fieldKeys.add(key)) {
      throwValidation("Duplicate field key: " + key);
    }

    String label = readRequiredText(fieldNode, "label", index);
    if (label.length() > 100) {
      throwValidation("Field " + index + " label must be at most 100 characters.");
    }

    String type = readRequiredText(fieldNode, "type", index).toUpperCase(Locale.ROOT);
    if (!SUPPORTED_TYPES.contains(type)) {
      throwValidation("Field " + index + " type is not supported.");
    }

    JsonNode requiredNode = fieldNode.get("required");
    if (requiredNode == null || !requiredNode.isBoolean()) {
      throwValidation("Field " + index + " required must be boolean.");
    }

    JsonNode orderNode = fieldNode.get("order");
    if (orderNode == null || !orderNode.canConvertToInt()) {
      throwValidation("Field " + index + " order must be integer.");
    }
    int order = orderNode.asInt();
    if (order < 1) {
      throwValidation("Field " + index + " order must be greater than 0.");
    }
    if (!fieldOrders.add(order)) {
      throwValidation("Duplicate field order: " + order);
    }

    JsonNode optionsNode = fieldNode.get("options");
    if (CHOICE_TYPES.contains(type)) {
      validateChoiceOptions(optionsNode, index);
    } else if (optionsNode != null && optionsNode.isArray() && !optionsNode.isEmpty()) {
      throwValidation("Field " + index + " options are only allowed for choice fields.");
    }
  }

  private void validateChoiceOptions(JsonNode optionsNode, int fieldIndex) {
    if (optionsNode == null || !optionsNode.isArray() || optionsNode.isEmpty()) {
      throwValidation("Field " + fieldIndex + " options must be a non-empty array.");
    }

    Set<String> optionValues = new HashSet<>();

    for (int i = 0; i < optionsNode.size(); i++) {
      JsonNode optionNode = optionsNode.get(i);
      String optionValue = extractOptionValue(optionNode, fieldIndex, i + 1);
      if (!optionValues.add(optionValue)) {
        throwValidation("Field " + fieldIndex + " has duplicate option value: " + optionValue);
      }
    }
  }

  private String extractOptionValue(JsonNode optionNode, int fieldIndex, int optionIndex) {
    if (optionNode == null) {
      throwValidation("Field " + fieldIndex + " option " + optionIndex + " is invalid.");
    }

    if (optionNode.isTextual()) {
      String value = optionNode.asText().trim();
      if (value.isEmpty()) {
        throwValidation("Field " + fieldIndex + " option " + optionIndex + " is blank.");
      }
      return value;
    }

    if (optionNode.isObject()) {
      String value = readRequiredText(optionNode, "value", fieldIndex);
      String label = readRequiredText(optionNode, "label", fieldIndex);
      if (label.length() > 100) {
        throwValidation("Field " + fieldIndex + " option " + optionIndex + " label is too long.");
      }
      return value;
    }

    throwValidation("Field " + fieldIndex + " option " + optionIndex + " is invalid.");
    return "";
  }

  private String readRequiredText(JsonNode node, String propertyName, int fieldIndex) {
    JsonNode valueNode = node.get(propertyName);
    if (valueNode == null || !valueNode.isTextual() || valueNode.asText().isBlank()) {
      throwValidation("Field " + fieldIndex + " " + propertyName + " is required.");
    }
    return valueNode.asText().trim();
  }

  private JsonNode parse(String schema) {
    try {
      return objectMapper.readTree(schema);
    } catch (JsonProcessingException e) {
      throwValidation("Template schema must be valid JSON.");
      return null;
    }
  }

  private void throwValidation(String reason) {
    throw new CoreException(ErrorType.VALIDATION_ERROR, reason);
  }
}
