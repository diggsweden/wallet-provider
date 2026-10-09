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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;

@SpringBootTest
@AutoConfigureMockMvc
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
  void returnsSignedWia() throws Exception {
    var response = tryRequestWia(Map.of("jwk", PUBLIC_JWK));

    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(response.getResponseBody()).containsKey("wallet_instance_attestation");
    var wia =
        SignedJWT.parse((String) response.getResponseBody().get("wallet_instance_attestation"));
    assertThat(wia.verify(new ECDSAVerifier(keystoreProperties.getPublicKey()))).isTrue();
  }

  @Test
  void returnsWiaWithOauthTypeAndCertificateChain() throws Exception {
    var wia = requestWia();

    assertThat(wia.getHeader().getType().toString()).isEqualTo("oauth-client-attestation+jwt");
    assertThat(wia.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
    var expectedChain = keystoreProperties.getCertificateChain().stream()
        .map(certificate -> {
          try {
            return com.nimbusds.jose.util.Base64.encode(certificate.getEncoded());
          } catch (java.security.cert.CertificateEncodingException e) {
            throw new IllegalStateException(e);
          }
        }).toList();
    assertThat(wia.getHeader().getX509CertChain()).containsExactlyElementsOf(expectedChain);
  }

  @Test
  void returnsWiaWithProofOfPossessionBoundToTheSuppliedPublicKey() throws Exception {
    var wia = requestWia();

    assertThat(wia.getJWTClaimsSet().getJSONObjectClaim("cnf"))
        .containsEntry("jwk", Map.of(
            "kty", "EC", "crv", "P-256",
            "x", "18wHLeIgW9wVN6VD1Txgpqy2LszYkMf6J8njVAibvhM",
            "y", "-V4dS4UaLMgP_4fY4j8ir7cl1TXlFdAgcx55o7TkcSA"));
  }

  @Test
  void returnsWiaWithConfiguredLifetimeBelow24Hours() throws Exception {
    var beforeRequest = Instant.now().minusSeconds(1);

    var claims = requestWia().getJWTClaimsSet();

    assertThat(claims.getIssueTime()).isNotNull();
    assertThat(claims.getExpirationTime()).isNotNull();
    var issuedAt = claims.getIssueTime().toInstant();
    var expiresAt = claims.getExpirationTime().toInstant();
    assertThat(issuedAt).isBetween(beforeRequest, Instant.now());
    assertThat(Duration.between(issuedAt, expiresAt)).isEqualTo(Duration.ofMinutes(60));
    assertThat(Duration.between(issuedAt, expiresAt)).isLessThan(Duration.ofHours(24));
  }

  @Test
  void returnsWiaWithMaintenancePeriodIndependentOfTokenExpiration() throws Exception {
    var claims = requestWia().getJWTClaimsSet();

    var clientStatus = claims.getJSONObjectClaim("client_status");
    assertThat(clientStatus).containsEntry("status", Map.of("status_list", Map.of(
        "idx", 412L, "uri", "https://example.org/wia-statuslists/1")));
    var statusExpiration = Instant.ofEpochSecond(((Number) clientStatus.get("exp")).longValue());
    assertThat(Duration.between(claims.getIssueTime().toInstant(), statusExpiration))
        .isEqualTo(Duration.ofDays(365));
    assertThat(statusExpiration).isAfter(claims.getExpirationTime().toInstant());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "not-json", "null", "{}", "{\"kty\":\"RSA\"}"})
  void rejectsInvalidPublicKey(String invalidJwk) {
    var response = tryRequestWia(Map.of("jwk", invalidJwk));

    assertThat(response.getStatus().value()).isEqualTo(400);
    assertThat(response.getResponseHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getResponseBody()).containsEntry("status", 400);
    assertThat(response.getResponseBody()).doesNotContainKey("wallet_instance_attestation");
  }

  @Test
  void rejectsPrivateKey() throws Exception {
    var privateJwk = new ECKeyGenerator(Curve.P_256).generate().toJSONString();

    var response = tryRequestWia(Map.of("jwk", privateJwk));

    assertThat(response.getStatus().value()).isEqualTo(400);
    assertThat(response.getResponseHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getResponseBody()).containsEntry("detail",
        "Private keys are not accepted.");
    assertThat(response.getResponseBody()).doesNotContainKey("wallet_instance_attestation");
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"jwk\":null}"})
  void rejectsRequestWithNoPublicKey(String requestBody) {
    var response = tryRequestWia(requestBody);

    assertThat(response.getStatus().value()).isEqualTo(400);
    assertThat(response.getResponseHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getResponseBody()).containsEntry("status", 400);
    assertThat(response.getResponseBody()).doesNotContainKey("wallet_instance_attestation");
  }

  @Test
  void returnsWiaWithConfiguredWalletSolutionIdentity() throws Exception {
    var wia = requestWia();

    assertThat(wia.getJWTClaimsSet().getClaims())
        .containsEntry("sub", "digg-wallet")
        .containsEntry("wallet_name", "Digg Wallet")
        .containsEntry("wallet_version", "0.0.1")
        .containsEntry("wallet_link", "https://www.digg.se")
        .containsEntry("wallet_solution_certification_information",
            "UNCERTIFIED");
  }

  private SignedJWT requestWia() throws Exception {
    var response = tryRequestWia(Map.of("jwk", PUBLIC_JWK));
    assertThat(response.getStatus().value()).isEqualTo(200);
    return SignedJWT.parse((String) response.getResponseBody().get("wallet_instance_attestation"));
  }

  private EntityExchangeResult<Map<String, Object>> tryRequestWia(Object body) {
    return RestTestClient.bindTo(mockMvc).build().post().uri("/v0/wallet-instance-attestations")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange().expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
        .returnResult();
  }
}
