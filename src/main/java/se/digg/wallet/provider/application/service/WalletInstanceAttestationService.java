// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.cert.CertificateEncodingException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Service;
import se.digg.wallet.provider.application.config.WiaProperties;
import se.digg.wallet.provider.application.config.WuaKeystoreProperties;
import se.digg.wallet.provider.application.service.exception.InvalidWiaRequestParameterException;
import se.digg.wallet.provider.application.service.exception.WalletRuntimeException;

@Service
public class WalletInstanceAttestationService {

  private final WuaKeystoreProperties keystoreProperties;
  private final WiaProperties wiaProperties;

  public WalletInstanceAttestationService(
      WuaKeystoreProperties keystoreProperties, WiaProperties wiaProperties) {
    this.keystoreProperties = keystoreProperties;
    this.wiaProperties = wiaProperties;
  }

  public SignedJWT createWalletInstanceAttestation(String publicKeyJwk) {
    ECKey publicKey;
    if (publicKeyJwk == null || publicKeyJwk.isBlank()) {
      throw new InvalidWiaRequestParameterException("jwk must not be empty.");
    }
    try {
      publicKey = EcKeyParser.parse(publicKeyJwk);
    } catch (ParseException e) {
      throw new InvalidWiaRequestParameterException("Invalid wallet public key JWK.", e);
    }
    if (publicKey.isPrivate()) {
      throw new InvalidWiaRequestParameterException("Private keys are not accepted.");
    }
    try {
      var certificateChain = keystoreProperties.getCertificateChain().stream()
          .map(certificate -> {
            try {
              return Base64.encode(certificate.getEncoded());
            } catch (CertificateEncodingException e) {
              throw new WalletRuntimeException("Could not create attestation.", e);
            }
          }).toList();
      var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
          .type(new JOSEObjectType("oauth-client-attestation+jwt"))
          .x509CertChain(certificateChain).build();
      var now = Instant.now();
      var jwt = new SignedJWT(header,
          new JWTClaimsSet.Builder()
              .issuer(keystoreProperties.issuer())
              .subject(wiaProperties.clientId())
              .issueTime(Date.from(now))
              .expirationTime(
                  Date.from(now.plus(Duration.ofMinutes(wiaProperties.validityMinutes()))))
              .claim("wallet_name", wiaProperties.walletName())
              .claim("wallet_version", wiaProperties.walletVersion())
              .claim("wallet_link", wiaProperties.walletLink())
              .claim("wallet_solution_certification_information",
                  wiaProperties.walletSolutionCertificationInformation())
              .claim("cnf", Map.of("jwk", publicKey.toJSONObject()))
              .claim("client_status", Map.of(
                  "status", JSONObjectUtils.parse(wiaProperties.status()),
                  "exp",
                  now.plus(Duration.ofDays(wiaProperties.statusMaintenanceDays()))
                      .getEpochSecond()))
              .build());
      jwt.sign(new ECDSASigner(keystoreProperties.getSigningKey()));
      return jwt;
    } catch (JOSEException | ParseException e) {
      throw new WalletRuntimeException("Could not create attestation.", e);
    }
  }
}
