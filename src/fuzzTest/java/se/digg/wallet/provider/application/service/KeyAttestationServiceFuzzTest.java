// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;
import se.digg.wallet.provider.application.service.exception.WalletRuntimeException;
import tools.jackson.databind.ObjectMapper;

/**
 * Fuzz tests for {@link KeyAttestationService#createKeyAttestation(String, String)}, the entry
 * point that parses client-supplied JWKs. Uses the same test keystore as
 * {@link WalletUnitAttestationServiceFuzzTest}. Uses JUnit 5 + Jazzer.
 */
public class KeyAttestationServiceFuzzTest {

  private KeyAttestationService service;

  @BeforeEach
  void setUp() {
    WuaKeystoreProperties keystoreProperties = new WuaKeystoreProperties(
        new ClassPathResource("certificates/wallet-provider.p12"),
        "secret",
        "wallet-provider",
        "PKCS12",
        "{\"status_list\": {\"idx\": 412,\"uri\": \"https://revocation_url/statuslists/1\"}}",
        24,
        "Digg");
    service = new KeyAttestationService(keystoreProperties, new ObjectMapper());
  }

  @FuzzTest
  public void fuzzCreateKeyAttestation(FuzzedDataProvider data) {
    String jwk = data.consumeString(data.remainingBytes() / 2);
    String nonce = data.consumeRemainingAsString();

    try {
      service.createKeyAttestation(jwk, nonce);
    } catch (WalletRuntimeException e) {
      // Expected wrapper for malformed jwk/nonce input. Any other, unwrapped exception type
      // surfacing here is a regression of the method's exception handling.
    }
  }
}
