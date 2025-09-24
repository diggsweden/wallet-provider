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
public final class WuaKeystoreProperties {

  private final KeyStore keyStore;
  private final String alias;
  private final String password;

  public WuaKeystoreProperties(Resource location, String password, String alias, String type) {
    this.alias = alias;
    this.password = password;
    try {
      this.keyStore = KeyStore.getInstance(type);
      this.keyStore.load(location.getInputStream(), password.toCharArray());
    } catch (Exception e) {
      throw new RuntimeException("Failed to load keystore from filesystem", e);
    }
  }

  public ECPrivateKey getSigningKey() {
    try {
      PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
      return (ECPrivateKey) privateKey;
    } catch (Exception e) {
      throw new RuntimeException("Failed to load signing key from keystore", e);
    }
  }

  public ECPublicKey getPublicKey() {
    try {
      Certificate cert = keyStore.getCertificate(alias);
      return (ECPublicKey) cert.getPublicKey();
    } catch (Exception e) {
      throw new RuntimeException("Failed to load public key from keystore", e);
    }
  }

  public String getAlias() {
    return alias;
  }
}
