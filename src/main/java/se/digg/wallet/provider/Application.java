// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government/wallet-provider
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
