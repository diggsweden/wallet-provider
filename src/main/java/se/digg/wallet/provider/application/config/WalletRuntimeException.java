// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.config;

public class WalletRuntimeException extends RuntimeException {
  public WalletRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public WalletRuntimeException(Throwable cause) {
    super(cause);
  }
}
