// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import com.nimbusds.jose.jwk.ECKey;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.provider.application.model.WalletUnitAttestationDto;

@RequestMapping("/wallet-unit-attestation")
@RestController
public class WalletUnitAttestationController {

  private final String createWuaResponse =
      """
          {
            "iss": "Digg",
            "iat": 1516247022,
            "exp": 1541493724,
            "eudi_wallet_info": {
              "general_info": {
                "wallet_provider_name": "Digg",
                "wallet_solution_id": "Diggidigg-id",
                "wallet_solution_version": "0.0.1",
                "wallet_solution_certification_information": "UNCERTIFIED"
              },
              "wscd_info": {
                "wscd_type": "REMOTE",
                "wscd_certification_information": "UNCERTIFIED",
                "wscd_attack_resistance": 2
              }
            },
            "status": {
              "status_list": {
                "idx": 412,
                "uri": "https://revocation_url/statuslists/1"
              }
            },
            "attested_keys": [
              %s
            ]
          }
          """;

  @PostMapping
  public ResponseEntity<String> postWalletUnitAttestation(
      @RequestBody WalletUnitAttestationDto walletUnitAttestationDto) throws Exception {

    ECKey key = ECKey.parse(walletUnitAttestationDto.jwk());

    return ResponseEntity.ok(String.format(createWuaResponse, key.toString()));
  }
}
