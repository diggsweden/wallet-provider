// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.provider.api.v0.WalletUnitAttestationApi;
import se.digg.wallet.provider.api.v0.model.WalletUnitAttestationRequest;
import se.digg.wallet.provider.application.service.WalletUnitAttestationService;

@RestController
public class WalletUnitAttestationController implements WalletUnitAttestationApi {

  private final WalletUnitAttestationService attestationService;

  public WalletUnitAttestationController(WalletUnitAttestationService attestationService) {
    this.attestationService = attestationService;
  }

  @Override
  public ResponseEntity<String> postWalletUnitAttestation(
      WalletUnitAttestationRequest walletUnitAttestationRequest) {
    SignedJWT signedJwt =
        attestationService.createWalletUnitAttestation(walletUnitAttestationRequest.getJwk(),
            walletUnitAttestationRequest.getNonce().orElse(null));
    return ResponseEntity.ok(signedJwt.serialize());
  }
}
