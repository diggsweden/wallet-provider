// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2
package se.digg.wallet.provider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ActuatorHealthTest {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void isHealthy() {
    ResponseEntity<String> response =
        testRestTemplate.exchange("/actuator/health", HttpMethod.GET, null, String.class);
    String body = response.getBody();

    assertNotNull(body);
    assertEquals("{\"status\":\"UP\"}", body);
  }

}
