// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import com.nimbusds.jose.JOSEException;
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
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;
import se.digg.wallet.provider.application.service.exception.InvalidKeyAttestationRequestParameterException;
import se.digg.wallet.provider.application.service.exception.WalletRuntimeException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class KeyAttestationService {

  private final Logger log = LoggerFactory.getLogger(KeyAttestationService.class);
  private final WuaKeystoreProperties keystoreProperties;
  private final ObjectMapper objectMapper;

  private static final String ATTACK_POTENTIAL_RESISTANCE = "iso_18045_high";
  private static final long ONE_YEAR_IN_SECONDS = 365 * 24 * 3600L;
  private static final TypeReference<Map<String, Object>> STATUS_TYPE_REF =
      new TypeReference<>() {};

  public KeyAttestationService(
      WuaKeystoreProperties keystoreProperties, ObjectMapper objectMapper) {
    this.keystoreProperties = keystoreProperties;
    this.objectMapper = objectMapper.rebuild().build();
  }

  private SignedJWT createKeyAttestationUnsafely(List<String> walletPublicKeyJwks, String nonce)
      throws ParseException, JOSEException {
    log.debug("Trying to create KA {} nonce", nonce == null ? "without" : "with");
    if (walletPublicKeyJwks == null || walletPublicKeyJwks.isEmpty()) {
      throw new InvalidKeyAttestationRequestParameterException("jwks must not be empty.");
    }
    List<Map<String, Object>> attestedKeys = new ArrayList<>();
    ECKey firstKey = null;
    for (String jwkString : walletPublicKeyJwks) {
      if (jwkString == null || jwkString.isBlank()) {
        throw new InvalidKeyAttestationRequestParameterException("jwk must not be empty.");
      }
      ECKey attestedKey = ECKey.parse(jwkString);
      if (attestedKey.isPrivate()) {
        throw new InvalidKeyAttestationRequestParameterException("Private keys are not accepted.");
      }
      if (firstKey == null) {
        firstKey = attestedKey;
      }
      attestedKeys.add(attestedKey.toJSONObject());
    }

    ECPrivateKey signingKey = keystoreProperties.getSigningKey();
    List<X509Certificate> certificateChain = keystoreProperties.getCertificateChain();
    Duration validity = Duration.ofHours(keystoreProperties.validityHours());

    Instant now = Instant.now();

    Map<String, Object> keyStorageStatus =
        Map.of(
            "status", getStatus(),
            "exp", (System.currentTimeMillis() / 1000) + ONE_YEAR_IN_SECONDS);

    var claimsSet =
        new JWTClaimsSet.Builder()
            .issuer(keystoreProperties.issuer())
            .subject(firstKey.computeThumbprint().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(validity)))
            .claim("certification", "http://example.com/cert")
            .claim("key_storage_status", keyStorageStatus)
            .claim("attested_keys", attestedKeys)
            .claim("nonce", nonce)
            .claim("key_storage", List.of(ATTACK_POTENTIAL_RESISTANCE))
            .claim("user_authentication", List.of(ATTACK_POTENTIAL_RESISTANCE))
            .build();

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
            // REQUIRED, MUST be key-attestation+jwt
            .type(new JOSEObjectType("key-attestation+jwt"))
            .x509CertChain(x5c)
            .build();

    SignedJWT signedJwt = new SignedJWT(header, claimsSet);

    JWSSigner signer = new ECDSASigner(signingKey);
    signedJwt.sign(signer);

    log.debug("Successfully created KA");
    return signedJwt;
  }

  public SignedJWT createKeyAttestation(List<String> walletPublicKeyJwks, String nonce) {
    try {
      return createKeyAttestationUnsafely(walletPublicKeyJwks, nonce);
    } catch (ParseException e) {
      throw new InvalidKeyAttestationRequestParameterException(
          "Invalid wallet public key JWK.", e);
    } catch (WalletRuntimeException e) {
      throw e;
    } catch (JOSEException | RuntimeException e) {
      log.warn("Could not create KA", e);
      throw new WalletRuntimeException("Could not create attestation.", e);
    }
  }

  public SignedJWT createKeyAttestation(String walletPublicKeyJwk, String nonce) {
    return createKeyAttestation(
        walletPublicKeyJwk == null ? List.of() : List.of(walletPublicKeyJwk), nonce);
  }

  private Map<String, Object> getStatus() throws JacksonException {
    return objectMapper.readValue(keystoreProperties.status(), STATUS_TYPE_REF);
  }
}
