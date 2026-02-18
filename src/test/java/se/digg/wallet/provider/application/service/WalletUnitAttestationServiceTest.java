// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
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
    Map<String, Object> status = jwt.getJWTClaimsSet().getJSONObjectClaim("status");
    Map<String, Object> statusList = (Map<String, Object>) status.get("status_list");
    assertEquals(412, statusList.get("idx"));
    assertEquals("https://revocation_url/statuslists/1", statusList.get("uri"));
  }

  @SuppressWarnings("unchecked")
  private static void verifyEudiWalletInfoClaim(SignedJWT jwt) throws ParseException {
    Map<String, Object> eudiWalletInfo =
        jwt.getJWTClaimsSet().getJSONObjectClaim("eudi_wallet_info");
    Map<String, Object> generalInfo = (Map<String, Object>) eudiWalletInfo.get("general_info");
    assertEquals("Diggidigg-id", generalInfo.get("wallet_solution_id"));
    assertEquals("Digg", generalInfo.get("wallet_provider_name"));
    assertEquals("0.0.1", generalInfo.get("wallet_solution_version"));
    assertEquals("UNCERTIFIED", generalInfo.get("wallet_solution_certification_information"));

    Map<String, Object> wscdInfo = (Map<String, Object>) eudiWalletInfo.get("wscd_info");
    assertEquals("UNCERTIFIED", wscdInfo.get("wscd_certification_information"));
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

  @SuppressWarnings("unchecked")
  @Test
  void assertThatCreateWalletUnitAttestationV2_givenValidJwk_shouldSucceed() throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwt = service.createWalletUnitAttestationV2(jwk.toString(), "nonce");

    assertNotNull(jwt);
    assertEquals("Digg", jwt.getJWTClaimsSet().getIssuer());

    verifyAttestedKeysClaim(jwt, jwk);
    verifyEudiWalletInfoClaim(jwt);
    verifyStatusClaim(jwt);
    verifyJwtSignature(jwt, keystoreProperties.getPublicKey());
  }

  @Test
  void assertThatCreateWalletUnitAttestationV2_hasX5cHeader() throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwt = service.createWalletUnitAttestationV2(jwk.toString(), "nonce");

    assertNotNull(jwt.getHeader().getX509CertChain());
    assertFalse(jwt.getHeader().getX509CertChain().isEmpty());
  }

  @Test
  void assertThatV2_containsNonceButNotKid() throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwtV2 = service.createWalletUnitAttestationV2(jwk.toString(), "nonce");

    assertEquals("key-attestation+jwt", jwtV2.getHeader().getType().getType());

    assertFalse(jwtV2.getHeader().toJSONObject().containsKey("kid"));

    assertTrue(jwtV2.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
  }

  @Test
  void assertThatV2_handlesEmptyNonce() throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwtV2 = service.createWalletUnitAttestationV2(jwk.toString(), "");

    assertEquals(7, jwtV2.getJWTClaimsSet().toJSONObject().size());
    assertTrue(jwtV2.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
    assertEquals("", jwtV2.getJWTClaimsSet().toJSONObject().get("nonce"));
  }

  @Test
  void assertThatV2_handlesNullNonce() throws Exception {
    ECKey jwk = createJWK();

    SignedJWT jwtV2 = service.createWalletUnitAttestationV2(jwk.toString(), null);

    assertEquals(6, jwtV2.getJWTClaimsSet().toJSONObject().size());
    assertFalse(jwtV2.getJWTClaimsSet().toJSONObject().containsKey("nonce"));
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
