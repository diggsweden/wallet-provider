// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;

/**
 * Fuzz tests for {@link LoggingFilter}. Covers the correlation ID handling - the only logic it runs
 * on every request regardless of configuration - via the real {@code
 * getOrGenerateCorrelationId} method (via reflection, since it is private), checking that whatever
 * a caller sends in {@code X-Correlation-Id}/{@code X-Request-ID} can never come back out
 * containing control characters or CRLF, since the result is echoed into a response header and
 * written into MDC/log output. Also covers {@code getPayload}'s byte-to-string truncation, which
 * runs on attacker-supplied request/response bodies when body logging is enabled. Uses JUnit 5 +
 * Jazzer.
 */
public class LoggingFilterFuzzTest {

  private static final String X_CORRELATION_ID = "X-Correlation-Id";
  private static final String X_REQUEST_ID = "X-Request-ID";
  private static final int MAX_PAYLOAD_LENGTH = 10_000;
  private static final String TRUNCATED_SUFFIX = "...[truncated]";

  private LoggingFilter filter;
  private Method getOrGenerateCorrelationId;
  private Method getPayload;

  @BeforeEach
  void setUp() throws ReflectiveOperationException {
    filter = new LoggingFilter();
    getOrGenerateCorrelationId = LoggingFilter.class.getDeclaredMethod(
        "getOrGenerateCorrelationId", HttpServletRequest.class);
    getOrGenerateCorrelationId.setAccessible(true);

    getPayload = LoggingFilter.class.getDeclaredMethod("getPayload", byte[].class);
    getPayload.setAccessible(true);

    Field maxPayloadLength = LoggingFilter.class.getDeclaredField("maxPayloadLength");
    maxPayloadLength.setAccessible(true);
    maxPayloadLength.set(filter, MAX_PAYLOAD_LENGTH);
  }

  private String correlationIdFor(String correlationIdHeader, String requestIdHeader)
      throws ReflectiveOperationException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(X_CORRELATION_ID)).thenReturn(correlationIdHeader);
    when(request.getHeader(X_REQUEST_ID)).thenReturn(requestIdHeader);
    return (String) getOrGenerateCorrelationId.invoke(filter, request);
  }

  private void assertSafeCorrelationId(String correlationId) {
    assertNotNull(correlationId);
    assertFalse(correlationId.isEmpty());
    assertFalse(
        correlationId.chars().anyMatch(c -> c < 0x20 || c == 0x7F),
        () -> "correlation id leaked a control character: " + correlationId);
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzCorrelationIdHeader(FuzzedDataProvider data) throws ReflectiveOperationException {
    assertSafeCorrelationId(correlationIdFor(data.consumeRemainingAsString(), null));
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzRequestIdHeaderFallback(FuzzedDataProvider data)
      throws ReflectiveOperationException {
    assertSafeCorrelationId(correlationIdFor(null, data.consumeRemainingAsString()));
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzBothHeadersPresent(FuzzedDataProvider data) throws ReflectiveOperationException {
    String correlationIdValue = data.consumeString(data.remainingBytes() / 2);
    String requestIdValue = data.consumeRemainingAsString();
    assertSafeCorrelationId(correlationIdFor(correlationIdValue, requestIdValue));
  }

  @FuzzTest(maxDuration = "5s")
  public void fuzzGetPayload(FuzzedDataProvider data) throws ReflectiveOperationException {
    byte[] content = data.consumeRemainingAsBytes();
    String result = (String) getPayload.invoke(filter, (Object) content);

    assertNotNull(result);
    assertTrue(result.length() <= MAX_PAYLOAD_LENGTH + TRUNCATED_SUFFIX.length(),
        () -> "payload was not truncated to the configured max length: " + result.length());
  }
}
