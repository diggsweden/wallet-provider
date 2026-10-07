// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;

@SpringBootTest(properties = {
    "wia.client-id=digg-test-client",
    "wia.wallet-name=Digg Test Wallet",
    "wia.wallet-version=1.2.3",
    "wia.wallet-link=https://example.org/wallet",
    "wia.wallet-solution-certification-information=https://example.org/certification",
    "wia.validity-minutes=30",
    "wia.status-maintenance-days=90",
    "wia.status={\"status_list\":{\"idx\":7,\"uri\":\"https://example.org/wia-statuslists/1\"}}"
})
@AutoConfigureMockMvc
@SuppressWarnings("checkstyle:MethodName")
class WalletInstanceAttestationControllerComponentTest {

  private static final String PUBLIC_JWK = """
      {"kty":"EC","crv":"P-256",\
      "x":"18wHLeIgW9wVN6VD1Txgpqy2LszYkMf6J8njVAibvhM",\
      "y":"-V4dS4UaLMgP_4fY4j8ir7cl1TXlFdAgcx55o7TkcSA"}
      """;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private WuaKeystoreProperties keystoreProperties;

  @Test
  void a_public_key_request_returns_a_wia_signed_by_the_wallet_provider() throws Exception {
    var response =
        RestTestClient.bindTo(mockMvc).build().post().uri("/v0/wallet-instance-attestations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("jwk", PUBLIC_JWK))
            .exchange()
            .expectBody(Map.class).returnResult();

    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(response.getResponseBody()).containsKey("wallet_instance_attestation");
    var jwt =
        SignedJWT.parse((String) response.getResponseBody().get("wallet_instance_attestation"));
    assertThat(jwt.verify(new ECDSAVerifier(keystoreProperties.getPublicKey()))).isTrue();
  }

  @Test
  void a_wia_identifies_its_oauth_type_and_provider_certificate_chain() throws Exception {
    var jwt = requestWia();

    assertThat(jwt.getHeader().getType().toString()).isEqualTo("oauth-client-attestation+jwt");
    assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
    var expectedChain = keystoreProperties.getCertificateChain().stream()
        .map(certificate -> {
          try {
            return com.nimbusds.jose.util.Base64.encode(certificate.getEncoded());
          } catch (java.security.cert.CertificateEncodingException e) {
            throw new IllegalStateException(e);
          }
        }).toList();
    assertThat(jwt.getHeader().getX509CertChain()).containsExactlyElementsOf(expectedChain);
  }

  @Test
  void a_wia_binds_proof_of_possession_to_the_supplied_public_key() throws Exception {
    var jwt = requestWia();

    assertThat(jwt.getJWTClaimsSet().getJSONObjectClaim("cnf"))
        .containsEntry("jwk", Map.of(
            "kty", "EC", "crv", "P-256",
            "x", "18wHLeIgW9wVN6VD1Txgpqy2LszYkMf6J8njVAibvhM",
            "y", "-V4dS4UaLMgP_4fY4j8ir7cl1TXlFdAgcx55o7TkcSA"));
  }

  @Test
  void a_wia_has_the_configured_lifetime_below_24_hours() throws Exception {
    var beforeRequest = Instant.now().minusSeconds(1);

    var claims = requestWia().getJWTClaimsSet();

    assertThat(claims.getIssueTime()).isNotNull();
    assertThat(claims.getExpirationTime()).isNotNull();
    var issuedAt = claims.getIssueTime().toInstant();
    var expiresAt = claims.getExpirationTime().toInstant();
    assertThat(issuedAt).isBetween(beforeRequest, Instant.now());
    assertThat(Duration.between(issuedAt, expiresAt)).isEqualTo(Duration.ofMinutes(30));
    assertThat(Duration.between(issuedAt, expiresAt)).isLessThan(Duration.ofHours(24));
  }

  @Test
  void a_wia_has_a_status_maintenance_period_independent_of_token_expiration() throws Exception {
    var claims = requestWia().getJWTClaimsSet();

    var clientStatus = claims.getJSONObjectClaim("client_status");
    assertThat(clientStatus).containsEntry("status", Map.of("status_list", Map.of(
        "idx", 7L, "uri", "https://example.org/wia-statuslists/1")));
    var statusExpiration = Instant.ofEpochSecond(((Number) clientStatus.get("exp")).longValue());
    assertThat(Duration.between(claims.getIssueTime().toInstant(), statusExpiration))
        .isEqualTo(Duration.ofDays(90));
    assertThat(statusExpiration).isAfter(claims.getExpirationTime().toInstant());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "not-json", "null", "{}", "{\"kty\":\"RSA\"}"})
  void an_invalid_public_key_is_rejected_with_problem_details(String invalidJwk) {
    var response =
        RestTestClient.bindTo(mockMvc).build().post().uri("/v0/wallet-instance-attestations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("jwk", invalidJwk))
            .exchange().expectBody(Map.class).returnResult();

    assertThat(response.getStatus().value()).isEqualTo(400);
    assertThat(response.getResponseHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getResponseBody()).containsEntry("status", 400);
    assertThat(response.getResponseBody()).doesNotContainKey("wallet_instance_attestation");
  }

  @Test
  void a_private_key_is_rejected_instead_of_being_embedded_in_a_wia() throws Exception {
    var privateJwk = new ECKeyGenerator(Curve.P_256).generate().toJSONString();

    var response =
        RestTestClient.bindTo(mockMvc).build().post().uri("/v0/wallet-instance-attestations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("jwk", privateJwk))
            .exchange().expectBody(Map.class).returnResult();

    assertThat(response.getStatus().value()).isEqualTo(400);
    assertThat(response.getResponseHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getResponseBody()).containsEntry("detail",
        "Private keys are not accepted.");
    assertThat(response.getResponseBody()).doesNotContainKey("wallet_instance_attestation");
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"jwk\":null}"})
  void a_request_without_a_public_key_is_rejected_with_problem_details(String requestBody) {
    var response =
        RestTestClient.bindTo(mockMvc).build().post().uri("/v0/wallet-instance-attestations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .exchange().expectBody(Map.class).returnResult();

    assertThat(response.getStatus().value()).isEqualTo(400);
    assertThat(response.getResponseHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getResponseBody()).containsEntry("status", 400);
    assertThat(response.getResponseBody()).doesNotContainKey("wallet_instance_attestation");
  }

  private SignedJWT requestWia() throws Exception {
    var response =
        RestTestClient.bindTo(mockMvc).build().post().uri("/v0/wallet-instance-attestations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("jwk", PUBLIC_JWK))
            .exchange().expectBody(Map.class).returnResult();
    assertThat(response.getStatus().value()).isEqualTo(200);
    return SignedJWT.parse((String) response.getResponseBody().get("wallet_instance_attestation"));
  }

  @Test
  void a_wia_contains_the_configured_wallet_solution_identity() throws Exception {
    var jwt = requestWia();

    assertThat(jwt.getJWTClaimsSet().getClaims())
        .containsEntry("sub", "digg-test-client")
        .containsEntry("wallet_name", "Digg Test Wallet")
        .containsEntry("wallet_version", "1.2.3")
        .containsEntry("wallet_link", "https://example.org/wallet")
        .containsEntry("wallet_solution_certification_information",
            "https://example.org/certification");
  }
}
