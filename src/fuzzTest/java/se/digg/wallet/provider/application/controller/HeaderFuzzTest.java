// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import org.springframework.http.HttpHeaders;

/**
 * Fuzz tests for HTTP header validation. Uses JUnit 5 + Jazzer.
 */
public class HeaderFuzzTest {

  @FuzzTest(maxDuration = "5s")
  public void fuzzHeaderName(FuzzedDataProvider data) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(data.consumeRemainingAsString(), "test-value");
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzHeaderValue(FuzzedDataProvider data) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Test-Header", data.consumeRemainingAsString());
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzAuthorizationHeader(FuzzedDataProvider data) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, data.consumeRemainingAsString());
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzContentTypeHeader(FuzzedDataProvider data) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_TYPE, data.consumeRemainingAsString());
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzAcceptHeader(FuzzedDataProvider data) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.ACCEPT, data.consumeRemainingAsString());
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzMultipleHeaders(FuzzedDataProvider data) {
    String header1 = data.consumeString(data.remainingBytes() / 4);
    String header2 = data.consumeString(data.remainingBytes() / 3);
    String value1 = data.consumeString(data.remainingBytes() / 2);
    String value2 = data.consumeRemainingAsString();

    HttpHeaders headers = new HttpHeaders();
    headers.set(header1, value1);
    headers.set(header2, value2);
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzLongHeaderValue(FuzzedDataProvider data) {
    String baseValue = data.consumeRemainingAsString();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      sb.append(baseValue);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Long-Header", sb.toString());
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzSpecialCharacterHeader(FuzzedDataProvider data) {
    String specialValue = data.consumeRemainingAsString();
    String safeValue = specialValue.replaceAll("[\\x00-\\x1F\\x7F]", "?");
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Special-Header", safeValue);
  }
}
