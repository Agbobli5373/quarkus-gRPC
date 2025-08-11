package org.isaac.exception;

/**
 * Exception thrown when attempting to access a user that doesn't exist
 * in the system.
 * <p>
 * This exception is used for operations like get, update, or delete
 * when the specified user ID cannot be found.
 */
public class UserNotFoundException extends RuntimeException {

    private final String userId;

    public UserNotFoundException(String userId) {
        super(String.format("User with ID '%s' not found", userId));
        this.userId = userId;
    }

    public UserNotFoundException(String userId, String message) {
        super(message);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}