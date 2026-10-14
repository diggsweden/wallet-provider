// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.attestation.issuer.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.attestation.issuer.api.v0.WalletInstanceAttestationApi;
import se.digg.wallet.attestation.issuer.api.v0.model.WalletInstanceAttestationRequestDto;
import se.digg.wallet.attestation.issuer.api.v0.model.WalletInstanceAttestationResponseDto;
import se.digg.wallet.attestation.issuer.domain.service.WalletInstanceAttestationService;

@RestController
public class WalletInstanceAttestationController implements WalletInstanceAttestationApi {

  private final WalletInstanceAttestationService attestationService;

  public WalletInstanceAttestationController(WalletInstanceAttestationService attestationService) {
    this.attestationService = attestationService;
  }

  @Override
  public ResponseEntity<WalletInstanceAttestationResponseDto> postWalletInstanceAttestation(
      WalletInstanceAttestationRequestDto request) {
    var jwt = attestationService.createWalletInstanceAttestation(request.getJwk());
    return ResponseEntity.ok(new WalletInstanceAttestationResponseDto(jwt.serialize()));
  }
}
