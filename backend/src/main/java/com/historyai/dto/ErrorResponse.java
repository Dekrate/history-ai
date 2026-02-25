package com.historyai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response DTO returned to clients when an error occurs.
 * Follows RFC 7807 Problem Details specification with extensions for better debugging.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String traceId;
    private List<FieldError> fieldErrors;
    private Map<String, Object> details;

    /**
     * Default constructor for JSON serialization.
     */
    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    /**
     * Constructs a new ErrorResponse with basic error information.
     *
     * @param status  the HTTP status code
     * @param error   the HTTP status reason phrase
     * @param message the detail message
     * @param path    the request path that caused the error
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * Builder method for setting trace ID.
     *
     * @param traceId the distributed tracing ID
     * @return this ErrorResponse for method chaining
     */
    public ErrorResponse withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * Builder method for setting field validation errors.
     *
     * @param fieldErrors list of field-level validation errors
     * @return this ErrorResponse for method chaining
     */
    public ErrorResponse withFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
        return this;
    }

    /**
     * Builder method for setting additional error details.
     *
     * @param details additional context map
     * @return this ErrorResponse for method chaining
     */
    public ErrorResponse withDetails(Map<String, Object> details) {
        this.details = details;
        return this;
    }

    // Getters and Setters

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    /**
     * Represents a single field-level validation error.
     */
    public static class FieldError {

        private String field;
        private String message;
        private Object rejectedValue;

        public FieldError() {
        }

        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
    }
}
