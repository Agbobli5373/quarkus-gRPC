package org.isaac.dto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UpdateUserDto validation.
 * Tests all validation constraints and edge cases.
 */
@QuarkusTest
class UpdateUserDtoTest {

    @Inject
    Validator validator;

    @Test
    void shouldPassValidationWithValidData() {
        // Given
        UpdateUserDto dto = new UpdateUserDto("Jane Smith", "jane.smith@example.com");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Valid DTO should not have validation violations");
    }

    @Test
    void shouldFailValidationWhenNameIsBlank() {
        // Given
        UpdateUserDto dto = new UpdateUserDto("", "jane.smith@example.com");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

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
        UpdateUserDto dto = new UpdateUserDto(null, "jane.smith@example.com");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateUserDto> violation = violations.iterator().next();
        assertEquals("Name is required", violation.getMessage());
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenNameIsTooShort() {
        // Given
        UpdateUserDto dto = new UpdateUserDto("B", "jane.smith@example.com");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateUserDto> violation = violations.iterator().next();
        assertEquals("Name must be between 2 and 100 characters", violation.getMessage());
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenNameIsTooLong() {
        // Given
        String longName = "B".repeat(101);
        UpdateUserDto dto = new UpdateUserDto(longName, "jane.smith@example.com");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateUserDto> violation = violations.iterator().next();
        assertEquals("Name must be between 2 and 100 characters", violation.getMessage());
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenEmailIsBlank() {
        // Given
        UpdateUserDto dto = new UpdateUserDto("Jane Smith", "");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateUserDto> violation = violations.iterator().next();
        assertEquals("Email is required", violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenEmailIsNull() {
        // Given
        UpdateUserDto dto = new UpdateUserDto("Jane Smith", null);

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateUserDto> violation = violations.iterator().next();
        assertEquals("Email is required", violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenEmailIsInvalid() {
        // Given
        UpdateUserDto dto = new UpdateUserDto("Jane Smith", "not-an-email");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateUserDto> violation = violations.iterator().next();
        assertEquals("Valid email is required", violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void shouldFailValidationWhenEmailIsTooLong() {
        // Given
        String longEmail = "b".repeat(250) + "@example.com"; // Total > 255 characters
        UpdateUserDto dto = new UpdateUserDto("Jane Smith", longEmail);

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

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
        UpdateUserDto dto = new UpdateUserDto("", "not-an-email");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

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
                "test@subdomain.example.org"
        };

        for (String email : validEmails) {
            UpdateUserDto dto = new UpdateUserDto("Jane Smith", email);
            Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(),
                    "Email '" + email + "' should be valid but got violations: " + violations);
        }
    }

    @Test
    void shouldAcceptMinimumValidName() {
        // Given - minimum valid name length
        UpdateUserDto dto = new UpdateUserDto("Jo", "jane@example.com");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Minimum valid name should pass validation");
    }

    @Test
    void shouldAcceptMaximumValidName() {
        // Given - maximum valid name length
        String maxName = "A".repeat(100);
        UpdateUserDto dto = new UpdateUserDto(maxName, "jane@example.com");

        // When
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Maximum valid name should pass validation");
    }
}