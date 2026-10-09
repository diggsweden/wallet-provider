// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.provider.api.v0.WalletInstanceAttestationApi;
import se.digg.wallet.provider.api.v0.model.WalletInstanceAttestationRequest;
import se.digg.wallet.provider.api.v0.model.WalletInstanceAttestationResponse;
import se.digg.wallet.provider.application.service.WalletInstanceAttestationService;

@RestController
public class WalletInstanceAttestationController implements WalletInstanceAttestationApi {

  private final WalletInstanceAttestationService attestationService;

  public WalletInstanceAttestationController(WalletInstanceAttestationService attestationService) {
    this.attestationService = attestationService;
  }

  @Override
  public ResponseEntity<WalletInstanceAttestationResponse> postWalletInstanceAttestation(
      WalletInstanceAttestationRequest request) {
    var jwt = attestationService.createWalletInstanceAttestation(request.getJwk());
    return ResponseEntity.ok(new WalletInstanceAttestationResponse(jwt.serialize()));
  }
}
