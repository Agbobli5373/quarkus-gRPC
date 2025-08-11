package org.isaac.rest.user;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.isaac.dto.CreateUserDto;
import org.isaac.dto.UpdateUserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive integration tests for REST endpoints demonstrating:
 * - REST API testing with RestAssured
 * - JSON serialization/deserialization testing
 * - HTTP status code validation
 * - REST-to-gRPC integration and error propagation
 * - Input validation and error response formats
 * 
 * Learning Objectives:
 * - Understand REST API testing with RestAssured
 * - Learn about JSON serialization testing
 * - Understand HTTP status code validation
 */
@QuarkusTest
@DisplayName("User REST API Integration Tests")
class UserRestIntegrationTest {

        @Nested
        @DisplayName("Valid Request/Response Cycles")
        class ValidRequestResponseTests {

                @Test
                @DisplayName("Should create user with valid data and return 201 with proper JSON structure")
                void shouldCreateUserSuccessfully() {
                        // Given - valid user data
                        CreateUserDto createUserDto = new CreateUserDto("John Doe", "john.doe@example.com");

                        // When - create user via REST
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .contentType(ContentType.JSON)
                                        .body("name", equalTo("John Doe"))
                                        .body("email", equalTo("john.doe@example.com"))
                                        .body("id", notNullValue())
                                        .body("id", matchesPattern("[a-f0-9-]{36}")) // UUID pattern
                                        .body("createdAt", notNullValue())
                                        .body("updatedAt", notNullValue())
                                        .body("createdAt",
                                                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z")) // ISO
                                                                                                                        // timestamp
                                        .body("updatedAt",
                                                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z")); // ISO
                                                                                                                         // timestamp
                }

                @Test
                @DisplayName("Should retrieve user by ID and return 200 with complete user data")
                void shouldGetUserSuccessfully() {
                        // Given - create a user first
                        CreateUserDto createUserDto = new CreateUserDto("Jane Smith", "jane.smith@example.com");

                        String userId = given()
                                        .contentType(ContentType.JSON)
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .extract()
                                        .path("id");

                        // When - retrieve the user
                        given()
                                        .when()
                                        .get("/api/users/" + userId)
                                        .then()
                                        .statusCode(200)
                                        .contentType(ContentType.JSON)
                                        .body("id", equalTo(userId))
                                        .body("name", equalTo("Jane Smith"))
                                        .body("email", equalTo("jane.smith@example.com"))
                                        .body("createdAt", notNullValue())
                                        .body("updatedAt", notNullValue());
                }

                @Test
                @DisplayName("Should update user and return 200 with updated data")
                void shouldUpdateUserSuccessfully() {
                        // Given - create a user first
                        CreateUserDto createUserDto = new CreateUserDto("Update Test User", "update@example.com");

                        String userId = given()
                                        .contentType(ContentType.JSON)
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .extract()
                                        .path("id");

                        // When - update the user
                        UpdateUserDto updateUserDto = new UpdateUserDto("Updated User Name", "updated@example.com");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(updateUserDto)
                                        .when()
                                        .put("/api/users/" + userId)
                                        .then()
                                        .statusCode(200)
                                        .contentType(ContentType.JSON)
                                        .body("id", equalTo(userId))
                                        .body("name", equalTo("Updated User Name"))
                                        .body("email", equalTo("updated@example.com"))
                                        .body("createdAt", notNullValue())
                                        .body("updatedAt", notNullValue());
                }

                @Test
                @DisplayName("Should delete user and return 204 No Content")
                void shouldDeleteUserSuccessfully() {
                        // Given - create a user first
                        CreateUserDto createUserDto = new CreateUserDto("Delete Test User", "delete@example.com");

                        String userId = given()
                                        .contentType(ContentType.JSON)
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .extract()
                                        .path("id");

                        // When - delete the user
                        given()
                                        .when()
                                        .delete("/api/users/" + userId)
                                        .then()
                                        .statusCode(204)
                                        .body(emptyString()); // No content in response body
                }

                @Test
                @DisplayName("Should list all users and return 200 with JSON array")
                void shouldListUsersSuccessfully() {
                        // Given - create multiple users with valid names (no numbers)
                        CreateUserDto user1 = new CreateUserDto("Alice Johnson", "alice@example.com");
                        CreateUserDto user2 = new CreateUserDto("Bob Wilson", "bob@example.com");

                        String userId1 = given()
                                        .contentType(ContentType.JSON)
                                        .body(user1)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .extract()
                                        .path("id");

                        String userId2 = given()
                                        .contentType(ContentType.JSON)
                                        .body(user2)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .extract()
                                        .path("id");

                        // When - list all users
                        given()
                                        .when()
                                        .get("/api/users")
                                        .then()
                                        .statusCode(200)
                                        .contentType(ContentType.JSON)
                                        .body("size()", greaterThanOrEqualTo(2))
                                        .body("find { it.id == '" + userId1 + "' }.name", equalTo("Alice Johnson"))
                                        .body("find { it.id == '" + userId2 + "' }.name", equalTo("Bob Wilson"))
                                        .body("[0].id", notNullValue())
                                        .body("[0].name", notNullValue())
                                        .body("[0].email", notNullValue())
                                        .body("[0].createdAt", notNullValue())
                                        .body("[0].updatedAt", notNullValue());
                }
        }

        @Nested
        @DisplayName("Input Validation and Error Handling")
        class ValidationAndErrorHandlingTests {

                @Test
                @DisplayName("Should return 400 for missing name in create request")
                void shouldRejectCreateUserWithMissingName() {
                        // Given - user data with missing name
                        String invalidJson = "{\"email\":\"test@example.com\"}";

                        // When - attempt to create user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(invalidJson)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(400)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("Validation failed"))
                                        .body("message", containsString("The request contains invalid data"))
                                        .body("details", hasItem("Name is required"))
                                        .body("timestamp", notNullValue());
                }

                @Test
                @DisplayName("Should return 400 for invalid email format in create request")
                void shouldRejectCreateUserWithInvalidEmail() {
                        // Given - user data with invalid email
                        CreateUserDto invalidUser = new CreateUserDto("Test User", "invalid-email");

                        // When - attempt to create user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(invalidUser)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(400)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("Validation failed"))
                                        .body("message", containsString("The request contains invalid data"))
                                        .body("details", hasItem("Valid email is required"))
                                        .body("timestamp", notNullValue());
                }

                @Test
                @DisplayName("Should return 400 for empty name in create request")
                void shouldRejectCreateUserWithEmptyName() {
                        // Given - user data with empty name
                        CreateUserDto invalidUser = new CreateUserDto("", "test@example.com");

                        // When - attempt to create user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(invalidUser)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(400)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("Validation failed"))
                                        .body("message", containsString("The request contains invalid data"))
                                        .body("details", hasItem("Name is required"))
                                        .body("timestamp", notNullValue());
                }

                @Test
                @DisplayName("Should return 400 for name that's too short")
                void shouldRejectCreateUserWithTooShortName() {
                        // Given - user data with name too short
                        CreateUserDto invalidUser = new CreateUserDto("A", "test@example.com");

                        // When - attempt to create user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(invalidUser)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(400)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("Validation failed"))
                                        .body("message", containsString("The request contains invalid data"))
                                        .body("details", hasItem("Name must be between 2 and 100 characters"))
                                        .body("timestamp", notNullValue());
                }

                @Test
                @DisplayName("Should return 400 for missing email in create request")
                void shouldRejectCreateUserWithMissingEmail() {
                        // Given - user data with missing email
                        String invalidJson = "{\"name\":\"Test User\"}";

                        // When - attempt to create user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(invalidJson)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(400)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("Validation failed"))
                                        .body("message", containsString("The request contains invalid data"))
                                        .body("details", hasItem("Email is required"))
                                        .body("timestamp", notNullValue());
                }

                @Test
                @DisplayName("Should return 400 for invalid JSON format")
                void shouldRejectInvalidJsonFormat() {
                        // Given - malformed JSON
                        String malformedJson = "{\"name\":\"Test User\",\"email\":}";

                        // When - attempt to create user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(malformedJson)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(400);
                }

                @Test
                @DisplayName("Should return 415 for missing Content-Type header")
                void shouldRejectRequestWithoutContentType() {
                        // Given - valid user data but no content type
                        CreateUserDto createUserDto = new CreateUserDto("Test User", "test@example.com");

                        // When - attempt to create user without content type
                        given()
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(415); // Unsupported Media Type
                }

                @Test
                @DisplayName("Should return 400 for name with invalid characters (business rule)")
                void shouldRejectCreateUserWithInvalidNameCharacters() {
                        // Given - user data with numbers in name (not allowed by business rules)
                        CreateUserDto invalidUser = new CreateUserDto("User123", "test@example.com");

                        // When - attempt to create user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(invalidUser)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(400)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("Invalid request"))
                                        .body("message", containsString(
                                                        "Name can only contain letters, spaces, hyphens, and apostrophes"))
                                        .body("timestamp", notNullValue());
                }
        }

        @Nested
        @DisplayName("gRPC Error Propagation to HTTP")
        class GrpcErrorPropagationTests {

                @Test
                @DisplayName("Should return 404 when getting non-existent user")
                void shouldHandleUserNotFound() {
                        // When - try to get non-existent user
                        given()
                                        .when()
                                        .get("/api/users/non-existent-id")
                                        .then()
                                        .statusCode(404)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("User not found"))
                                        .body("message", containsString("not found"))
                                        .body("timestamp", notNullValue())
                                        .body("details", nullValue());
                }

                @Test
                @DisplayName("Should return 404 when updating non-existent user")
                void shouldHandleUpdateNonExistentUser() {
                        // Given - valid update data for non-existent user
                        UpdateUserDto updateUserDto = new UpdateUserDto("Updated Name", "updated@example.com");

                        // When - try to update non-existent user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(updateUserDto)
                                        .when()
                                        .put("/api/users/non-existent-id")
                                        .then()
                                        .statusCode(404)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("User not found"))
                                        .body("message", containsString("not found"))
                                        .body("timestamp", notNullValue());
                }

