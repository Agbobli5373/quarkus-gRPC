package org.isaac.exception;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

/**
 * Centralized gRPC exception handler for converting business exceptions
 * to appropriate gRPC status codes.
 * 
 * This handler demonstrates:
 * - Proper gRPC error model implementation
 * - Exception mapping patterns
 * - Security considerations for error messages
 * - Structured error handling in reactive streams
 * 
 * Learning objectives:
 * - Understand gRPC error model and status codes
 * - Learn about exception mapping in gRPC
 * - Understand error propagation in reactive streams
 * - Learn security best practices for error handling
 */
@ApplicationScoped
public class GrpcExceptionHandler {

    private static final Logger logger = Logger.getLogger(GrpcExceptionHandler.class.getName());

    /**
     * Maps business exceptions to appropriate gRPC StatusRuntimeException.
     * 
     * This method follows gRPC best practices by:
     * - Using appropriate status codes for different error types
     * - Providing informative but secure error messages
     * - Logging detailed errors internally for debugging
     * - Sanitizing error messages for external clients
     * 
     * @param throwable the business exception to map
     * @return StatusRuntimeException with appropriate gRPC status
     */
    public StatusRuntimeException mapException(Throwable throwable) {
        // Log the original exception with full details for internal debugging
        logger.warning("Mapping exception to gRPC status: " + throwable.getClass().getSimpleName() +
                " - " + throwable.getMessage());

        if (logger.isLoggable(java.util.logging.Level.FINE)) {
            logger.fine("Exception stack trace: " + getStackTraceAsString(throwable));
        }

        return switch (throwable) {
            case UserNotFoundException e -> {
                logger.info("User not found: " + e.getUserId());
                yield Status.NOT_FOUND
                        .withDescription(sanitizeErrorMessage(e.getMessage()))
                        .asRuntimeException();
            }

            case DuplicateEmailException e -> {
                logger.info("Duplicate email attempt: " + e.getEmail());
                yield Status.ALREADY_EXISTS
                        .withDescription(sanitizeErrorMessage(e.getMessage()))
                        .asRuntimeException();
            }

            case InvalidNameException e -> {
                logger.info("Invalid name validation failed: " + e.getName());
                yield Status.INVALID_ARGUMENT
                        .withDescription(sanitizeErrorMessage(e.getMessage()))
                        .asRuntimeException();
            }

            case ValidationException e -> {
                logger.info("Validation failed: " + e.getMessage());
                yield Status.INVALID_ARGUMENT
                        .withDescription(sanitizeErrorMessage(e.getMessage()))
                        .asRuntimeException();
            }

            case IllegalArgumentException e -> {
                logger.info("Invalid argument: " + e.getMessage());
                yield Status.INVALID_ARGUMENT
                        .withDescription("Invalid request parameters")
                        .asRuntimeException();
            }

            case IllegalStateException e -> {
                logger.warning("Illegal state: " + e.getMessage());
                yield Status.FAILED_PRECONDITION
                        .withDescription("Operation cannot be performed in current state")
                        .asRuntimeException();
            }

            case UnsupportedOperationException e -> {
                logger.warning("Unsupported operation: " + e.getMessage());
                yield Status.UNIMPLEMENTED
                        .withDescription("Operation not supported")
                        .asRuntimeException();
            }

            default -> {
                // Log full details of unexpected exceptions for debugging
                logger.severe("Unexpected exception: " + throwable.getClass().getName() +
                        " - " + throwable.getMessage());
                if (throwable.getCause() != null) {
                    logger.severe("Caused by: " + throwable.getCause().getClass().getName() +
                            " - " + throwable.getCause().getMessage());
                }

                yield Status.INTERNAL
                        .withDescription("Internal server error")
                        .asRuntimeException();
            }
        };
    }

