package com.historyai.controller;

import com.historyai.dto.ErrorResponse;
import com.historyai.dto.ErrorResponse.FieldError;
import com.historyai.exception.CharacterAlreadyExistsException;
import com.historyai.exception.CharacterNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler providing centralized error handling across all REST endpoints.
 * Returns standardized error responses following RFC 7807 Problem Details specification.
 * Includes structured logging and trace ID propagation for debugging.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_ID_KEY = "traceId";
    private static final String UNKNOWN_ERROR_MESSAGE = "An unexpected error occurred";

    /**
     * Handles CharacterNotFoundException - returns 404 NOT_FOUND.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(CharacterNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCharacterNotFound(
            CharacterNotFoundException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logError(traceId, request, ex, HttpStatus.NOT_FOUND);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles CharacterAlreadyExistsException - returns 409 CONFLICT.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(CharacterAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCharacterAlreadyExists(
            CharacterAlreadyExistsException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logWarn(traceId, request, ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles IllegalArgumentException - returns 400 BAD_REQUEST.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logWarn(traceId, request, ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles NoSuchElementException - returns 404 NOT_FOUND.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(
            NoSuchElementException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logWarn(traceId, request, ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles MethodArgumentNotValidException - returns 400 BAD_REQUEST with field errors.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response with field validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logger.warn("[{}] Validation failed for {} - {}", traceId, request.getRequestURI(), 
                ex.getMessage());

        List<FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .collect(Collectors.toList());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed for one or more fields",
                request.getRequestURI()
        ).withTraceId(traceId)
         .withFieldErrors(fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles ConstraintViolationException - returns 400 BAD_REQUEST.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logger.warn("[{}] Constraint violation for {} - {}", traceId, request.getRequestURI(),
                ex.getMessage());

        List<FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(this::extractFieldError)
                .collect(Collectors.toList());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Constraint violation",
                request.getRequestURI()
        ).withTraceId(traceId)
         .withFieldErrors(fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles HttpMessageNotReadableException - returns 400 BAD_REQUEST.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logWarn(traceId, request, ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Malformed request body",
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles MethodArgumentTypeMismatchException - returns 400 BAD_REQUEST.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logger.warn("[{}] Type mismatch for {} - {}", traceId, request.getRequestURI(),
                ex.getMessage());

        String message = String.format("Parameter '%s' expected type '%s', got '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getValue());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles HttpRequestMethodNotSupportedException - returns 405 METHOD_NOT_ALLOWED.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logWarn(traceId, request, ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                String.format("HTTP method '%s' not supported for this endpoint", ex.getMethod()),
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handles HttpMediaTypeNotSupportedException - returns 415 UNSUPPORTED_MEDIA_TYPE.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logWarn(traceId, request, ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
                String.format("Media type '%s' not supported", ex.getContentType()),
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    /**
     * Handles NoHandlerFoundException - returns 404 NOT_FOUND.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logger.warn("[{}] No handler found for {} {}", traceId, request.getMethod(),
                request.getRequestURI());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                "Endpoint not found",
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles all uncaught exceptions - returns 500 INTERNAL_SERVER_ERROR.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String traceId = getTraceId();
        logError(traceId, request, ex, HttpStatus.INTERNAL_SERVER_ERROR);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                UNKNOWN_ERROR_MESSAGE,
                request.getRequestURI()
        ).withTraceId(traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Extracts FieldError from ConstraintViolation.
     *
     * @param violation the constraint violation
     * @return FieldError object
     */
    private FieldError extractFieldError(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        int lastDot = field.lastIndexOf('.');
        if (lastDot > 0) {
            field = field.substring(lastDot + 1);
        }
        return new FieldError(field, violation.getMessage(), violation.getInvalidValue());
    }

    /**
     * Gets trace ID from MDC or generates new one.
     *
     * @return trace ID string
     */
    private String getTraceId() {
        return Optional.ofNullable(MDC.get(TRACE_ID_KEY))
                .orElse("unknown");
    }

    /**
     * Logs error with full stack trace at ERROR level.
     */
    private void logError(String traceId, HttpServletRequest request, Exception ex,
            HttpStatusCode status) {
        logger.error("[{}] Error processing {} {} - Status: {} - Message: {}",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                status.value(),
                ex.getMessage(),
                ex);
    }

    /**
     * Logs warning at WARN level without full stack trace for expected errors.
     */
    private void logWarn(String traceId, HttpServletRequest request, Exception ex) {
        logger.warn("[{}] Warning processing {} {} - Message: {}",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
    }
}
