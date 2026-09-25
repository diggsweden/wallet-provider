// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;
import se.digg.wallet.provider.application.service.exception.InvalidWuaRequestParameterException;
import se.digg.wallet.provider.application.service.exception.WalletRuntimeException;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class WalletUnitAttestationServiceTest {

  @Autowired
  private WalletUnitAttestationService service;
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
  void assertThatCreateWalletUnitAttestation_givenValidJwk_shouldSucceed() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "nonce");

    assertNotNull(jwt);
    assertEquals("http://example.com/cert", jwt.getJWTClaimsSet().getStringClaim("certification"));

    verifyAttestedKeysClaim(jwt, jwk);
    verifyStatusClaim(jwt);
    verifyJwtSignature(jwt, keystoreProperties.getPublicKey());
  }

  @Test
  void must_throw_invalid_wua_parameter_exception_for_an_invalid_jwk() {
    InvalidWuaRequestParameterException exception = assertThrows(
        InvalidWuaRequestParameterException.class,
        () -> service.createWalletUnitAttestation("not-a-jwk", "nonce"));

    assertEquals("Invalid wallet public key JWK.", exception.getMessage());
    assertTrue(exception.getCause() instanceof ParseException);
  }

  @Test
  void must_throw_invalid_wua_parameter_exception_when_jwk_is_the_json_literal_null() {
    // nimbus-jose-jwt's ECKey.parse() throws an unchecked NullPointerException instead of a
    // ParseException for this specific input (JSONObjectUtils.parse() returns null for the JSON
    // literal "null" without throwing). Regression test for that library quirk: it must still be
    // classified as an invalid client parameter (400), not an internal server error (500).
    InvalidWuaRequestParameterException exception = assertThrows(
        InvalidWuaRequestParameterException.class,
        () -> service.createWalletUnitAttestation("null", "nonce"));

    assertEquals("Invalid wallet public key JWK.", exception.getMessage());
    assertTrue(exception.getCause() instanceof ParseException);
  }


  @Test
  void must_wrap_jose_exception_in_wallet_runtime_exception() {
    WuaKeystoreProperties properties = mock(WuaKeystoreProperties.class);
    when(properties.getSigningKey()).thenReturn(mock(java.security.interfaces.ECPrivateKey.class));
    when(properties.getCertificateChain()).thenReturn(List.of());
    when(properties.validityHours()).thenReturn(1);
    when(properties.status()).thenReturn("{}");

    WalletUnitAttestationService service =
        new WalletUnitAttestationService(properties, new ObjectMapper());

    WalletRuntimeException exception = assertThrows(
        WalletRuntimeException.class,
        () -> service.createWalletUnitAttestation(createJwk().toString(), "nonce"));

    assertEquals("Could not create attestation.", exception.getMessage());
    assertInstanceOf(JOSEException.class, exception.getCause());
  }

  @Test
  void must_use_safe_client_message_for_cert_encoding_failure_while_preserving_cause_for_logs()
      throws Exception {

    String causeDetail = "some encoding failure detail";
    X509Certificate badCert = mock(X509Certificate.class);
    when(badCert.getEncoded()).thenThrow(new CertificateEncodingException(causeDetail));

    WuaKeystoreProperties properties = mock(WuaKeystoreProperties.class);
    when(properties.getSigningKey()).thenReturn(mock(ECPrivateKey.class));
    when(properties.getCertificateChain()).thenReturn(List.of(badCert));
    when(properties.validityHours()).thenReturn(1);
    when(properties.status()).thenReturn("{}");

    WalletUnitAttestationService service =
        new WalletUnitAttestationService(properties, new ObjectMapper());

    WalletRuntimeException exception = assertThrows(
        WalletRuntimeException.class,
        () -> service.createWalletUnitAttestation(createJwk().toString(), "nonce"));

    // Client-facing: fixed and safe, independent of the cause chain's content.
    assertEquals("Could not create attestation.", exception.getMessage());

    // Server-side: the full cause chain, including the original detail, is preserved intact.
    assertInstanceOf(WalletRuntimeException.class, exception.getCause());
    assertInstanceOf(CertificateEncodingException.class, exception.getCause().getCause());
    assertEquals(causeDetail, exception.getCause().getCause().getMessage());
  }


  @Test
  void assertThatCreateWalletUnitAttestation_hasX5cHeader() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "nonce");

    assertNotNull(jwt.getHeader().getX509CertChain());
    assertFalse(jwt.getHeader().getX509CertChain().isEmpty());
  }

  @Test
  void assertThatCreateWalletUnitAttestation_containsNonceButNotKid() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "nonce");

    assertEquals("key-attestation+jwt", jwt.getHeader().getType().getType());

    assertFalse(jwt.getHeader().toJSONObject().containsKey("kid"));

    assertTrue(jwt.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
  }

  @Test
  void assertThatCreateWalletUnitAttestation_handlesEmptyNonce() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "");

    assertEquals(8, jwt.getJWTClaimsSet().toJSONObject().size());
    assertTrue(jwt.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
    assertEquals("", jwt.getJWTClaimsSet().toJSONObject().get("nonce"));
  }

  @Test
  void assertThatCreateWalletUnitAttestation_containsKeyStorageAndUserAuthentication()
      throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "nonce");

    assertEquals(List.of("iso_18045_high"),
        jwt.getJWTClaimsSet().getStringListClaim("key_storage"));
    assertEquals(
        List.of("iso_18045_high"), jwt.getJWTClaimsSet().getStringListClaim("user_authentication"));
  }

  @Test
  void assertThatCreateWalletUnitAttestation_handlesNullNonce() throws Exception {
    ECKey jwk = createJwk();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), null);

    assertEquals(7, jwt.getJWTClaimsSet().toJSONObject().size());
    assertFalse(jwt.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
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
}
