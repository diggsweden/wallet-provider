// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import tools.jackson.databind.ObjectMapper;

/**
 * Fuzz tests for {@link SensitiveDataMasker}, which parses and pattern-matches attacker-controlled
 * request/response body text (when body logging is enabled) and header values. {@code maskJsonBody}
 * recursively walks attacker-supplied JSON and applies the {@code EMAIL_PATTERN} regex to every
 * string value - the kind of nested-quantifier regex (a {@code .} inside a character class
 * immediately before a literal {@code \.}) that is a classic ReDoS candidate. Jazzer's built-in
 * {@code RegexRoadblocks} sanitizer instruments the regex engine and flags catastrophic
 * backtracking automatically, without needing an explicit timeout assertion here. Uses JUnit 5 +
 * Jazzer.
 */
public class SensitiveDataMaskerFuzzTest {

  private SensitiveDataMasker masker;

  @BeforeEach
  void setUp() throws ReflectiveOperationException {
    masker = new SensitiveDataMasker();
    setField("objectMapper", new ObjectMapper());
    setField("sensitiveHeaders", List.of("authorization", "x-api-key", "cookie"));
    setField("mask", "***MASKED***");
  }

  private void setField(String name, Object value) throws ReflectiveOperationException {
    Field field = SensitiveDataMasker.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(masker, value);
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzMaskJsonBody(FuzzedDataProvider data) {
    masker.maskJsonBody(data.consumeRemainingAsString());
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzMaskHeaders(FuzzedDataProvider data) {
    String headerName = data.consumeString(data.remainingBytes() / 2);
    String headerValue = data.consumeRemainingAsString();

    Map<String, String> input = Map.of(headerName, headerValue);
    Map<String, String> result = masker.maskHeaders(input);

    assertEquals(input.size(), result.size(), "maskHeaders must not add or drop header entries");
  }
}
