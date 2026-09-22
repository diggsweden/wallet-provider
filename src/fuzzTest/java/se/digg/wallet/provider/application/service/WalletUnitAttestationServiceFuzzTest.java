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
 * Fuzz tests for {@link WalletUnitAttestationService#createWalletUnitAttestation}, the actual
 * end-to-end orchestration entry point (claims/attestedKeys/JOSE header construction). Uses the
 * same test keystore as {@code WalletUnitAttestationServiceTest}
 * (src/test/resources/certificates/wallet-provider.p12), built directly rather than via
 * {@code @SpringBootTest}: Jazzer's fuzzing mode manages its own test instance lifecycle, and every
 * other fuzz test in this suite avoids depending on a Spring context for the same reason. Uses
 * JUnit 5 + Jazzer.
 */
public class WalletUnitAttestationServiceFuzzTest {

  private WalletUnitAttestationService service;

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
    service = new WalletUnitAttestationService(keystoreProperties, new ObjectMapper());
  }

  @FuzzTest
  public void fuzzCreateWalletUnitAttestation(FuzzedDataProvider data) {
    String jwk = data.consumeString(data.remainingBytes() / 2);
    String nonce = data.consumeRemainingAsString();

    try {
      service.createWalletUnitAttestation(jwk, nonce);
    } catch (WalletRuntimeException e) {
      // Expected wrapper for malformed jwk/nonce input - see
      // WalletUnitAttestationService#createWalletUnitAttestation. Any other, unwrapped exception
      // type surfacing here is itself a regression of that method's exception handling.
    }
  }
}
