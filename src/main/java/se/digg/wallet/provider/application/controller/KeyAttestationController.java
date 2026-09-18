// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import com.nimbusds.jwt.SignedJWT;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.provider.api.v0.KeyAttestationApi;
import se.digg.wallet.provider.api.v0.model.KeyAttestationItem;
import se.digg.wallet.provider.api.v0.model.KeyAttestationRequest;
import se.digg.wallet.provider.api.v0.model.KeyAttestationResponse;
import se.digg.wallet.provider.application.service.KeyAttestationService;
import se.digg.wallet.provider.application.service.exception.InvalidKeyAttestationRequestParameterException;

@RestController
public class KeyAttestationController implements KeyAttestationApi {

  private final KeyAttestationService attestationService;

  public KeyAttestationController(KeyAttestationService attestationService) {
    this.attestationService = attestationService;
  }

  @Override
  public ResponseEntity<KeyAttestationResponse> postKeyAttestation(
      KeyAttestationRequest keyAttestationRequest) {
    List<KeyAttestationItem> items = keyAttestationRequest.getJwks();
    if (items == null || items.isEmpty()) {
      throw new InvalidKeyAttestationRequestParameterException("jwks must not be empty.");
    }
    if (items.stream()
        .anyMatch(item -> item == null || item.getJwk() == null || item.getJwk().isBlank())) {
      throw new InvalidKeyAttestationRequestParameterException("jwk must not be empty.");
    }

    List<String> jwks = items.stream().map(KeyAttestationItem::getJwk).toList();
    SignedJWT signedJwt =
        attestationService.createKeyAttestation(
            jwks,
            keyAttestationRequest.getNonce().orElse(null));
    return ResponseEntity.ok(new KeyAttestationResponse(signedJwt.serialize()));
  }
}
