// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
  void basicTest() throws Exception {
    WalletUnitAttestationDto input = new WalletUnitAttestationDto(UUID.randomUUID(), "publicKey");
    // {
    // "kty": "EC",
    // "crv": "P-256",
    // "x": "f83OJ3D2xF4G7Xvt1M7QhZ8P9d8LxV_tvn9RB9a9j7o",
    // "y": "x_FEzRu9PsXvZ46Vf1pSx1DjxzVg6RtqZjXU2JcY4xE"
    // }

    ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = objectWriter.writeValueAsString(input);

    String path = "/wallet-unit-attestation";
    mockMvc
        .perform(post(path).contentType(MediaType.APPLICATION_JSON.getType()).content(requestJson))
        .andExpect(status().isCreated());
  }
}
