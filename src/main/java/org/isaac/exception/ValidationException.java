package org.isaac.exception;

/**
 * Base exception for validation errors in the user service.
 * This exception is thrown when business validation rules are violated.
 * 
 * Learning objectives:
 * - Understand custom exception hierarchies
 * - Learn about business validation vs input validation
 * - Understand exception handling in reactive streams
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}