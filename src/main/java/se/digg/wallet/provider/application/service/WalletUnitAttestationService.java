// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;

@Service
public class WalletUnitAttestationService {

  private final WuaKeystoreProperties keystoreProperties;
  private final ObjectMapper objectMapper;

  public WalletUnitAttestationService(
      WuaKeystoreProperties keystoreProperties,
      ObjectMapper objectMapper) {
    this.keystoreProperties = keystoreProperties;
    this.objectMapper = objectMapper;
  }

  public SignedJWT createWalletUnitAttestation(String walletPublicKeyJwk) throws Exception {
    ECKey attestedKey = ECKey.parse(walletPublicKeyJwk);
    List<Map<String, Object>> attestedKeys = List.of(attestedKey.toJSONObject());

    Map<String, Object> claims = new HashMap<>();
    claims.put("eudi_wallet_info", getEudiWalletInfo());
    claims.put("status", getStatus());
    claims.put("attested_keys", attestedKeys);

    return createSignedJwt(
        keystoreProperties.getSigningKey(),
        keystoreProperties.alias(),
        keystoreProperties.issuer(),
        Duration.ofHours(keystoreProperties.validityHours()),
        claims);
  }

  private Map<String, Object> getStatus() throws JsonProcessingException {
    return objectMapper.readValue(keystoreProperties.status(), new TypeReference<>() {});
  }

  private Map<String, Object> getEudiWalletInfo() throws JsonProcessingException {
    return objectMapper.readValue(
        keystoreProperties.eudiWalletInfo(), new TypeReference<>() {});
  }

  private SignedJWT createSignedJwt(
      ECPrivateKey signingKey,
      String keyId,
      String issuer,
      Duration validity,
      Map<String, Object> claims)
      throws JOSEException {
    Instant now = Instant.now();

    JWTClaimsSet.Builder claimsBuilder =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(validity)));

    if (claims != null) {
      claims.forEach(claimsBuilder::claim);
    }

    JWTClaimsSet claimsSet = claimsBuilder.build();

    JWSHeader header =
        new JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID(keyId)
            .type(new JOSEObjectType("keyattestation+jwt"))
            .build();

    SignedJWT signedJwt = new SignedJWT(header, claimsSet);

    JWSSigner signer = new ECDSASigner(signingKey);
    signedJwt.sign(signer);

    return signedJwt;
  }
}
