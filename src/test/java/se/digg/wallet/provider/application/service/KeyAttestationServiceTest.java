// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;
import se.digg.wallet.provider.application.service.exception.InvalidKeyAttestationRequestParameterException;
import se.digg.wallet.provider.application.service.exception.WalletRuntimeException;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class KeyAttestationServiceTest {

  @Autowired
  private KeyAttestationService service;
  @Autowired
  private WuaKeystoreProperties keystoreProperties;

  @SuppressWarnings("unchecked")
  private static void verifyStatusClaim(SignedJWT jwt) throws ParseException {
    Map<String, Object> keyStorageStatus =
        jwt.getJWTClaimsSet().getJSONObjectClaim("key_storage_status");
    Map<String, Object> status = (Map<String, Object>) keyStorageStatus.get("status");
    Map<String, Object> statusList = (Map<String, Object>) status.get("status_list");
    assertEquals(412, statusList.get("idx"));
    assertEquals("https://revocation_url/statuslists/1", statusList.get("uri"));
    assertNotNull(keyStorageStatus.get("exp"));
  }

  @SuppressWarnings("unchecked")
  private static void verifyAttestedKeysClaim(SignedJWT jwt, ECKey jwk) throws ParseException {
    assertNotNull(jwt.getJWTClaimsSet().getClaim("attested_keys"));

    List<Map<String, Object>> attestedKeys =
        (List<Map<String, Object>>) jwt.getJWTClaimsSet().getClaim("attested_keys");
    Map<String, Object> attestedKey = attestedKeys.getFirst();
    assertEquals(jwk.getX().toString(), attestedKey.get("x"));
    assertEquals(jwk.getY().toString(), attestedKey.get("y"));
    assertEquals(jwk.getCurve().toString(), attestedKey.get("crv"));
  }

  @Test
  void assertThatCreateKeyAttestation_givenValidJwk_shouldSucceed() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createKeyAttestation(jwk.toString(), "nonce");

    assertNotNull(jwt);
    assertEquals("http://example.com/cert", jwt.getJWTClaimsSet().getStringClaim("certification"));
    assertEquals(keystoreProperties.issuer(), jwt.getJWTClaimsSet().getIssuer());
    assertEquals(jwk.computeThumbprint().toString(), jwt.getJWTClaimsSet().getSubject());

    verifyAttestedKeysClaim(jwt, jwk);
    verifyStatusClaim(jwt);
    verifyJwtSignature(jwt, keystoreProperties.getPublicKey());
  }

  @Test
  void must_throw_invalid_key_attestation_parameter_exception_when_jwk_contains_private_key()
      throws Exception {
    ECKey jwkWithPrivate = createJwkWithPrivateKey();

    assertThatThrownBy(() -> service.createKeyAttestation(jwkWithPrivate.toString(), "nonce"))
        .isInstanceOf(InvalidKeyAttestationRequestParameterException.class)
        .hasMessage("Private keys are not accepted.");
  }

  @Test
  void must_throw_invalid_key_attestation_parameter_exception_for_an_invalid_jwk() {
    InvalidKeyAttestationRequestParameterException exception =
        assertThrows(
            InvalidKeyAttestationRequestParameterException.class,
            () -> service.createKeyAttestation("not-a-jwk", "nonce"));

    assertEquals("Invalid wallet public key JWK.", exception.getMessage());
    assertTrue(exception.getCause() instanceof ParseException);
  }

  @Test
  void must_throw_invalid_key_attestation_parameter_exception_for_a_jwk_that_is_the_json_literal_null() {
    // nimbus-jose-jwt's ECKey.parse() throws an unchecked NullPointerException instead of a
    // ParseException for this specific input (JSONObjectUtils.parse() returns null for the JSON
    // literal "null" without throwing). Regression test for that library quirk: it must still be
    // classified as an invalid client parameter (400), not an internal server error (500).
    InvalidKeyAttestationRequestParameterException exception =
        assertThrows(
            InvalidKeyAttestationRequestParameterException.class,
            () -> service.createKeyAttestation("null", "nonce"));

    assertEquals("Invalid wallet public key JWK.", exception.getMessage());
    assertTrue(exception.getCause() instanceof ParseException);
  }

  @Test
  void must_use_a_safe_client_message_for_certificate_encoding_failure_while_preserving_cause_for_logs()
      throws Exception {
    String causeDetail = "some encoding failure detail";
    X509Certificate badCert = mock(X509Certificate.class);
    when(badCert.getEncoded()).thenThrow(new CertificateEncodingException(causeDetail));

    WuaKeystoreProperties properties = mock(WuaKeystoreProperties.class);
    when(properties.getSigningKey()).thenReturn(mock(ECPrivateKey.class));
    when(properties.getCertificateChain()).thenReturn(List.of(badCert));
    when(properties.validityHours()).thenReturn(1);
    when(properties.status()).thenReturn("{}");

    KeyAttestationService service = new KeyAttestationService(properties, new ObjectMapper());

    WalletRuntimeException exception = assertThrows(
        WalletRuntimeException.class,
        () -> service.createKeyAttestation(createJwk().toString(), "nonce"));

    // Client-facing: fixed and safe, independent of the cause chain's content.
    assertEquals("Could not create attestation.", exception.getMessage());

    // Server-side: the full cause chain, including the original detail, is preserved intact.
    assertInstanceOf(WalletRuntimeException.class, exception.getCause());
    assertInstanceOf(CertificateEncodingException.class, exception.getCause().getCause());
    assertEquals(causeDetail, exception.getCause().getCause().getMessage());
  }

  @Test
  void must_wrap_jose_exception_in_wallet_runtime_exception() {
    WuaKeystoreProperties properties = mock(WuaKeystoreProperties.class);
    when(properties.getSigningKey()).thenReturn(mock(ECPrivateKey.class));
    when(properties.getCertificateChain()).thenReturn(List.of());
    when(properties.validityHours()).thenReturn(1);
    when(properties.status()).thenReturn("{}");

    KeyAttestationService service = new KeyAttestationService(properties, new ObjectMapper());

    WalletRuntimeException exception =
        assertThrows(
            WalletRuntimeException.class,
            () -> service.createKeyAttestation(createJwk().toString(), "nonce"));

    assertEquals("Could not create attestation.", exception.getMessage());
    assertInstanceOf(JOSEException.class, exception.getCause());
  }

  @Test
  void assertThatCreateKeyAttestation_hasX5cHeader() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createKeyAttestation(jwk.toString(), "nonce");

    assertNotNull(jwt.getHeader().getX509CertChain());
    assertFalse(jwt.getHeader().getX509CertChain().isEmpty());
  }

  @Test
  void assertThatCreateKeyAttestation_containsNonceButNotKid() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createKeyAttestation(jwk.toString(), "nonce");

    assertEquals("key-attestation+jwt", jwt.getHeader().getType().getType());

    assertFalse(jwt.getHeader().toJSONObject().containsKey("kid"));

    assertTrue(jwt.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
  }

  @Test
  void assertThatCreateKeyAttestation_handlesEmptyNonce() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createKeyAttestation(jwk.toString(), "");

    assertEquals(
        Set.of(
            "iss",
            "sub",
            "iat",
            "exp",
            "certification",
            "key_storage_status",
            "attested_keys",
            "nonce",
            "key_storage",
            "user_authentication"),
        jwt.getJWTClaimsSet().toJSONObject().keySet());
    assertTrue(jwt.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
    assertEquals("", jwt.getJWTClaimsSet().toJSONObject().get("nonce"));
  }

  @Test
  void assertThatCreateKeyAttestation_containsKeyStorageAndUserAuthentication() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createKeyAttestation(jwk.toString(), "nonce");

    assertEquals(List.of("iso_18045_high"),
        jwt.getJWTClaimsSet().getStringListClaim("key_storage"));
    assertEquals(
        List.of("iso_18045_high"), jwt.getJWTClaimsSet().getStringListClaim("user_authentication"));
  }

  @Test
  void assertThatCreateKeyAttestation_handlesNullNonce() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createKeyAttestation(jwk.toString(), null);

    assertEquals(
        Set.of(
            "iss",
            "sub",
            "iat",
            "exp",
            "certification",
            "key_storage_status",
            "attested_keys",
            "key_storage",
            "user_authentication"),
        jwt.getJWTClaimsSet().toJSONObject().keySet());
    assertFalse(jwt.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
  }

  @Test
  void assertThatCreateKeyAttestation_givenMultipleValidJwks_shouldSucceed() throws Exception {
    ECKey jwk1 = createJwk();
    ECKey jwk2 = createJwk();

    SignedJWT jwt =
        service.createKeyAttestation(List.of(jwk1.toString(), jwk2.toString()), "nonce");

    assertNotNull(jwt);
    assertEquals(jwk1.computeThumbprint().toString(), jwt.getJWTClaimsSet().getSubject());

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> attestedKeys =
        (List<Map<String, Object>>) jwt.getJWTClaimsSet().getClaim("attested_keys");
    assertEquals(2, attestedKeys.size());
    assertEquals(jwk1.getX().toString(), attestedKeys.get(0).get("x"));
    assertEquals(jwk2.getX().toString(), attestedKeys.get(1).get("x"));
  }

  @Test
  void must_throw_invalid_key_attestation_parameter_exception_when_jwks_list_is_empty() {
    assertThatThrownBy(() -> service.createKeyAttestation(List.of(), "nonce"))
        .isInstanceOf(InvalidKeyAttestationRequestParameterException.class)
        .hasMessage("jwks must not be empty.");
  }

  @Test
  void must_throw_invalid_key_attestation_parameter_exception_when_any_jwk_contains_private_key()
      throws Exception {
    ECKey validJwk = createJwk();
    ECKey jwkWithPrivate = createJwkWithPrivateKey();

    assertThatThrownBy(
        () -> service.createKeyAttestation(
            List.of(validJwk.toString(), jwkWithPrivate.toString()), "nonce"))
        .isInstanceOf(InvalidKeyAttestationRequestParameterException.class)
        .hasMessage("Private keys are not accepted.");
  }

  @Test
  void must_throw_invalid_key_attestation_parameter_exception_when_jwk_is_null_or_blank() {
    assertThatThrownBy(() -> service.createKeyAttestation(Collections.singletonList(null), "nonce"))
        .isInstanceOf(InvalidKeyAttestationRequestParameterException.class)
        .hasMessage("jwk must not be empty.");

    assertThatThrownBy(() -> service.createKeyAttestation(List.of("   "), "nonce"))
        .isInstanceOf(InvalidKeyAttestationRequestParameterException.class)
        .hasMessage("jwk must not be empty.");
  }

  @ParameterizedTest(name = "rejects mixed list with {0}")
  @MethodSource("invalidMixedJwkCases")
  void mixedListContainingAnyInvalidJwkIsRejected(
      String description, List<String> jwks, String expectedMessagePrefix) {
    assertThatThrownBy(() -> service.createKeyAttestation(jwks, "nonce"))
        .isInstanceOf(InvalidKeyAttestationRequestParameterException.class)
        .hasMessageStartingWith(expectedMessagePrefix);
  }

  static Stream<Arguments> invalidMixedJwkCases() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
    gen.initialize(Curve.P_256.toECParameterSpec());
    KeyPair keyPair = gen.generateKeyPair();
    String validJwk =
        new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).build().toString();
    String privateJwk =
        new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
            .privateKey(keyPair.getPrivate())
            .build()
            .toString();

    return Stream.of(
        Arguments.of(
            "private key mixed with valid key",
            List.of(validJwk, privateJwk),
            "Private keys are not accepted."),
        Arguments.of(
            "malformed jwk string mixed with valid key",
            List.of(validJwk, "not-a-valid-jwk-json"),
            "Invalid wallet public key JWK."),
        Arguments.of(
            "blank jwk mixed with valid key",
            List.of(validJwk, "   "),
            "jwk must not be empty."),
        Arguments.of(
            "null jwk mixed with valid key",
            Arrays.asList(validJwk, null),
            "jwk must not be empty."),
        Arguments.of(
            "malformed jwk as first item before valid key",
            List.of("not-a-valid-jwk-json", validJwk),
            "Invalid wallet public key JWK."));
  }

  private void verifyJwtSignature(SignedJWT jwt, ECPublicKey publicKey) throws JOSEException {
    assertTrue(jwt.verify(new ECDSAVerifier(publicKey)));
  }

  private ECKey createJwk() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
    gen.initialize(Curve.P_256.toECParameterSpec());
    KeyPair keyPair = gen.generateKeyPair();

    return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).build();
  }

  private ECKey createJwkWithPrivateKey() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
    gen.initialize(Curve.P_256.toECParameterSpec());
    KeyPair keyPair = gen.generateKeyPair();

    return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
        .privateKey(keyPair.getPrivate())
        .build();
  }
}
