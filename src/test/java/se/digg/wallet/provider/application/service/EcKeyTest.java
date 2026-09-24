// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.Test;

class EcKeyTest {

  @Test
  void throwsNullPointerExceptionInsteadOfParseExceptionWhenParsingTheStringNull() {
    // Canary for a nimbus-jose-jwt bug that EcKeyUtils.parse() works around: parsing the JSON
    // literal "null" throws an unchecked NullPointerException instead of the documented
    // ParseException. If this check ever fails, the library has fixed it upstream, and
    // EcKeyUtils/its callers can go back to calling ECKey.parse(String) directly.
    assertThrows(NullPointerException.class, () -> ECKey.parse("null"));
  }
}
