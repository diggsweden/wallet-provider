// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.model;

import java.util.UUID;

public record WalletUnitAttestationDtoV2(UUID walletId, String jwk, String nonce) {
}
