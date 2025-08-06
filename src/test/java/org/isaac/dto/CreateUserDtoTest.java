package org.isaac.dto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CreateUserDto validation.
 * Tests all validation constraints and edge cases.
 */
@QuarkusTest
class CreateUserDtoTest {

    @Inject
    Validator validator;

    @Test
    void shouldPassValidationWithValidData() {
        // Given
        CreateUserDto dto = new CreateUserDto("John Doe", "john.doe@example.com");

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Valid DTO should not have validation violations");
    }

    @Test
    void shouldFailValidationWhenNameIsBlank() {
        // Given
        CreateUserDto dto = new CreateUserDto("", "john.doe@example.com");

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(2, violations.size()); // Both @NotBlank and @Size violations

        // Check that we have violations for the name field
        boolean hasNotBlankViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name") &&
                        v.getMessage().equals("Name is required"));
        boolean hasSizeViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name") &&
                        v.getMessage().equals("Name must be between 2 and 100 characters"));

        assertTrue(hasNotBlankViolation, "Should have @NotBlank violation");
        assertTrue(hasSizeViolation, "Should have @Size violation");
    }

    @Test
    void shouldFailValidationWhenNameIsNull() {
        // Given
        CreateUserDto dto = new CreateUserDto(null, "john.doe@example.com");

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<CreateUserDto> violation = violations.iterator().next();
        assertEquals("Name is required", violation.getMessage());
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenNameIsTooShort() {
        // Given
        CreateUserDto dto = new CreateUserDto("A", "john.doe@example.com");

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<CreateUserDto> violation = violations.iterator().next();
        assertEquals("Name must be between 2 and 100 characters", violation.getMessage());
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenNameIsTooLong() {
        // Given
        String longName = "A".repeat(101);
        CreateUserDto dto = new CreateUserDto(longName, "john.doe@example.com");

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<CreateUserDto> violation = violations.iterator().next();
        assertEquals("Name must be between 2 and 100 characters", violation.getMessage());
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenEmailIsBlank() {
        // Given
        CreateUserDto dto = new CreateUserDto("John Doe", "");

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<CreateUserDto> violation = violations.iterator().next();
        assertEquals("Email is required", violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenEmailIsNull() {
        // Given
        CreateUserDto dto = new CreateUserDto("John Doe", null);

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<CreateUserDto> violation = violations.iterator().next();
        assertEquals("Email is required", violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenEmailIsInvalid() {
        // Given
        CreateUserDto dto = new CreateUserDto("John Doe", "invalid-email");

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<CreateUserDto> violation = violations.iterator().next();
        assertEquals("Valid email is required", violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenEmailIsTooLong() {
        // Given
        String longEmail = "a".repeat(250) + "@example.com"; // Total > 255 characters
        CreateUserDto dto = new CreateUserDto("John Doe", longEmail);

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(2, violations.size()); // Both @Email and @Size violations for invalid long email

        // Check that we have violations for the email field
        boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email") &&
                        v.getMessage().equals("Valid email is required"));
        boolean hasSizeViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email") &&
                        v.getMessage().equals("Email must not exceed 255 characters"));

        assertTrue(hasEmailViolation, "Should have @Email violation");
        assertTrue(hasSizeViolation, "Should have @Size violation");
    }

    @Test
    void shouldFailValidationWithMultipleErrors() {
        // Given
        CreateUserDto dto = new CreateUserDto("", "invalid-email");

        // When
        Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(3, violations.size()); // 2 name violations (@NotBlank + @Size) + 1 email violation (@Email)

        // Check that both name and email violations are present
        boolean hasNameNotBlankViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name") &&
                        v.getMessage().equals("Name is required"));
        boolean hasNameSizeViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name") &&
                        v.getMessage().equals("Name must be between 2 and 100 characters"));
        boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email") &&
                        v.getMessage().equals("Valid email is required"));

        assertTrue(hasNameNotBlankViolation, "Should have name @NotBlank violation");
        assertTrue(hasNameSizeViolation, "Should have name @Size violation");
        assertTrue(hasEmailViolation, "Should have email validation violation");
    }

    @Test
    void shouldAcceptValidEmailFormats() {
        // Test various valid email formats
        String[] validEmails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.com",
                "user123@example-domain.com",
                "a@b.co"
        };

        for (String email : validEmails) {
            CreateUserDto dto = new CreateUserDto("John Doe", email);
            Set<ConstraintViolation<CreateUserDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(),
                    "Email '" + email + "' should be valid but got violations: " + violations);
        }
    }
}