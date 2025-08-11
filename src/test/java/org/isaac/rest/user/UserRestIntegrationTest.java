package org.isaac.rest.user;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.isaac.dto.CreateUserDto;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration test that verifies the complete REST to gRPC integration works correctly.
 * This test demonstrates the full flow from REST endpoint to gRPC service.
 */
@QuarkusTest
class UserRestIntegrationTest{

    @Test
    void shouldCreateAndRetrieveUserSuccessfully() {
        // Given - valid user data
        CreateUserDto createUserDto = new CreateUserDto("Integration Test User", "integration@example.com");

        // When - create user via REST
        String userId = given()
                .contentType(ContentType.JSON)
                .body(createUserDto)
        .when()
                .post("/api/users")
        .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("name", equalTo("Integration Test User"))
                .body("email", equalTo("integration@example.com"))
                .body("id", notNullValue())
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue())
                .extract()
                .path("id");

        // Then - retrieve the created user
        given()
        .when()
                .get("/api/users/" + userId)
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(userId))
                .body("name", equalTo("Integration Test User"))
                .body("email", equalTo("integration@example.com"));

        // And - verify user appears in list
        given()
        .when()
                .get("/api/users")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", greaterThan(0))
                .body("find { it.id == '" + userId + "' }.name", equalTo("Integration Test User"));
    }

    @Test
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
        given()
                .contentType(ContentType.JSON)
                .body(new org.isaac.dto.UpdateUserDto("Updated User Name", "updated@example.com"))
        .when()
                .put("/api/users/" + userId)
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(userId))
                .body("name", equalTo("Updated User Name"))
                .body("email", equalTo("updated@example.com"));
    }

    @Test
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
                .statusCode(204);

        // Then - verify user is not found
        given()
        .when()
                .get("/api/users/" + userId)
        .then()
                .statusCode(404)
                .body("error", equalTo("User not found"));
    }

    @Test
    void shouldHandleNotFoundGracefully() {
        // When - try to get non-existent user
        given()
        .when()
                .get("/api/users/non-existent-id")
        .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", equalTo("User not found"))
                .body("message", containsString("not found"))
                .body("timestamp", notNullValue());
    }
}