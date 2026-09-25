// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.jwk.ECKey;
import java.text.ParseException;
import org.junit.jupiter.api.Test;

class EcKeyParserTest {

  @Test
  void parseMethodAvoidsLibraryNullPointerException() {
    // Canary for a nimbus-jose-jwt bug (first assertion) that EcKeyParser.parse() works around
    // (second assertion): parsing the JSON literal "null" throws an unchecked
    // NullPointerException instead of the documented ParseException. If the first assertion
    // ever fails, the library has fixed it upstream, and EcKeyParser/its callers can go back to
    // calling ECKey.parse(String) directly.
    String jsonLiteralNull = "null";

    assertThrows(NullPointerException.class, () -> ECKey.parse(jsonLiteralNull));
    assertThrows(ParseException.class, () -> EcKeyParser.parse(jsonLiteralNull));
  }
}
