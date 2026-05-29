// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class LoggingContextFilter extends OncePerRequestFilter {

  private final Logger log = LoggerFactory.getLogger(LoggingContextFilter.class);

  private static final String REQUEST_ID = "correlationId";
  private static final Pattern VALID_HEADER_FORMAT = Pattern.compile("[a-zA-Z0-9_-]{1,64}");
  public static final String X_CORRELATION_ID = "X-Correlation-Id";

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain) throws ServletException, IOException {
    try {
      String correlationId = resolveCorrelationId(request.getHeader(X_CORRELATION_ID));
      MDC.put(REQUEST_ID, correlationId);
      response.setHeader(X_CORRELATION_ID, correlationId);
      chain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  private String resolveCorrelationId(String headerValue) {
    if (headerValue == null) {
      return UUID.randomUUID().toString();
    }
    if (VALID_HEADER_FORMAT.matcher(headerValue).matches()) {
      return headerValue;
    }
    String newId = UUID.randomUUID().toString();
    log.warn("Replacing poorly formatted or potentially malicious id header with {}", newId);
    return newId;
  }

}
