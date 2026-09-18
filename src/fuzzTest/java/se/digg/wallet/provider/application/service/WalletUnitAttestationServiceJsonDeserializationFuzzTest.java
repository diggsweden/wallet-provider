// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Fuzz tests for JSON deserialization in WalletUnitAttestationService. Uses JUnit 5 + Jazzer.
 */
public class WalletUnitAttestationServiceJsonDeserializationFuzzTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @FuzzTest(maxDuration = "10s")
  public void fuzzJsonDeserialization(FuzzedDataProvider data) {
    try {
      objectMapper.readValue(
          data.consumeRemainingAsString(), new TypeReference<Map<String, Object>>() {});
    } catch (JacksonException e) {
      // Ignore - expected parse failure, fuzzing for coverage
    }
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzStringDeserialization(FuzzedDataProvider data) {
    try {
      objectMapper.readValue(data.consumeRemainingAsString(), String.class);
    } catch (JacksonException e) {
      // Ignore - expected parse failure, fuzzing for coverage
    }
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzObjectDeserialization(FuzzedDataProvider data) {
    try {
      objectMapper.readValue(data.consumeRemainingAsString(), Object.class);
    } catch (JacksonException e) {
      // Ignore - expected parse failure, fuzzing for coverage
    }
  }
}
