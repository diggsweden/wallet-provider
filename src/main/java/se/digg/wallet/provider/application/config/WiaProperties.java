// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "wia")
@Validated
public record WiaProperties(
    String clientId,
    String walletName,
    String walletVersion,
    String walletLink,
    String walletSolutionCertificationInformation,
    @Min(1) @Max(1439) int validityMinutes,
    String status,
    @Min(31) int statusMaintenanceDays) {
}
