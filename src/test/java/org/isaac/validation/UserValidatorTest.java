package org.isaac.validation;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.isaac.exception.DuplicateEmailException;
import org.isaac.exception.InvalidNameException;
import org.isaac.exception.ValidationException;
import org.isaac.grpc.user.UserProto.CreateUserRequest;
import org.isaac.grpc.user.UserProto.UpdateUserRequest;
import org.isaac.grpc.user.UserProto.User;
import org.isaac.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * Comprehensive unit tests for UserValidator.
 * 
 * These tests verify business validation logic including:
 * - Name format validation
 * - Email format validation
 * - Email uniqueness validation
 * - Name uniqueness validation (business rule)
 * - Error handling in reactive streams
 */
@QuarkusTest
class UserValidatorTest {

    @Inject
    UserValidator userValidator;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clear repository before each test
        userRepository.clear().await().atMost(Duration.ofSeconds(1));
    }

    @Nested
    @DisplayName("Create User Request Validation")
    class CreateUserRequestValidation {

        @Test
        @DisplayName("Should validate valid create request successfully")
        void shouldValidateValidCreateRequest() {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName("John Doe")
                    .setEmail("john.doe@example.com")
                    .build();

            Uni<Void> result = userValidator.validateCreateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitItem(Duration.ofSeconds(1))
                    .assertCompleted();
        }

        @Test
        @DisplayName("Should fail validation for duplicate email")
        void shouldFailValidationForDuplicateEmail() {
            // Create existing user
            User existingUser = User.newBuilder()
                    .setId("existing-id")
                    .setName("Existing User")
                    .setEmail("john.doe@example.com")
                    .setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            userRepository.save(existingUser).await().atMost(Duration.ofSeconds(1));

            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName("John Doe")
                    .setEmail("john.doe@example.com")
                    .build();

            Uni<Void> result = userValidator.validateCreateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(DuplicateEmailException.class,
                            "User with email 'john.doe@example.com' already exists");
        }

        @Test
        @DisplayName("Should fail validation for duplicate name")
        void shouldFailValidationForDuplicateName() {
            // Create existing user
            User existingUser = User.newBuilder()
                    .setId("existing-id")
                    .setName("John Doe")
                    .setEmail("existing@example.com")
                    .setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            userRepository.save(existingUser).await().atMost(Duration.ofSeconds(1));

            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName("John Doe")
                    .setEmail("john.doe@example.com")
                    .build();

            Uni<Void> result = userValidator.validateCreateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(ValidationException.class, "User with name 'John Doe' already exists");
        }

        @Test
        @DisplayName("Should fail validation for invalid name format")
        void shouldFailValidationForInvalidNameFormat() {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName("J") // Too short
                    .setEmail("john.doe@example.com")
                    .build();

            Uni<Void> result = userValidator.validateCreateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(InvalidNameException.class, "Name must be at least 2 characters long");
        }

        @Test
        @DisplayName("Should fail validation for invalid email format")
        void shouldFailValidationForInvalidEmailFormat() {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName("John Doe")
                    .setEmail("invalid-email")
                    .build();

            Uni<Void> result = userValidator.validateCreateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(ValidationException.class, "Email format is invalid");
        }
    }

    @Nested
    @DisplayName("Update User Request Validation")
    class UpdateUserRequestValidation {

        @Test
        @DisplayName("Should validate valid update request successfully")
        void shouldValidateValidUpdateRequest() {
            UpdateUserRequest request = UpdateUserRequest.newBuilder()
                    .setId("user-id")
                    .setName("John Doe")
                    .setEmail("john.doe@example.com")
                    .build();

            Uni<Void> result = userValidator.validateUpdateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitItem(Duration.ofSeconds(1))
                    .assertCompleted();
        }

        @Test
        @DisplayName("Should allow user to keep their own email during update")
        void shouldAllowUserToKeepTheirOwnEmail() {
            // Create existing user
            User existingUser = User.newBuilder()
                    .setId("user-id")
                    .setName("John Doe")
                    .setEmail("john.doe@example.com")
                    .setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            userRepository.save(existingUser).await().atMost(Duration.ofSeconds(1));

            UpdateUserRequest request = UpdateUserRequest.newBuilder()
                    .setId("user-id")
                    .setName("John Updated")
                    .setEmail("john.doe@example.com") // Same email
                    .build();

            Uni<Void> result = userValidator.validateUpdateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitItem(Duration.ofSeconds(1))
                    .assertCompleted();
        }

        @Test
        @DisplayName("Should fail validation for email used by another user")
        void shouldFailValidationForEmailUsedByAnotherUser() {
            // Create existing users
            User existingUser1 = User.newBuilder()
                    .setId("user-1")
                    .setName("User One")
                    .setEmail("user1@example.com")
                    .setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            User existingUser2 = User.newBuilder()
                    .setId("user-2")
                    .setName("User Two")
                    .setEmail("user2@example.com")
                    .setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            userRepository.save(existingUser1).await().atMost(Duration.ofSeconds(1));
            userRepository.save(existingUser2).await().atMost(Duration.ofSeconds(1));

            // Try to update user-2 with user-1's email
            UpdateUserRequest request = UpdateUserRequest.newBuilder()
                    .setId("user-2")
                    .setName("User Two Updated")
                    .setEmail("user1@example.com")
                    .build();

            Uni<Void> result = userValidator.validateUpdateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(DuplicateEmailException.class,
                            "User with email 'user1@example.com' already exists");
        }
    }

    @Nested
    @DisplayName("Email Uniqueness Validation")
    class EmailUniquenessValidation {

        @Test
        @DisplayName("Should pass validation for unique email")
        void shouldPassValidationForUniqueEmail() {
            Uni<Void> result = userValidator.validateEmailUniqueness("unique@example.com", null);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitItem(Duration.ofSeconds(1))
                    .assertCompleted();
        }

        @Test
        @DisplayName("Should fail validation for existing email")
        void shouldFailValidationForExistingEmail() {
            // Create existing user
            User existingUser = User.newBuilder()
                    .setId("existing-id")
                    .setName("Existing User")
                    .setEmail("existing@example.com")
                    .setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            userRepository.save(existingUser).await().atMost(Duration.ofSeconds(1));

            Uni<Void> result = userValidator.validateEmailUniqueness("existing@example.com", null);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(DuplicateEmailException.class,
                            "User with email 'existing@example.com' already exists");
        }

        @Test
        @DisplayName("Should pass validation when excluding specific user ID")
        void shouldPassValidationWhenExcludingSpecificUserId() {
            // Create existing user
            User existingUser = User.newBuilder()
                    .setId("user-id")
                    .setName("Existing User")
                    .setEmail("existing@example.com")
                    .setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            userRepository.save(existingUser).await().atMost(Duration.ofSeconds(1));

            Uni<Void> result = userValidator.validateEmailUniqueness("existing@example.com", "user-id");

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitItem(Duration.ofSeconds(1))
                    .assertCompleted();
        }

        @Test
        @DisplayName("Should fail validation for null email")
        void shouldFailValidationForNullEmail() {
            Uni<Void> result = userValidator.validateEmailUniqueness(null, null);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(ValidationException.class, "Email cannot be null or empty");
        }

        @Test
        @DisplayName("Should fail validation for empty email")
        void shouldFailValidationForEmptyEmail() {
            Uni<Void> result = userValidator.validateEmailUniqueness("", null);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(ValidationException.class, "Email cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Name Format Validation")
    class NameFormatValidation {

        @Test
        @DisplayName("Should accept valid names")
        void shouldAcceptValidNames() {
            String[] validNames = {
                    "John Doe",
                    "Mary Jane",
                    "Jean-Pierre",
                    "O'Connor",
                    "Anna-Maria Smith",
                    "José María"
            };

            for (String name : validNames) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                        .setName(name)
                        .setEmail("test@example.com")
                        .build();

                Uni<Void> result = userValidator.validateCreateRequest(request);

                result.subscribe().withSubscriber(UniAssertSubscriber.create())
                        .awaitItem(Duration.ofSeconds(1))
                        .assertCompleted();
            }
        }

        @Test
        @DisplayName("Should reject names that are too short")
        void shouldRejectNamesThatAreTooShort() {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName("J")
                    .setEmail("test@example.com")
                    .build();

            Uni<Void> result = userValidator.validateCreateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(InvalidNameException.class, "Name must be at least 2 characters long");
        }

        @Test
        @DisplayName("Should reject names that are too long")
        void shouldRejectNamesThatAreTooLong() {
            String longName = "A".repeat(51); // Exceeds MAX_NAME_LENGTH

            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName(longName)
                    .setEmail("test@example.com")
                    .build();

            Uni<Void> result = userValidator.validateCreateRequest(request);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(InvalidNameException.class, "Name cannot exceed 50 characters");
        }

        @Test
        @DisplayName("Should reject names with invalid characters")
        void shouldRejectNamesWithInvalidCharacters() {
            String[] invalidNames = {
                    "John123",
                    "John@Doe",
                    "John#Doe",
                    "John$Doe",
                    "John%Doe"
            };

            for (String name : invalidNames) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                        .setName(name)
                        .setEmail("test@example.com")
                        .build();

                Uni<Void> result = userValidator.validateCreateRequest(request);

                result.subscribe().withSubscriber(UniAssertSubscriber.create())
                        .awaitFailure(Duration.ofSeconds(1))
                        .assertFailedWith(InvalidNameException.class,
                                "Name can only contain letters, spaces, hyphens, and apostrophes");
            }
        }

        @Test
        @DisplayName("Should reject names with leading or trailing whitespace")
        void shouldRejectNamesWithLeadingOrTrailingWhitespace() {
            String[] invalidNames = {
                    " John Doe",
                    "John Doe ",
                    " John Doe ",
                    "\tJohn Doe",
                    "John Doe\n"
            };

            for (String name : invalidNames) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                        .setName(name)
                        .setEmail("test@example.com")
                        .build();

                Uni<Void> result = userValidator.validateCreateRequest(request);

                result.subscribe().withSubscriber(UniAssertSubscriber.create())
                        .awaitFailure(Duration.ofSeconds(1))
                        .assertFailedWith(InvalidNameException.class, "Name cannot start or end with whitespace");
            }
        }
    }

    @Nested
    @DisplayName("Email Format Validation")
    class EmailFormatValidation {

        @Test
        @DisplayName("Should accept valid email formats")
        void shouldAcceptValidEmailFormats() {
            String[] validEmails = {
                    "test@example.com",
                    "user.name@example.com",
                    "user+tag@example.com",
                    "user_name@example.co.uk",
                    "123@example.com",
                    "test@sub.example.com"
            };

            for (String email : validEmails) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                        .setName("John Doe")
                        .setEmail(email)
                        .build();

                Uni<Void> result = userValidator.validateCreateRequest(request);

                result.subscribe().withSubscriber(UniAssertSubscriber.create())
                        .awaitItem(Duration.ofSeconds(1))
                        .assertCompleted();
            }
        }

        @Test
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmailFormats() {
            String[] invalidEmails = {
                    "invalid-email",
                    "@example.com",
                    "test@",
                    "test.example.com",
                    "test@.com",
                    "test@example."
            };

            for (String email : invalidEmails) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                        .setName("John Doe")
                        .setEmail(email)
                        .build();

                Uni<Void> result = userValidator.validateCreateRequest(request);

                result.subscribe().withSubscriber(UniAssertSubscriber.create())
                        .awaitFailure(Duration.ofSeconds(1))
                        .assertFailedWith(ValidationException.class, "Email format is invalid");
            }
        }

        @Test
        @DisplayName("Should reject emails with leading or trailing whitespace")
        void shouldRejectEmailsWithLeadingOrTrailingWhitespace() {
            String[] invalidEmails = {
                    " test@example.com",
                    "test@example.com ",
                    " test@example.com ",
                    "\ttest@example.com",
                    "test@example.com\n"
            };

            for (String email : invalidEmails) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                        .setName("John Doe")
                        .setEmail(email)
                        .build();

                Uni<Void> result = userValidator.validateCreateRequest(request);

                result.subscribe().withSubscriber(UniAssertSubscriber.create())
                        .awaitFailure(Duration.ofSeconds(1))
                        .assertFailedWith(ValidationException.class, "Email cannot start or end with whitespace");
            }
        }
    }

    @Nested
    @DisplayName("Name Uniqueness Validation")
    class NameUniquenessValidation {

        @Test
        @DisplayName("Should pass validation for unique name")
        void shouldPassValidationForUniqueName() {
            Uni<Void> result = userValidator.validateNameUniqueness("Unique Name", null);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitItem(Duration.ofSeconds(1))
                    .assertCompleted();
        }

        @Test
        @DisplayName("Should fail validation for existing name (case insensitive)")
        void shouldFailValidationForExistingNameCaseInsensitive() {
            // Create existing user
            User existingUser = User.newBuilder()
                    .setId("existing-id")
                    .setName("John Doe")
                    .setEmail("existing@example.com")
                    .setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            userRepository.save(existingUser).await().atMost(Duration.ofSeconds(1));

            // Test case insensitive matching
            Uni<Void> result = userValidator.validateNameUniqueness("john doe", null);

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitFailure(Duration.ofSeconds(1))
                    .assertFailedWith(ValidationException.class, "User with name 'john doe' already exists");
        }

        @Test
        @DisplayName("Should pass validation when excluding specific user ID")
        void shouldPassValidationWhenExcludingSpecificUserId() {
            // Create existing user
            User existingUser = User.newBuilder()
                    .setId("user-id")
                    .setName("John Doe")
                    .setEmail("existing@example.com")
                    .setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            userRepository.save(existingUser).await().atMost(Duration.ofSeconds(1));

            Uni<Void> result = userValidator.validateNameUniqueness("John Doe", "user-id");

            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .awaitItem(Duration.ofSeconds(1))
                    .assertCompleted();
        }
    }
}