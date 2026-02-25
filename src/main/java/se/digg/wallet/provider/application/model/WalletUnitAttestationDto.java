// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.model;

public record WalletUnitAttestationDto(String jwk, String nonce) {
}
