// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.digg.wallet.provider.application.config.WalletRuntimeException;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;

@Service
public class WalletUnitAttestationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WalletUnitAttestationService.class);

  private final WuaKeystoreProperties keystoreProperties;
  private final ObjectMapper objectMapper;

  public WalletUnitAttestationService(
      WuaKeystoreProperties keystoreProperties, ObjectMapper objectMapper) {
    this.keystoreProperties = keystoreProperties;
    this.objectMapper = objectMapper.copy();
  }

  public SignedJWT createWalletUnitAttestation(String walletPublicKeyJwk) {
    try {
      ECKey attestedKey = ECKey.parse(walletPublicKeyJwk);
      List<Map<String, Object>> attestedKeys = List.of(attestedKey.toJSONObject());

      Map<String, Object> claims = new HashMap<>();
      claims.put("eudi_wallet_info", getEudiWalletInfo());
      claims.put("status", getStatus());
      claims.put("attested_keys", attestedKeys);

      ECPrivateKey signingKey = keystoreProperties.getSigningKey();
      String keyId = keystoreProperties.alias();
      List<X509Certificate> certificateChain = keystoreProperties.getCertificateChain();
      String issuer = keystoreProperties.issuer();
      Duration validity = Duration.ofHours(keystoreProperties.validityHours());

      Instant now = Instant.now();

      JWTClaimsSet.Builder claimsBuilder =
          new JWTClaimsSet.Builder()
              .issuer(issuer)
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plus(validity)));

      claims.forEach(claimsBuilder::claim);

      JWTClaimsSet claimsSet = claimsBuilder.build();

      List<Base64> x5c =
          certificateChain.stream()
              .map(
                  c -> {
                    try {
                      return Base64.encode(c.getEncoded());
                    } catch (CertificateEncodingException e) {
                      throw new WalletRuntimeException(e);
                    }
                  })
              .toList();

      JWSHeader header =
          new JWSHeader.Builder(JWSAlgorithm.ES256)
              .keyID(keyId)
              .type(new JOSEObjectType("keyattestation+jwt"))
              .x509CertChain(x5c)
              .build();

      SignedJWT signedJwt = new SignedJWT(header, claimsSet);

      JWSSigner signer = new ECDSASigner(signingKey);
      signedJwt.sign(signer);

      return signedJwt;
    } catch (Throwable e) {
      LOGGER.error("Could not create attestation.");
      return null;
    }
  }

  private Map<String, Object> getStatus() throws JsonProcessingException {
    return objectMapper.readValue(keystoreProperties.status(), new TypeReference<>() {});
  }

  private Map<String, Object> getEudiWalletInfo() throws JsonProcessingException {
    return objectMapper.readValue(keystoreProperties.eudiWalletInfo(), new TypeReference<>() {});
  }
}
