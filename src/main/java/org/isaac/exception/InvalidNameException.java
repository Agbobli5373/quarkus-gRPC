package org.isaac.exception;

/**
 * Exception thrown when a user name doesn't meet the required format or
 * business rules.
 * 
 * This exception handles name-specific validation rules such as:
 * - Name length requirements
 * - Character restrictions
 * - Business-specific naming rules
 */
public class InvalidNameException extends ValidationException {

    private final String name;

    public InvalidNameException(String name, String message) {
        super(message);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}