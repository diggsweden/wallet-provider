// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.nimbusds.jose.jwk.ECKey;
import java.text.ParseException;

/**
 * Fuzz tests for JWK parsing in WalletUnitAttestationService. Focus: JWK parsing via ECKey.parse()
 * - main attack surface. Uses JUnit 5 + Jazzer.
 */
public class WalletUnitAttestationServiceJwkParsingFuzzTest {

  /**
   * Tests direct calls to ECKey.parse() - main attack surface. Tests all types of JWK input: null,
   * empty string, invalid format, wrong curve, malformed Base64, etc. Fuzzing for coverage - all
   * inputs are OK, we just want to test many different paths.
   */
  @FuzzTest(maxDuration = "10s")
  public void fuzzJwkParsingCritical(FuzzedDataProvider data) {
    try {
      ECKey.parse(data.consumeRemainingAsString());
    } catch (ParseException | NullPointerException e) {
      // NullPointerException: known nimbus-jose-jwt quirk, see
      // TokenParsingFuzzTest#fuzzEcKeyParsing and
      // WalletUnitAttestationService#createWalletUnitAttestation for the mitigation.
    }
  }

  /**
   * Tests JWK parsing with varied inputs. Fuzzing for coverage.
   */
  @FuzzTest(maxDuration = "10s")
  public void fuzzJwkParsingVaried(FuzzedDataProvider data) {
    try {
      ECKey.parse(data.consumeRemainingAsString());
    } catch (ParseException | NullPointerException e) {
      // See fuzzJwkParsingCritical().
    }
  }
}
