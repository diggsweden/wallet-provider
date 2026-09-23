// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service.exception;

/**
 * Indicates that a client supplied an invalid parameter when requesting a Key Attestation.
 */
public class InvalidKeyAttestationRequestParameterException extends WalletRuntimeException {

  public InvalidKeyAttestationRequestParameterException(String message) {
    super(message);
  }

  public InvalidKeyAttestationRequestParameterException(String message, Throwable cause) {
    super(message, cause);
  }
}
