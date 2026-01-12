// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.config;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "wua.keystore")
public record WuaKeystoreProperties(
    Resource location,
    String password,
    String alias,
    String type,
    String eudiWalletInfo,
    String status,
    String issuer,
    int validityHours) {

  public ECPrivateKey getSigningKey() {
    try {
      KeyStore keyStore = KeyStore.getInstance(type());
      keyStore.load(location().getInputStream(), password().toCharArray());
      PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias(), password().toCharArray());

      return (ECPrivateKey) privateKey;
    } catch (Throwable e) {
      throw new WalletRuntimeException("Failed to load signing key from filesystem", e);
    }
  }

  public ECPublicKey getPublicKey() {
    try {
      KeyStore keyStore = KeyStore.getInstance(type());
      keyStore.load(location().getInputStream(), password().toCharArray());
      Certificate cert = keyStore.getCertificate(alias());
      return (ECPublicKey) cert.getPublicKey();
    } catch (Throwable e) {
      throw new WalletRuntimeException("Failed to load public key from filesystem", e);
    }
  }

  public List<X509Certificate> getCertificateChain() {
    try {
      KeyStore keyStore = KeyStore.getInstance(type());
      keyStore.load(location().getInputStream(), password().toCharArray());
      return Arrays.stream(keyStore.getCertificateChain(alias()))
          .map(c -> (X509Certificate) c)
          .collect(Collectors.toList());
    } catch (Throwable e) {
      throw new WalletRuntimeException("Failed to load certificate chain from filesystem", e);
    }
  }
}
