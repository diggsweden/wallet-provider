// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.provider.application.model.WalletUnitAttestationDto;

@RequestMapping("/wallet-unit-attestation")
@RestController

public class WalletUnitAttestationController {

  @PostMapping
  public ResponseEntity<String> postWalletUnitAttestation(
      WalletUnitAttestationDto walletUnitAttestationDto) {

    return ResponseEntity.created(URI.create("/users/" + "id")).build();
  }
}
