package org.isaac.validation;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.isaac.exception.DuplicateEmailException;
import org.isaac.exception.InvalidNameException;
import org.isaac.exception.ValidationException;
import org.isaac.grpc.user.UserProto.CreateUserRequest;
import org.isaac.grpc.user.UserProto.UpdateUserRequest;
import org.isaac.repository.UserRepository;

import java.util.regex.Pattern;

/**
 * Service for validating business rules related to user operations.
 * <p>
 * This validator handles business-level validation that goes beyond simple
 * input validation. It includes rules like email uniqueness, name format
 * requirements, and other domain-specific constraints.
 * <p>
 * Learning objectives:
 * - Understand business validation vs input validation
 * - Learn about custom validation logic
 * - Understand exception handling in reactive streams
 */
@ApplicationScoped
public class UserValidator {


    private final UserRepository userRepository;

    // Business rules constants
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 50;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[\\p{L}\\s'-]+$");
    @Inject
    public UserValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Validates a create user request according to business rules.
     * <p>
     * Business rules validated:
     * - Name format and length requirements
     * - Email format and uniqueness
     * - No duplicate names (business rule)
     * 
     * @param request the create user request to validate
     * @return Uni<Void> that completes successfully if validation passes,
     *         or fails with ValidationException if validation fails
     */
    public Uni<Void> validateCreateRequest(CreateUserRequest request) {
        return validateBasicFields(request.getName(), request.getEmail())
                .chain(() -> validateEmailUniqueness(request.getEmail(), null))
                .chain(() -> validateNameUniqueness(request.getName(), null));
    }

    /**
     * Validates an update user request according to business rules.
     * <p>
     * Business rules validated:
     * - Name format and length requirements
     * - Email format and uniqueness (excluding the user being updated)
     * - No duplicate names (excluding the user being updated)
     * 
     * @param request the update user request to validate
     * @return Uni<Void> that completes successfully if validation passes,
     *         or fails with ValidationException if validation fails
     */
    public Uni<Void> validateUpdateRequest(UpdateUserRequest request) {
        return validateBasicFields(request.getName(), request.getEmail())
                .chain(() -> validateEmailUniqueness(request.getEmail(), request.getId()))
                .chain(() -> validateNameUniqueness(request.getName(), request.getId()));
    }

    /**
     * Validates email uniqueness across all users.
     * 
     * @param email     the email to check for uniqueness
     * @param excludeId optional user ID to exclude from the check (for updates)
     * @return Uni<Void> that completes successfully if email is unique,
     *         or fails with DuplicateEmailException if email already exists
     */
    public Uni<Void> validateEmailUniqueness(String email, String excludeId) {
        if (email == null || email.trim().isEmpty()) {
            return Uni.createFrom().failure(
                    new ValidationException("Email cannot be null or empty"));
        }

        Uni<Boolean> emailExistsCheck;
        if (excludeId != null) {
            emailExistsCheck = userRepository.existsByEmailExcludingId(email, excludeId);
        } else {
            emailExistsCheck = userRepository.existsByEmail(email);
        }

        return emailExistsCheck
                .chain(exists -> exists
                    ? Uni.createFrom().failure(new DuplicateEmailException(email))
                    : Uni.createFrom().voidItem())
                .onFailure().transform(throwable -> {
                    if (throwable instanceof DuplicateEmailException) {
                        return throwable;
                    }
                    return new ValidationException("Error validating email uniqueness", throwable);
                });
    }

    /**
     * Validates name uniqueness across all users (business rule).
     * This is a business-specific rule that prevents duplicate names.
     * 
     * @param name      the name to check for uniqueness
     * @param excludeId optional user ID to exclude from the check (for updates)
     * @return Uni<Void> that completes successfully if name is unique,
     *         or fails with ValidationException if name already exists
     */
    public Uni<Void> validateNameUniqueness(String name, String excludeId) {
        if (name == null || name.trim().isEmpty()) {
            return Uni.createFrom().failure(
                    new ValidationException("Name cannot be null or empty"));
        }

        return userRepository.findAll()
                .filter(user -> {
                    // Case-insensitive name comparison
                    boolean nameMatches = name.trim().equalsIgnoreCase(user.getName().trim());
                    // Exclude the user being updated
                    boolean shouldExclude = excludeId != null && excludeId.equals(user.getId());
                    return nameMatches && !shouldExclude;
                })
                .collect().first()
                .chain(existingUser -> existingUser != null
                    ? Uni.createFrom().failure(new ValidationException(
                        String.format("User with name '%s' already exists", name)))
                    : Uni.createFrom().voidItem())
                .onFailure().transform(throwable -> {
                    if (throwable instanceof ValidationException) {
                        return throwable;
                    }
                    return new ValidationException("Error validating name uniqueness", throwable);
                });
    }

    /**
     * Validates basic field requirements (format, length, etc.).
     * 
     * @param name  the name to validate
     * @param email the email to validate
     * @return Uni<Void> that completes successfully if fields are valid,
     *         or fails with ValidationException if validation fails
     */
    private Uni<Void> validateBasicFields(String name, String email) {
        return Uni.createFrom().item(() -> {
            // Validate name
            validateNameFormat(name);

            // Validate email
            validateEmailFormat(email);

            return null;
        });
    }

    /**
     * Validates name format according to business rules.
     * <p>
     * Rules:
     * - Cannot be null or empty
     * - Must be between MIN_NAME_LENGTH and MAX_NAME_LENGTH characters
     * - Can only contain letters, spaces, hyphens, and apostrophes
     * - Cannot start or end with whitespace
     * 
     * @param name the name to validate
     * @throws InvalidNameException if name doesn't meet format requirements
     */
    private void validateNameFormat(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidNameException(name, "Name cannot be null or empty");
        }

        String trimmedName = name.trim();

        if (trimmedName.length() < MIN_NAME_LENGTH) {
            throw new InvalidNameException(name,
                    String.format("Name must be at least %d characters long", MIN_NAME_LENGTH));
        }

        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new InvalidNameException(name,
                    String.format("Name cannot exceed %d characters", MAX_NAME_LENGTH));
        }

        if (!NAME_PATTERN.matcher(trimmedName).matches()) {
            throw new InvalidNameException(name,
                    "Name can only contain letters, spaces, hyphens, and apostrophes");
        }

        // Check for leading/trailing whitespace in original name
        if (!name.equals(trimmedName)) {
            throw new InvalidNameException(name,
                    "Name cannot start or end with whitespace");
        }
    }

    /**
     * Validates email format according to business rules.
     * <p>
     * Rules:
     * - Cannot be null or empty
     * - Must match a valid email pattern
     * - Cannot start or end with whitespace
     * 
     * @param email the email to validate
     * @throws ValidationException if email doesn't meet format requirements
     */
    private void validateEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email cannot be null or empty");
        }

        String trimmedEmail = email.trim();

        // Check for leading/trailing whitespace in original email
        if (!email.equals(trimmedEmail)) {
            throw new ValidationException("Email cannot start or end with whitespace");
        }

        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new ValidationException("Email format is invalid");
        }
    }
}