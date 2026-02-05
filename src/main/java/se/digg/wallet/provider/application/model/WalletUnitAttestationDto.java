// SPDX-FileCopyrightText: 2025 diggsweden/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.model;

import java.util.UUID;

@Deprecated(since = "0.0.3", forRemoval = true)
public record WalletUnitAttestationDto(UUID walletId, String jwk) {
}
