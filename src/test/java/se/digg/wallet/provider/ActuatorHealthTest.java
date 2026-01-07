// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
public class ActuatorHealthTest {

  @Autowired
  private RestTestClient restClient;

  @Test
  void isHealthy() {
    restClient.get()
        .uri("/actuator/health")
        .exchangeSuccessfully()
        .expectBody(String.class)
        .isEqualTo("""
            {"groups":["liveness","readiness"],"status":"UP"}""");
  }
}
