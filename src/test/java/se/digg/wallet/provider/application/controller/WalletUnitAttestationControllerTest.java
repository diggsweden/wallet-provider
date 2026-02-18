// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jwt.SignedJWT;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.digg.wallet.provider.application.model.WalletUnitAttestationDtoV2;
import se.digg.wallet.provider.application.service.WalletUnitAttestationService;

@WebMvcTest(WalletUnitAttestationController.class)
class WalletUnitAttestationControllerTest {

  private final ObjectMapper mapper = new ObjectMapper();
  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private WalletUnitAttestationService service;

  private static final List<String> PATHS = List.of(
      "/wallet-unit-attestation/v2",
      "/wallet-unit-attestation");

  @ParameterizedTest
  @FieldSource("PATHS")
  void assertThatPostWalletUnitAttestationV2_givenPublicKeyAndNonce_shouldReturnOk(String path)
      throws Exception {
    String expectedJwt = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJEaWdnIn0.test";
    when(service.createWalletUnitAttestationV2(anyString(), anyString()))
        .thenReturn(SignedJWT.parse(expectedJwt));

    String jwk =
        """
            {
                "kty": "EC",
                "use": "sig",
                "crv": "P-256",
                "x": "18wHLeIgW9wVN6VD1Txgpqy2LszYkMf6J8njVAibvhM",
                "y": "-V4dS4UaLMgP_4fY4j8ir7cl1TXlFdAgcx55o7TkcSA"
            }
            """;
    String nonce = "123123123123";
    WalletUnitAttestationDtoV2 input =
        new WalletUnitAttestationDtoV2(UUID.randomUUID(), jwk, nonce);

    mockMvc
        .perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(input)))
        .andExpect(status().isOk())
        .andExpect(content().string(expectedJwt));
  }

  @ParameterizedTest
  @FieldSource("PATHS")
  void assertThatPostWalletUnitAttestationV2_givenPublicKeyAndEmptyNonce_shouldReturnOk(String path)
      throws Exception {
    String expectedJwt = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJEaWdnIn0.test";
    when(service.createWalletUnitAttestationV2(anyString(), anyString()))
        .thenReturn(SignedJWT.parse(expectedJwt));

    String jwk =
        """
            {
                "kty": "EC",
                "use": "sig",
                "crv": "P-256",
                "x": "18wHLeIgW9wVN6VD1Txgpqy2LszYkMf6J8njVAibvhM",
                "y": "-V4dS4UaLMgP_4fY4j8ir7cl1TXlFdAgcx55o7TkcSA"
            }
            """;
    String nonce = "";
    WalletUnitAttestationDtoV2 input =
        new WalletUnitAttestationDtoV2(UUID.randomUUID(), jwk, nonce);

    mockMvc
        .perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(input)))
        .andExpect(status().isOk())
        .andExpect(content().string(expectedJwt));
  }

  @ParameterizedTest
  @FieldSource("PATHS")
  void assertThatPostWalletUnitAttestationV2_givenPublicKeyAndNullNonce_shouldReturnOk(String path)
      throws Exception {
    String expectedJwt = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJEaWdnIn0.test";
    when(service.createWalletUnitAttestationV2(anyString(), eq(null)))
        .thenReturn(SignedJWT.parse(expectedJwt));

    String jwk =
        """
            {
                "kty": "EC",
                "use": "sig",
                "crv": "P-256",
                "x": "18wHLeIgW9wVN6VD1Txgpqy2LszYkMf6J8njVAibvhM",
                "y": "-V4dS4UaLMgP_4fY4j8ir7cl1TXlFdAgcx55o7TkcSA"
            }
            """;

    WalletUnitAttestationDtoV2 input =
        new WalletUnitAttestationDtoV2(UUID.randomUUID(), jwk, null);

    mockMvc
        .perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(input)))
        .andExpect(status().isOk())
        .andExpect(content().string(expectedJwt));
  }

  private String asJson(Object input) throws JsonProcessingException {
    ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
    return objectWriter.writeValueAsString(input);
  }
}
