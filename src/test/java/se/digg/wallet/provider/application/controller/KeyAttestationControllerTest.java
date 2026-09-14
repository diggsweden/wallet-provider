// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.digg.wallet.provider.api.v0.model.KeyAttestationRequest;
import se.digg.wallet.provider.application.filter.SensitiveDataMasker;
import se.digg.wallet.provider.application.service.WalletUnitAttestationService;
import se.digg.wallet.provider.application.service.exception.InvalidWuaRequestParameterException;
import se.digg.wallet.provider.application.service.exception.WalletRuntimeException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

@WebMvcTest(KeyAttestationController.class)
class KeyAttestationControllerTest {

  private final ObjectMapper mapper = new ObjectMapper();
  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private WalletUnitAttestationService service;
  @MockitoBean
  private SensitiveDataMasker sensitiveDataMasker;

  @Test
  void a_valid_request_returns_200_ok() throws Exception {
    String expectedJwt = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJEaWdnIn0.test";
    when(service.createWalletUnitAttestation(anyString(), anyString()))
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
    KeyAttestationRequest input =
        KeyAttestationRequest.builder().jwk(jwk).nonce(nonce).build();

    mockMvc
        .perform(
            post("/key_attestations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(input)))
        .andExpect(status().isOk())
        .andExpect(content().string(expectedJwt));
  }

  @Test
  void a_request_with_null_nonce_returns_200_ok() throws Exception {
    String expectedJwt = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJEaWdnIn0.test";
    when(service.createWalletUnitAttestation(anyString(), eq(null)))
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
    KeyAttestationRequest input =
        KeyAttestationRequest.builder().jwk(jwk).nonce(null).build();

    mockMvc
        .perform(
            post("/key_attestations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(input)))
        .andExpect(status().isOk())
        .andExpect(content().string(expectedJwt));
  }

  @Test
  void a_request_with_an_invalid_parameter_returns_400_bad_request() throws Exception {
    String errorMessage = "Private keys are not accepted.";
    when(service.createWalletUnitAttestation(anyString(), anyString()))
        .thenThrow(new InvalidWuaRequestParameterException(errorMessage));

    mockMvc
        .perform(
            post("/key_attestations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jwk":"test-jwk","nonce":"test-nonce"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(errorMessage));
  }

  @Test
  void a_runtime_failure_returns_500_internal_server_error() throws Exception {
    String errorMessage = "Could not create attestation.";
    when(service.createWalletUnitAttestation(anyString(), anyString()))
        .thenThrow(new WalletRuntimeException(errorMessage, null));

    mockMvc
        .perform(
            post("/key_attestations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jwk":"test-jwk","nonce":"test-nonce"}
                    """))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Internal Server Error"))
        .andExpect(jsonPath("$.detail").value(errorMessage));
  }

  private String asJson(KeyAttestationRequest input) throws JacksonException {
    ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
    return objectWriter.writeValueAsString(input);
  }
}
