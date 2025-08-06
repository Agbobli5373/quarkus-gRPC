package org.isaac.exception;

/**
 * Exception thrown when attempting to create or update a user with an email
 * that already exists in the system.
 * 
 * This is a specific type of validation exception that handles email uniqueness
 * business rules.
 */
public class DuplicateEmailException extends ValidationException {

    private final String email;

    public DuplicateEmailException(String email) {
        super(String.format("User with email '%s' already exists", email));
        this.email = email;
    }

    public DuplicateEmailException(String email, String message) {
        super(message);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}