    /**
     * Maps business exceptions with additional context information.
     * 
     * This overloaded method allows providing additional context that can be
     * useful for debugging while maintaining security for external clients.
     * 
     * @param throwable the business exception to map
     * @param context   additional context information for logging
     * @return StatusRuntimeException with appropriate gRPC status
     */
    public StatusRuntimeException mapException(Throwable throwable, String context) {
        logger.warning("Exception in context '" + context + "': " + throwable.getClass().getSimpleName() +
                " - " + throwable.getMessage());

        return mapException(throwable);
    }

    /**
     * Sanitizes error messages to prevent information leakage.
     * 
     * This method ensures that error messages are informative for legitimate
     * clients but don't expose internal implementation details or sensitive
     * information that could be used maliciously.
     * 
     * Security considerations:
     * - Remove stack traces from client-facing messages
     * - Remove internal class names and package information
     * - Remove database connection details or file paths
     * - Keep messages helpful but generic
     * 
     * @param originalMessage the original error message
     * @return sanitized error message safe for external clients
     */
    private String sanitizeErrorMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            return "An error occurred";
        }

        // Remove common patterns that might expose internal details
        String sanitized = originalMessage
                // Remove package names and class references
                .replaceAll("\\b[a-z]+\\.[a-z]+\\.[A-Za-z]+", "")
                // Remove file paths
                .replaceAll("\\b[A-Za-z]:\\\\[^\\s]+", "")
                .replaceAll("\\b/[^\\s]+", "")
                // Remove SQL-like patterns
                .replaceAll("(?i)\\bselect\\b.*?\\bfrom\\b.*", "database query failed")
                .replaceAll("(?i)\\binsert\\b.*?\\binto\\b.*", "database insert failed")
                .replaceAll("(?i)\\bupdate\\b.*?\\bset\\b.*", "database update failed")
                .replaceAll("(?i)\\bdelete\\b.*?\\bfrom\\b.*", "database delete failed")
                // Remove connection strings
                .replaceAll("(?i)jdbc:[^\\s]+", "database connection")
                // Clean up extra whitespace
                .replaceAll("\\s+", " ")
                .trim();

        // Ensure the message is not empty after sanitization
        if (sanitized.isEmpty()) {
            return "An error occurred";
        }

        // Limit message length to prevent potential DoS through large error messages
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 197) + "...";
        }

        return sanitized;
    }

    /**
     * Converts throwable stack trace to string for logging purposes.
     * 
     * @param throwable the throwable to convert
     * @return stack trace as string
     */
    private String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Checks if an exception should be logged at ERROR level.
     * 
     * This method helps determine the appropriate log level based on
     * the exception type. Some exceptions are expected (like validation
     * errors) and should be logged at INFO/WARN level, while others
     * indicate serious problems and should be logged at ERROR level.
     * 
     * @param throwable the exception to check
     * @return true if should be logged at ERROR level
     */
    public boolean shouldLogAsError(Throwable throwable) {
        return switch (throwable) {
            case UserNotFoundException ignored -> false;
            case ValidationException ignored -> false;
            // case DuplicateEmailException ignored -> false;
            // case InvalidNameException ignored -> false;
            case IllegalArgumentException ignored -> false;
            default -> true;
        };
    }

    /**
     * Gets the appropriate gRPC status code for an exception without
     * creating a full StatusRuntimeException.
     * 
     * This method is useful for testing or when you need just the status
     * code for logging or metrics purposes.
     * 
     * @param throwable the exception to map
     * @return the appropriate gRPC Status.Code
     */
    public Status.Code getStatusCode(Throwable throwable) {
        return switch (throwable) {
            case UserNotFoundException ignored -> Status.Code.NOT_FOUND;
            case DuplicateEmailException ignored -> Status.Code.ALREADY_EXISTS;
            case InvalidNameException ignored -> Status.Code.INVALID_ARGUMENT;
            case ValidationException ignored -> Status.Code.INVALID_ARGUMENT;
            case IllegalArgumentException ignored -> Status.Code.INVALID_ARGUMENT;
            case IllegalStateException ignored -> Status.Code.FAILED_PRECONDITION;
            case UnsupportedOperationException ignored -> Status.Code.UNIMPLEMENTED;
            default -> Status.Code.INTERNAL;
        };
    }
}