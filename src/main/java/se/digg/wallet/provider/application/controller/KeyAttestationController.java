// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import com.nimbusds.jwt.SignedJWT;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.provider.api.v0.KeyAttestationApi;
import se.digg.wallet.provider.api.v0.model.KeyAttestationItemDto;
import se.digg.wallet.provider.api.v0.model.KeyAttestationRequestDto;
import se.digg.wallet.provider.api.v0.model.KeyAttestationResponseDto;
import se.digg.wallet.provider.domain.exception.InvalidKeyAttestationRequestParameterException;
import se.digg.wallet.provider.domain.service.KeyAttestationService;

@RestController
public class KeyAttestationController implements KeyAttestationApi {

  private final KeyAttestationService attestationService;

  public KeyAttestationController(KeyAttestationService attestationService) {
    this.attestationService = attestationService;
  }

  @Override
  public ResponseEntity<KeyAttestationResponseDto> postKeyAttestation(
      KeyAttestationRequestDto keyAttestationRequest) {
    List<KeyAttestationItemDto> items = keyAttestationRequest.getJwks();
    if (items == null || items.isEmpty()) {
      throw new InvalidKeyAttestationRequestParameterException("jwks must not be empty.");
    }
    if (items.stream()
        .anyMatch(item -> item == null || item.getJwk() == null || item.getJwk().isBlank())) {
      throw new InvalidKeyAttestationRequestParameterException("jwk must not be empty.");
    }

    List<String> jwks = items.stream().map(KeyAttestationItemDto::getJwk).toList();
    SignedJWT signedJwt =
        attestationService.createKeyAttestation(
            jwks,
            keyAttestationRequest.getNonce().orElse(null));
    return ResponseEntity.ok(new KeyAttestationResponseDto(signedJwt.serialize()));
  }
}
