// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service.exception;

/**
 * Indicates that a client supplied an invalid parameter when requesting a WIA.
 */
public class InvalidWiaRequestParameterException extends WalletRuntimeException {

  public InvalidWiaRequestParameterException(String message) {
    super(message);
  }

  public InvalidWiaRequestParameterException(String message, Throwable cause) {
    super(message, cause);
  }
}
