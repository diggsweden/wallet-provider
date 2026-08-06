// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;

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
    ECKey jwk = createJWK();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "nonce");

    assertNotNull(jwt);
    assertEquals("http://example.com/cert", jwt.getJWTClaimsSet().getStringClaim("certification"));

    verifyAttestedKeysClaim(jwt, jwk);
    verifyStatusClaim(jwt);
    verifyJwtSignature(jwt, keystoreProperties.getPublicKey());
  }

  @Test
  void assertThatCreateWalletUnitAttestation_hasX5CHeader() throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "nonce");

    assertNotNull(jwt.getHeader().getX509CertChain());
    assertFalse(jwt.getHeader().getX509CertChain().isEmpty());
  }

  @Test
  void assertThatCreateWalletUnitAttestation_containsNonceButNotKid() throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "nonce");

    assertEquals("key-attestation+jwt", jwt.getHeader().getType().getType());

    assertFalse(jwt.getHeader().toJSONObject().containsKey("kid"));

    assertTrue(jwt.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
  }

  @Test
  void assertThatCreateWalletUnitAttestation_handlesEmptyNonce() throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "");

    assertEquals(8, jwt.getJWTClaimsSet().toJSONObject().size());
    assertTrue(jwt.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
    assertEquals("", jwt.getJWTClaimsSet().toJSONObject().get("nonce"));
  }

  @Test
  void assertThatCreateWalletUnitAttestation_containsKeyStorageAndUserAuthentication()
      throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), "nonce");

    assertEquals(List.of("iso_18045_high"),
        jwt.getJWTClaimsSet().getStringListClaim("key_storage"));
    assertEquals(
        List.of("iso_18045_high"), jwt.getJWTClaimsSet().getStringListClaim("user_authentication"));
  }

  @Test
  void assertThatCreateWalletUnitAttestation_handlesNullNonce() throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwt = service.createWalletUnitAttestation(jwk.toString(), null);

    assertEquals(7, jwt.getJWTClaimsSet().toJSONObject().size());
    assertFalse(jwt.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
  }

  private void verifyJwtSignature(SignedJWT jwt, ECPublicKey publicKey) throws JOSEException {
    assertTrue(jwt.verify(new ECDSAVerifier(publicKey)));
  }

  private ECKey createJWK() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
    gen.initialize(Curve.P_256.toECParameterSpec());
    KeyPair keyPair = gen.generateKeyPair();

    return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).build();
  }
}