                @Test
                @DisplayName("Should return 404 when deleting non-existent user")
                void shouldHandleDeleteNonExistentUser() {
                        // When - try to delete non-existent user
                        given()
                                        .when()
                                        .delete("/api/users/non-existent-id")
                                        .then()
                                        .statusCode(404)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("User not found"))
                                        .body("message", containsString("not found"))
                                        .body("timestamp", notNullValue());
                }

                @Test
                @DisplayName("Should return 409 when creating user with duplicate email")
                void shouldHandleDuplicateEmailError() {
                        // Given - create a user first
                        CreateUserDto createUserDto = new CreateUserDto("First User", "duplicate@example.com");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201);

                        // When - try to create another user with same email
                        CreateUserDto duplicateUser = new CreateUserDto("Second User", "duplicate@example.com");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(duplicateUser)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(409)
                                        .contentType(ContentType.JSON)
                                        .body("error", equalTo("User already exists"))
                                        .body("message", containsString("already exists"))
                                        .body("timestamp", notNullValue());
                }
        }

        @Nested
        @DisplayName("JSON Format Validation")
        class JsonFormatValidationTests {

                @Test
                @DisplayName("Should properly serialize timestamps in ISO format")
                void shouldSerializeTimestampsCorrectly() {
                        // Given - create a user
                        CreateUserDto createUserDto = new CreateUserDto("Timestamp Test", "timestamp@example.com");

                        // When - create and retrieve user
                        String userId = given()
                                        .contentType(ContentType.JSON)
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .extract()
                                        .path("id");

                        // Then - verify timestamp format in response
                        given()
                                        .when()
                                        .get("/api/users/" + userId)
                                        .then()
                                        .statusCode(200)
                                        .body("createdAt",
                                                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z"))
                                        .body("updatedAt",
                                                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z"));
                }

                @Test
                @DisplayName("Should handle special characters in user data")
                void shouldHandleSpecialCharactersInUserData() {
                        // Given - user data with special characters allowed by business rules (hyphens
                        // and apostrophes)
                        CreateUserDto createUserDto = new CreateUserDto("Mary O'Connor-Smith",
                                        "mary.oconnor@example.com");

                        // When - create user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .contentType(ContentType.JSON)
                                        .body("name", equalTo("Mary O'Connor-Smith"))
                                        .body("email", equalTo("mary.oconnor@example.com"));
                }

                @Test
                @DisplayName("Should handle Unicode characters in user data")
                void shouldHandleUnicodeCharactersInUserData() {
                        // Given - user data with Unicode letters (business rules allow Unicode letters)
                        CreateUserDto createUserDto = new CreateUserDto("François Müller",
                                        "francois.muller@example.com");

                        // When - create user
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .contentType(ContentType.JSON)
                                        .body("name", equalTo("François Müller"))
                                        .body("email", equalTo("francois.muller@example.com"));
                }

                @Test
                @DisplayName("Should return consistent error response format")
                void shouldReturnConsistentErrorResponseFormat() {
                        // When - trigger various error scenarios and verify consistent format
                        given()
                                        .when()
                                        .get("/api/users/non-existent")
                                        .then()
                                        .statusCode(404)
                                        .body("error", notNullValue())
                                        .body("message", notNullValue())
                                        .body("timestamp", notNullValue())
                                        .body("timestamp",
                                                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z"));
                }
        }

        @Nested
        @DisplayName("Complete User Lifecycle Integration")
        class CompleteLifecycleTests {

                @Test
                @DisplayName("Should handle complete CRUD lifecycle successfully")
                void shouldHandleCompleteUserLifecycle() {
                        // Create
                        CreateUserDto createUserDto = new CreateUserDto("Lifecycle Test User", "lifecycle@example.com");

                        String userId = given()
                                        .contentType(ContentType.JSON)
                                        .body(createUserDto)
                                        .when()
                                        .post("/api/users")
                                        .then()
                                        .statusCode(201)
                                        .body("name", equalTo("Lifecycle Test User"))
                                        .body("email", equalTo("lifecycle@example.com"))
                                        .extract()
                                        .path("id");

                        // Read
                        given()
                                        .when()
                                        .get("/api/users/" + userId)
                                        .then()
                                        .statusCode(200)
                                        .body("id", equalTo(userId))
                                        .body("name", equalTo("Lifecycle Test User"))
                                        .body("email", equalTo("lifecycle@example.com"));

                        // Update
                        UpdateUserDto updateUserDto = new UpdateUserDto("Updated Lifecycle User",
                                        "updated.lifecycle@example.com");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(updateUserDto)
                                        .when()
                                        .put("/api/users/" + userId)
                                        .then()
                                        .statusCode(200)
                                        .body("id", equalTo(userId))
                                        .body("name", equalTo("Updated Lifecycle User"))
                                        .body("email", equalTo("updated.lifecycle@example.com"));

                        // Verify in list
                        given()
                                        .when()
                                        .get("/api/users")
                                        .then()
                                        .statusCode(200)
                                        .body("find { it.id == '" + userId + "' }.name",
                                                        equalTo("Updated Lifecycle User"));

                        // Delete
                        given()
                                        .when()
                                        .delete("/api/users/" + userId)
                                        .then()
                                        .statusCode(204);

                        // Verify deletion
                        given()
                                        .when()
                                        .get("/api/users/" + userId)
                                        .then()
                                        .statusCode(404);
                }
        }
}