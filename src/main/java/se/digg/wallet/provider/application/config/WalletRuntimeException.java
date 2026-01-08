// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.config;

import java.security.cert.CertificateEncodingException;

public class WalletRuntimeException extends RuntimeException {
  public WalletRuntimeException(String s, Throwable cause) {
    super(s, cause);
  }

  public WalletRuntimeException(CertificateEncodingException e) {
    super(e);
  }
}
