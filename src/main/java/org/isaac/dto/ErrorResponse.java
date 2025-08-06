package org.isaac.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response format for REST API endpoints.
 * Provides consistent error structure across all error scenarios.
 */
public class ErrorResponse {
    private String error;
    private String message;
    private Instant timestamp;
    private List<String> details;

    // Default constructor for JSON serialization
    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    // Constructor with error and message
    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
    }

    // Constructor with error, message, and details
    public ErrorResponse(String error, String message, List<String> details) {
        this.error = error;
        this.message = message;
        this.details = details;
        this.timestamp = Instant.now();
    }

    // Getters and setters
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", details=" + details +
                '}';
    }
}