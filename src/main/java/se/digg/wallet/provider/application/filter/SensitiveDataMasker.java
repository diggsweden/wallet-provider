// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Utility for masking sensitive data in logs. Masks header values by name and email patterns in
 * body text.
 *
 * <p>
 * NOTE: This masker does NOT detect JWTs, tokens, or other sensitive patterns that may appear in
 * field values. JWTs transmitted as strings will be logged in full. For production, body logging
 * should be disabled to prevent sensitive data leakage.
 */
@Component
public class SensitiveDataMasker {

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${properties.logging-filter.sensitive-data-mask.headers:}")
  private List<String> sensitiveHeaders;

  @Value("${properties.logging-filter.sensitive-data-mask.mask-value:***MASKED***}")
  private String mask;

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

  /**
   * Masks sensitive headers in a map.
   */
  public Map<String, String> maskHeaders(Map<String, String> headers) {

    if (headers == null) {
      return null;
    }

    Map<String, String> maskedHeaders = new HashMap<>();

    headers.forEach((key, value) -> {
      if (sensitiveHeaders.contains(key.toLowerCase(Locale.getDefault()))) {
        // Mask the value but show first few characters
        maskedHeaders.put(key, maskValue(value));
      } else {
        maskedHeaders.put(key, value);
      }
    });

    return maskedHeaders;
  }

  /**
   * Masks sensitive fields in a JSON body.
   */
  public String maskJsonBody(String jsonBody) {
    if (jsonBody == null || jsonBody.isEmpty()) {
      return jsonBody;
    }

    try {
      JsonNode rootNode = objectMapper.readTree(jsonBody);
      maskJsonNode(rootNode);
      return objectMapper.writeValueAsString(rootNode);
    } catch (JacksonException e) {
      // Not valid JSON, apply pattern-based masking
      return maskPatterns(jsonBody);
    }
  }

  /**
   * Recursively masks sensitive fields in a JSON node.
   */
  private void maskJsonNode(JsonNode node) {
    if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      objectNode.properties().forEach(entry -> {
        String fieldName = entry.getKey();
        JsonNode childNode = entry.getValue();

        if (childNode.isObject() || childNode.isArray()) {
          // Recurse into nested structures
          maskJsonNode(childNode);
        } else if (childNode.isTextual()) {
          // Check for patterns in string values
          String value = childNode.asText();
          String maskedValue = maskPatterns(value);
          if (!value.equals(maskedValue)) {
            objectNode.put(fieldName, maskedValue);
          }
        }
      });
    } else if (node.isArray()) {
      node.forEach(this::maskJsonNode);
    }
  }

  /**
   * Applies pattern-based masking to a string.
   */
  private String maskPatterns(String value) {
    // Mask email addresses
    return EMAIL_PATTERN.matcher(value).replaceAll(match -> {
      String email = match.group();
      int atIndex = email.indexOf('@');
      if (atIndex > 2) {
        return String.format("%s%s@%s",
            email.substring(0, 2), mask, email.substring(atIndex + 1));
      }
      return String.format("%s@%s", mask, email.substring(atIndex + 1));
    });
  }

  /**
   * Masks a value showing only first few characters.
   */
  private String maskValue(String value) {
    if (value == null || value.length() <= 4) {
      return mask;
    }
    return value.substring(0, 4) + "..." + mask;
  }
}
