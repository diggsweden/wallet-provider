// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import java.text.ParseException;
import java.util.Base64;

/**
 * Fuzz tests for JWT/token parsing. Uses JUnit 5 + Jazzer.
 */
public class TokenParsingFuzzTest {

  @FuzzTest(maxDuration = "10s")
  public void fuzzEcKeyParsing(FuzzedDataProvider data) {
    String jwkJson = data.consumeRemainingAsString();
    try {
      if (!jwkJson.isEmpty()) {
        ECKey.parse(jwkJson);
      }
    } catch (ParseException | NullPointerException e) {
      // NullPointerException: known nimbus-jose-jwt quirk (JSONObjectUtils.getGeneric) where
      // certain malformed input reaches JWKMetadata.parseKeyType() as a null map instead of
      // raising ParseException. Callers that pass attacker-controlled JWK strings must catch this
      // too - see WalletUnitAttestationService#createWalletUnitAttestation. Not re-thrown here so
      // fuzzing keeps exploring past this already-triaged finding.
    }
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzRsaKeyParsing(FuzzedDataProvider data) {
    String jwkJson = data.consumeRemainingAsString();
    try {
      if (!jwkJson.isEmpty()) {
        RSAKey.parse(jwkJson);
      }
    } catch (ParseException | NullPointerException e) {
      // See fuzzEcKeyParsing() - RSAKey.parse() shares the same JWKMetadata.parseKeyType() path.
    }
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzJwtParsing(FuzzedDataProvider data) {
    String jwtToken = data.consumeRemainingAsString();
    try {
      if (!jwtToken.isEmpty()) {
        com.nimbusds.jwt.SignedJWT.parse(jwtToken);
      }
    } catch (ParseException e) {
      // Ignore - fuzzing for coverage
    }
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzBase64EncodedJwk(FuzzedDataProvider data) {
    String base64Input = data.consumeRemainingAsString();
    try {
      if (!base64Input.isEmpty()) {
        byte[] decoded = Base64.getUrlDecoder().decode(base64Input);
        String jwkJson = new String(decoded);
        ECKey.parse(jwkJson);
      }
    } catch (ParseException | IllegalArgumentException | NullPointerException e) {
      // See fuzzEcKeyParsing() - shares the same JWKMetadata.parseKeyType() path.
    }
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzMalformedJwkJson(FuzzedDataProvider data) {
    String malformedJson = data.consumeRemainingAsString();
    try {
      ECKey.parse(malformedJson);
    } catch (ParseException | NullPointerException e) {
      // See fuzzEcKeyParsing() - shares the same JWKMetadata.parseKeyType() path.
    }
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzLongJwkFields(FuzzedDataProvider data) {
    String baseValue = data.consumeRemainingAsString();
    try {
      String longValue = baseValue.repeat(100);
      String jwkJson = String.format(
          "{\"kty\":\"EC\",\"kid\":\"%s\",\"crv\":\"P-256\",\"x\":\"%s\",\"y\":\"%s\"}",
          longValue, longValue, longValue);
      ECKey.parse(jwkJson);
    } catch (ParseException | NullPointerException e) {
      // See fuzzEcKeyParsing() - shares the same JWKMetadata.parseKeyType() path.
    }
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzEmptyJwkFields(FuzzedDataProvider data) {
    String fieldValue = data.consumeRemainingAsString();
    try {
      String jwkJson = "{\"kty\":\"EC\",\"kid\":\"" + fieldValue + "\",\"crv\":\"P-256\"}";
      ECKey.parse(jwkJson);
    } catch (ParseException | NullPointerException e) {
      // See fuzzEcKeyParsing() - shares the same JWKMetadata.parseKeyType() path.
    }
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzJwtLengthVariations(FuzzedDataProvider data) {
    String baseToken = data.consumeRemainingAsString();
    try {
      String shortToken = baseToken.substring(0, Math.min(10, baseToken.length()));
      String longToken = baseToken.repeat(50);
      if (!shortToken.isEmpty()) {
        com.nimbusds.jwt.SignedJWT.parse(shortToken);
      }
      if (longToken.length() < 10000) {
        com.nimbusds.jwt.SignedJWT.parse(longToken);
      }
    } catch (ParseException e) {
      // Ignore - fuzzing for coverage
    }
  }
}
