// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.config;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "wua.keystore")
public record WuaKeystoreProperties(Resource location, String password, String alias, String type) {

  public ECPrivateKey getSigningKey() {
    try {
      KeyStore keyStore = KeyStore.getInstance(type());
      keyStore.load(location().getInputStream(), password().toCharArray());
      PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias(), password().toCharArray());

      return (ECPrivateKey) privateKey;
    } catch (Exception e) {
      throw new RuntimeException("Failed to load signing key from filesystem", e);
    }
  }

  public ECPublicKey getPublicKey() {
    try {
      KeyStore keyStore = KeyStore.getInstance(type());
      keyStore.load(location().getInputStream(), password().toCharArray());
      Certificate cert = keyStore.getCertificate(alias());
      return (ECPublicKey) cert.getPublicKey();
    } catch (Exception e) {
      throw new RuntimeException("Failed to load public key from filesystem", e);
    }
  }
}
