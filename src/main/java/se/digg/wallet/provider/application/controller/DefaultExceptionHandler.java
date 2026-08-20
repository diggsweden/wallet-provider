// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import static se.digg.wallet.provider.application.config.LoggingContextFilter.MDC_TRANSACTION_ID;
import static se.digg.wallet.provider.application.controller.ProblemType.INTERNAL;
import static se.digg.wallet.provider.application.controller.ProblemType.REQUEST_ARGUMENT_NOT_VALID;
import static se.digg.wallet.provider.application.controller.ProblemType.REQUEST_VALIDATION_FAILURE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import se.digg.wallet.provider.api.v0.model.ProblemResponse;
import se.digg.wallet.provider.application.config.WalletRuntimeException;


@RestControllerAdvice
public class DefaultExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExceptionHandler.class);
  private static final String ABOUT_BLANK = "about:blank";

  private final HttpServletRequest httpServletRequest;

  DefaultExceptionHandler(HttpServletRequest httpServletRequest) {
    this.httpServletRequest = httpServletRequest;
  }

  /*
   * Handle Constraint Violation Exception Occurs when validation fails on query parameters, path
   * variables, or service layer methods. Managed by a class-level @Validated annotation.
   */
  @ExceptionHandler({ConstraintViolationException.class})
  public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e,
      WebRequest request) {

    var method = httpServletRequest.getMethod();
    var path = httpServletRequest.getServletPath();

    var problemResponse = buildProblemResponse(REQUEST_ARGUMENT_NOT_VALID)
        .detail(e.getLocalizedMessage())
        .instance(path);


    var violations = Map.of(
        "violations",
        e.getConstraintViolations().stream().map(violation -> MessageFormat.format("{0} {1} {2}",
            violation.getRootBeanClass().getName(),
            violation.getPropertyPath().toString(),
            violation.getMessage())).toList());
    logDebug("Request argument not valid", method, path, violations);

    return createResponseEntity(problemResponse.build());
  }

  /*
   * Handle Method Argument Not Valid Exception Occurs when processing the request body, and a field
   * value does not meet validation criteria. Activated on model class fields annotated with @Valid
   * (@NotNull, @NotBlank, @Size etc.)
   */
  @Override
  protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status,
      WebRequest request) {

    var method = httpServletRequest.getMethod();
    var path = httpServletRequest.getServletPath();

    var problemResponse = buildProblemResponse(REQUEST_VALIDATION_FAILURE)
        .detail("Request body field value(s) does not validate.")
        .instance(path);

    var errors = Map.of(
        "globalErrors", e.getBindingResult().getGlobalErrors().stream()
            .map(ObjectError::getDefaultMessage).toList(),
        "fieldErrors", e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage).toList());
    logDebug("Input validation failure", method, path, errors);

    return createResponseEntity(problemResponse.build());
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException e, HttpHeaders headers, HttpStatusCode status,
      WebRequest request) {

    var method = httpServletRequest.getMethod();
    var path = httpServletRequest.getServletPath();

    var statusCode = HttpStatus.BAD_REQUEST;
    var problemDetailResponse = ProblemResponse.builder()
        .type(REQUEST_ARGUMENT_NOT_VALID.getUri().toString())
        .title(statusCode.getReasonPhrase())
        .status(statusCode.value())
        .detail(e.getMessage())
        .instance(httpServletRequest.getContextPath())
        .build();

    logDebug("A requested resource was not found in remote service",
        method, path, Map.of());

    return createResponseEntity(problemDetailResponse);
  }

  /*
   * Handle WalletRuntimeException. A generic application error
   *
   */
  @ExceptionHandler(WalletRuntimeException.class)
  public ResponseEntity<Object> handleWalletRuntimeException(
      WalletRuntimeException e) {

    var method = httpServletRequest.getMethod();
    var path = httpServletRequest.getServletPath();
    var problemResponse = ProblemResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .type(ABOUT_BLANK)
        .title(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .detail(e.getLocalizedMessage())
        .instance(path)
        .build();

    logDebug("Generic error.", method, path, null, e);
    return createResponseEntity(problemResponse);
  }

  /*
   * Handle exception not handled elsewhere.
   */
  @ExceptionHandler(Throwable.class)
  public ResponseEntity<Object> handleAnyException(Throwable e) {

    var method = httpServletRequest.getMethod();
    var path = httpServletRequest.getServletPath();
    var problemResponse = buildProblemResponse(INTERNAL)
        .detail(e.getLocalizedMessage())
        .instance(path)
        .build();

    logError("Unexpected exception", method, path, e);
    return createResponseEntity(problemResponse);
  }

  /*
   * For exceptions handled by the super class, map to the problem response defined by the API.
   */
  @Override
  protected ResponseEntity<Object> createResponseEntity(@Nullable Object body,
      HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

    var problemDetailResponse = ProblemResponse.builder()
        .type(ABOUT_BLANK)
        .status(statusCode.value())
        .instance(request.getContextPath());

    if (body instanceof ProblemDetail problemDetail) {
      problemDetailResponse
          .title(problemDetail.getTitle())
          .detail(problemDetail.getDetail());

    } else {
      var title = statusCode.is4xxClientError() ? HttpStatus.BAD_REQUEST.getReasonPhrase()
          : HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();

      problemDetailResponse
          .title(title)
          .detail("unknown");
    }

    return createResponseEntity(problemDetailResponse.build());
  }

  private ResponseEntity<Object> createResponseEntity(ProblemResponse problemResponse) {


    return ResponseEntity
        .status(problemResponse.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemResponse);
  }

  private ProblemResponse.Builder buildProblemResponse(ProblemType problemType) {

    return ProblemResponse.builder()
        .type(Optional.ofNullable(problemType.getUri().toASCIIString())
            .orElse(ABOUT_BLANK))
        .title(problemType.getTitle())
        .status(problemType.getHttpStatus().value());
  }

  private void logDebug(String message, String method, String path,
      @Nullable Map<String, ?> properties) {

    logDebug(message, method, path, properties, null);
  }

  private void logDebug(String message, String method, String path,
      @Nullable Map<String, ?> properties, Throwable e) {

    LOGGER.debug("{} {} {} {} transaction-id: {}", method, path, message,
        Optional.ofNullable(properties).orElse(Map.of()), MDC.get(MDC_TRANSACTION_ID), e);
  }

  private void logError(String message, String method, String path, Throwable e) {

    LOGGER.error("{} {} {} transaction-id: {}", method, path, message, MDC.get(MDC_TRANSACTION_ID),
        e);
  }
}
