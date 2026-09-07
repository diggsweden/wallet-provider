// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service.exception;

/**
 * Indicates that a client supplied an invalid parameter.
 */
public class InvalidWuaRequestParameterException extends WalletRuntimeException {

  public InvalidWuaRequestParameterException(Throwable cause) {
    super(cause);
  }

  public InvalidWuaRequestParameterException(String message, Throwable cause) {
    super(message, cause);
  }
}
