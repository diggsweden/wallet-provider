// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.provider.api.v0.KeyAttestationApi;
import se.digg.wallet.provider.api.v0.model.KeyAttestationRequest;
import se.digg.wallet.provider.api.v0.model.KeyAttestationResponse;
import se.digg.wallet.provider.application.service.KeyAttestationService;

@RestController
public class KeyAttestationController implements KeyAttestationApi {

  private final KeyAttestationService attestationService;

  public KeyAttestationController(KeyAttestationService attestationService) {
    this.attestationService = attestationService;
  }

  @Override
  public ResponseEntity<KeyAttestationResponse> postKeyAttestation(
      KeyAttestationRequest keyAttestationRequest) {
    SignedJWT signedJwt =
        attestationService.createKeyAttestation(
            keyAttestationRequest.getJwks(),
            keyAttestationRequest.getNonce().orElse(null));
    return ResponseEntity.ok(new KeyAttestationResponse(signedJwt.serialize()));
  }
}
