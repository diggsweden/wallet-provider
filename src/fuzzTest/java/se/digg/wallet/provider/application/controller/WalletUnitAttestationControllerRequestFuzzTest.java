// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.Optional;
import se.digg.wallet.provider.api.v0.model.WalletUnitAttestationRequest;

/**
 * Fuzz tests for WalletUnitAttestationController. Uses JUnit 5 + Jazzer.
 */
public class WalletUnitAttestationControllerRequestFuzzTest {

  @FuzzTest(maxDuration = "10s")
  public void fuzzRequestConstruction(FuzzedDataProvider data) {
    String jwk = data.consumeString(data.remainingBytes() / 2);
    String nonce = data.consumeRemainingAsString();

    WalletUnitAttestationRequest request = new WalletUnitAttestationRequest();
    request.setJwk(jwk);
    request.setNonce(Optional.ofNullable(nonce));
    request.getJwk();
    request.getNonce();
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzJwkField(FuzzedDataProvider data) {
    WalletUnitAttestationRequest request = new WalletUnitAttestationRequest();
    request.setJwk(data.consumeRemainingAsString());
    request.getJwk();
  }

  @FuzzTest(maxDuration = "10s")
  public void fuzzNonceField(FuzzedDataProvider data) {
    WalletUnitAttestationRequest request = new WalletUnitAttestationRequest();
    request.setNonce(Optional.ofNullable(data.consumeRemainingAsString()));
    request.getNonce();
  }
}
