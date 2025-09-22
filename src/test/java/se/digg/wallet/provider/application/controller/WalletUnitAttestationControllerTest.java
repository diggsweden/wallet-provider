// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import se.digg.wallet.provider.application.model.WalletUnitAttestationDto;

@WebMvcTest(WalletUnitAttestationController.class)
class WalletUnitAttestationControllerTest {
  @Autowired
  MockMvc mockMvc;

  ObjectMapper mapper = new ObjectMapper();

  @Test
  void assertThatPostWalletUnitAttestation_givenValidPublicKey_shouldReturnOk() throws Exception {

    KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
    gen.initialize(Curve.P_256.toECParameterSpec());
    KeyPair keyPair = gen.generateKeyPair();

    // Convert to JWK format
    JWK jwk =
        new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
            .privateKey((ECPrivateKey) keyPair.getPrivate())
            .build();

    WalletUnitAttestationDto input =
        new WalletUnitAttestationDto(UUID.randomUUID(), jwk.toString());

    String path = "/wallet-unit-attestation";
    mockMvc
        .perform(post(path).contentType(MediaType.APPLICATION_JSON).content(asJson(input)))
        .andDo(log())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attested_keys[0].x").value(jwk.toECKey().getX().toString()));
  }

  private String asJson(WalletUnitAttestationDto input) throws JsonProcessingException {
    ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
    return objectWriter.writeValueAsString(input);
  }
}
