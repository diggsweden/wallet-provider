// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.provider.application.model.WalletUnitAttestationDto;
import se.digg.wallet.provider.application.service.WalletUnitAttestationService;

@RequestMapping("/wallet-unit-attestation")
@RestController
public class WalletUnitAttestationController {

  private final WalletUnitAttestationService attestationService;

  public WalletUnitAttestationController(WalletUnitAttestationService attestationService) {
    this.attestationService = attestationService;
  }

  @PostMapping
  public ResponseEntity<String> postWalletUnitAttestation(
      @RequestBody WalletUnitAttestationDto walletUnitAttestationDto) throws Exception {
    SignedJWT signedJwt =
        attestationService.createWalletUnitAttestation(walletUnitAttestationDto.jwk());
    return ResponseEntity.ok(signedJwt.serialize());
  }
}
