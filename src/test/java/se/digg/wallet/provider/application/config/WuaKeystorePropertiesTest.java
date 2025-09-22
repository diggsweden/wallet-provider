// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WuaKeystorePropertiesTest {

  @Autowired
  private WuaKeystoreProperties properties;

  @Test
  void assertThatGetSigningKey_givenValidKeyStore_shouldReturnSigningKey() {
    ECPrivateKey key = properties.getSigningKey();

    assertNotNull(key);
    assertEquals("EC", key.getAlgorithm());
  }

  @Test
  void assertThatGetSigningKey_givenValidKeyStore_shouldReturnPublicKey() {
    ECPublicKey key = properties.getPublicKey();

    assertNotNull(key);
    assertEquals("EC", key.getAlgorithm());
  }
}
