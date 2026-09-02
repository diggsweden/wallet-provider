// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class LoggingFilter extends OncePerRequestFilter {

  // NOTE: Body logging is disabled by default in production (log-body: false) due to
  // security concerns. The SensitiveDataMasker only masks by field name, NOT by pattern,
  // so JWTs and other sensitive data in non-standard fields will NOT be masked.
  // Enable body logging only in development environments for debugging.

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

  private static final Pattern VALID_HEADER_FORMAT = Pattern.compile("[a-zA-Z0-9_-]{1,64}");
  private static final String X_CORRELATION_ID = "X-Correlation-Id";

  public static final String MDC_TRANSACTION_ID = "transactionId";
  public static final String MDC_CORRELATION_ID = "correlationId";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private SensitiveDataMasker sensitiveDataMasker;

  @Value("${properties.logging-filter.enabled:false}")
  private boolean isLoggingEnabled;

  @Value("${properties.logging-filter.max-payload-length:10000}")
  private int maxPayloadLength;

  @Value("${properties.logging-filter.exclude-path.exact-match:}")
  private List<String> excludePathExactMatch;

  @Value("${properties.logging-filter.exclude-path.contains:}")
  private List<String> excludePathContains;

  @Value("${properties.logging-filter.log-body:false}")
  private boolean isBodyLoggingEnabled;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    // Generate Trace IDs
    String correlationId = getOrGenerateCorrelationId(request);
    String transactionId = UUID.randomUUID().toString();

    // Add to Mapped Diagnostic Context - MDC for log correlation
    MDC.put(MDC_CORRELATION_ID, correlationId);
    MDC.put(MDC_TRANSACTION_ID, transactionId);

    // Add correlation ID to response header
    response.setHeader(X_CORRELATION_ID, correlationId);

    try {
      // Only wrap request and response when both logging is enabled and body logging is enabled
      // to avoid unnecessary memory buffering (OOM risk with large responses)
      if (isLoggingEnabled && isBodyLoggingEnabled) {
        ContentCachingRequestWrapper wrappedRequest =
            new ContentCachingRequestWrapper(request, maxPayloadLength);
        ContentCachingResponseWrapper wrappedResponse =
            new ContentCachingResponseWrapper(response);

        Instant startTime = Instant.now();
        Exception capturedException = null;

        try {
          filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception e) {
          capturedException = e;
          throw e;
        } finally {
          // Calculate duration
          long durationMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();

          // Build structured log entry
          logStructuredEntry(
              correlationId,
              wrappedRequest,
              wrappedResponse,
              durationMs,
              capturedException);

          try {
            // Copy response body back
            wrappedResponse.copyBodyToResponse();
          } catch (IOException e) {
            LOGGER.error("Failed to copy response body back to original response", e);
          }
        }
      } else if (isLoggingEnabled) {
        // Logging enabled but body logging disabled - no need to wrap
        Instant startTime = Instant.now();
        Exception capturedException = null;

        try {
          filterChain.doFilter(request, response);
        } catch (Exception e) {
          capturedException = e;
          throw e;
        } finally {
          // Calculate duration
          long durationMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();

          // Build structured log entry (without body)
          logStructuredEntry(
              correlationId,
              request,
              response,
              durationMs,
              capturedException);
        }
      } else {
        // Logging disabled - just pass through without wrapping
        filterChain.doFilter(request, response);
      }
    } finally {
      // Ensure MDC is always cleared
      MDC.clear();
    }
  }

  /**
   * Gets existing correlation ID from request header or generates a new one. Validates the header
   * value against VALID_HEADER_FORMAT to prevent log injection and header injection attacks.
   */
  private String getOrGenerateCorrelationId(HttpServletRequest request) {
    String correlationId = request.getHeader(X_CORRELATION_ID);
    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = request.getHeader("X-Request-ID");
    }
    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = UUID.randomUUID().toString();
    } else if (!VALID_HEADER_FORMAT.matcher(correlationId).matches()) {
      String newId = UUID.randomUUID().toString();
      LOGGER.warn("Replacing poorly formatted or potentially malicious id header with {}", newId);
      correlationId = newId;
    }
    return correlationId;
  }

  /**
   * Builds and logs a structured log entry.
   */
  private void logStructuredEntry(
      String correlationId,
      HttpServletRequest request,
      HttpServletResponse response,
      long durationMs,
      Exception exception) {

    try {
      String timestamp = Instant.now().toString();
      int status = response instanceof ContentCachingResponseWrapper
          ? ((ContentCachingResponseWrapper) response).getStatus()
          : 200;

      doLog(timestamp, correlationId, "request", requestDetails(request), durationMs, null, status);
      doLog(timestamp, correlationId, "response", responseDetails(response), durationMs, exception,
          status);

    } catch (Throwable e) {
      LOGGER.error("Failed to create structured log entry", e);
    }
  }

  private void doLog(String timestamp, String correlationId, String key, Object details,
      long durationMs, Exception exception, int responseStatus)
      throws JacksonException {
    Map<String, Object> logEntry = new LinkedHashMap<>();

    // Basic request info
    logEntry.put("timestamp", timestamp);
    logEntry.put("id", correlationId);
    logEntry.put("type", "http_request");

    // request or response details
    logEntry.put(key, details);

    // Timing
    logEntry.put("durationMs", durationMs);

    // Error info if present
    if (exception != null) {
      logEntry.put("error", errorDetails(exception));
    }

    writeAsJsonToLogger(logEntry, responseStatus);
  }

  private Map<String, Object> requestDetails(HttpServletRequest request) {
    // Request details
    Map<String, Object> requestDetails = new LinkedHashMap<>();
    requestDetails.put("method", request.getMethod());
    requestDetails.put("path", request.getRequestURI());
    requestDetails.put("queryString", request.getQueryString());
    requestDetails.put("remoteAddress", request.getRemoteAddr());
    requestDetails.put("userAgent", request.getHeader("User-Agent"));
    requestDetails.put("contentType", request.getContentType());
    requestDetails.put("contentLength", request.getContentLength());

    // Masked headers
    requestDetails.put("headers", sensitiveDataMasker.maskHeaders(extractHeaders(request)));

    // Masked request body (only available if request was wrapped)
    if (isBodyLoggingEnabled && request instanceof ContentCachingRequestWrapper) {
      String requestBody =
          getPayload(((ContentCachingRequestWrapper) request).getContentAsByteArray());
      if (!requestBody.isEmpty()) {
        requestDetails.put("body", sensitiveDataMasker.maskJsonBody(requestBody));
      }
    }

    return requestDetails;
  }

  private Map<String, Object> responseDetails(HttpServletResponse response) {
    // Response details
    Map<String, Object> responseDetails = new LinkedHashMap<>();

    int status = response instanceof ContentCachingResponseWrapper
        ? ((ContentCachingResponseWrapper) response).getStatus()
        : 200;
    responseDetails.put("status", status);
    responseDetails.put("contentType", response.getContentType());

    // Response body (only available if response was wrapped)
    if (isBodyLoggingEnabled && response instanceof ContentCachingResponseWrapper) {
      String responseBody =
          getPayload(((ContentCachingResponseWrapper) response).getContentAsByteArray());
      if (!responseBody.isEmpty()) {
        responseDetails.put("body", sensitiveDataMasker.maskJsonBody(responseBody));
      }
    }

    return responseDetails;
  }

  private Map<String, Object> errorDetails(Exception exception) {
    Map<String, Object> errorDetails = new LinkedHashMap<>();
    errorDetails.put("type", exception.getClass().getName());
    errorDetails.put("message", exception.getMessage());

    return errorDetails;
  }

  private void writeAsJsonToLogger(Map<String, Object> logEntry, int status)
      throws JacksonException {
    // Log as JSON
    String jsonLog = objectMapper.writeValueAsString(logEntry);

    if (status >= 500) {
      LOGGER.error(jsonLog);
    } else if (status >= 400) {
      LOGGER.warn(jsonLog);
    } else {
      LOGGER.info(jsonLog);
    }
  }

  /**
   * Extracts headers from request into a map.
   */
  private Map<String, String> extractHeaders(HttpServletRequest request) {

    Map<String, String> headers = new HashMap<>();
    Collections.list(request.getHeaderNames())
        .forEach(name -> headers.put(name, request.getHeader(name)));

    return headers;
  }

  /**
   * Converts byte array to string with truncation. NOTE: Truncated JSON will be invalid;
   * maskJsonBody() falls back to pattern masking.
   */
  private String getPayload(byte[] content) {

    if (content == null || content.length == 0) {
      return "";
    }

    int length = Math.min(content.length, maxPayloadLength);
    String payload = new String(content, 0, length, StandardCharsets.UTF_8);
    if (content.length > maxPayloadLength) {
      payload += "...[truncated]";
    }

    return payload;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {

    String path = request.getRequestURI();

    return excludePathExactMatch.stream().anyMatch(path::equals)
        || excludePathContains.stream().anyMatch(path::contains);
  }
}
