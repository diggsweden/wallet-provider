// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jwt.SignedJWT;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.digg.wallet.provider.api.v0.model.KeyAttestationItem;
import se.digg.wallet.provider.api.v0.model.KeyAttestationRequest;
import se.digg.wallet.provider.application.filter.SensitiveDataMasker;
import se.digg.wallet.provider.application.service.KeyAttestationService;
import se.digg.wallet.provider.application.service.exception.InvalidKeyAttestationRequestParameterException;
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
  private KeyAttestationService service;
  @MockitoBean
  private SensitiveDataMasker sensitiveDataMasker;

  @ParameterizedTest(name = "returns 200 OK for {0} JWK(s)")
  @MethodSource("validJwkListCases")
  void validRequestWithJwksReturns200Ok(
      String description, List<KeyAttestationItem> jwks) throws Exception {
    String expectedJwt = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJEaWdnIn0.test";
    when(service.createKeyAttestation(anyList(), anyString()))
        .thenReturn(SignedJWT.parse(expectedJwt));

    String nonce = "123123123123";
    KeyAttestationRequest input =
        KeyAttestationRequest.builder().jwks(jwks).nonce(nonce).build();

    mockMvc
        .perform(
            post("/v0/key-attestations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(input)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.key_attestation").value(expectedJwt));

    List<String> expectedJwkStrings = jwks.stream().map(KeyAttestationItem::getJwk).toList();
    verify(service).createKeyAttestation(eq(expectedJwkStrings), eq(nonce));
  }

  @Test
  void requestWithNullNonceReturns200Ok() throws Exception {
    String expectedJwt = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJEaWdnIn0.test";
    when(service.createKeyAttestation(anyList(), eq(null)))
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
        KeyAttestationRequest.builder()
            .jwks(List.of(KeyAttestationItem.builder().jwk(jwk).build()))
            .nonce(null)
            .build();

    mockMvc
        .perform(
            post("/v0/key-attestations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(input)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.key_attestation").value(expectedJwt));
  }

  @ParameterizedTest(name = "returns {1} when service throws {0}")
  @MethodSource("serviceErrorCases")
  void service_exceptions_are_mapped_to_problem_details(
      Exception serviceException,
      HttpStatus expectedStatus,
      String expectedTitle,
      String expectedDetail)
      throws Exception {
    when(service.createKeyAttestation(anyList(), any())).thenThrow(serviceException);

    mockMvc
        .perform(
            post("/v0/key-attestations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jwks":[{"jwk":"test-jwk"}],"nonce":"test-nonce"}
                    """))
        .andExpect(status().is(expectedStatus.value()))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value(expectedTitle))
        .andExpect(jsonPath("$.status").value(expectedStatus.value()))
        .andExpect(jsonPath("$.detail").value(expectedDetail));
  }

  @ParameterizedTest(name = "returns 400 Bad Request for {0}")
  @MethodSource("invalidControllerJwkListCases")
  void anInvalidJwkListReturns400BadRequest(
      String description, String jsonPayload, String expectedDetail) throws Exception {
    mockMvc
        .perform(
            post("/v0/key-attestations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(expectedDetail));
  }

  static Stream<Arguments> invalidControllerJwkListCases() {
    return Stream.of(
        Arguments.of(
            "empty jwks list",
            """
                {"jwks":[],"nonce":"test-nonce"}
                """,
            "jwks must not be empty."),
        Arguments.of(
            "jwks item with blank jwk",
            """
                {"jwks":[{"jwk":"   "}],"nonce":"test-nonce"}
                """,
            "jwk must not be empty."),
        Arguments.of(
            "mixed list with a blank jwk",
            """
                {"jwks":[{"jwk":"valid-jwk"},{"jwk":"  "}],"nonce":"test-nonce"}
                """,
            "jwk must not be empty."));
  }

  static Stream<Arguments> validJwkListCases() {
    String jwk1 =
        """
            {
                "kty": "EC",
                "use": "sig",
                "crv": "P-256",
                "x": "18wHLeIgW9wVN6VD1Txgpqy2LszYkMf6J8njVAibvhM",
                "y": "-V4dS4UaLMgP_4fY4j8ir7cl1TXlFdAgcx55o7TkcSA"
            }
            """;
    String jwk2 =
        """
            {
                "kty": "EC",
                "use": "sig",
                "crv": "P-256",
                "x": "f83OJ3D2xFMTbKEAnDEQxEtIFXJhFLDKDitwHR6Elxo",
                "y": "x_daQauqmFriU-BRnrqkoqWZF3LGURQJn6C30Gw9Vio"
            }
            """;
    return Stream.of(
        Arguments.of("single", List.of(KeyAttestationItem.builder().jwk(jwk1).build())),
        Arguments.of(
            "multiple",
            List.of(
                KeyAttestationItem.builder().jwk(jwk1).build(),
                KeyAttestationItem.builder().jwk(jwk2).build())));
  }

  static Stream<Arguments> serviceErrorCases() {
    return Stream.of(
        Arguments.of(
            new InvalidKeyAttestationRequestParameterException("Private keys are not accepted."),
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            "Private keys are not accepted."),
        Arguments.of(
            new WalletRuntimeException("Could not create attestation.", null),
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "Could not create attestation."),
        Arguments.of(
            new IllegalStateException("SENSITIVE INTERNAL DETAIL"),
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred."));
  }

  private String asJson(KeyAttestationRequest input) throws JacksonException {
    ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
    return objectWriter.writeValueAsString(input);
  }
}
