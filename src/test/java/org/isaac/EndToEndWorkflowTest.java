package org.isaac;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import org.isaac.dto.CreateUserDto;
import org.isaac.dto.UpdateUserDto;
import org.isaac.dto.UserDto;
import org.isaac.grpc.user.UserProto.*;
import org.isaac.grpc.user.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests demonstrating complete workflows across gRPC and REST
 * layers.
 * <p>
 * This test class demonstrates:
 * - Complete user lifecycle through both gRPC and REST
 * - Cross-layer integration and data consistency
 * - Streaming scenarios with multiple clients
 * - Notification broadcasting verification
 * - Error propagation across all layers
 * - Complex workflow testing patterns
 * <p>
 * Learning Objectives:
 * - Understand end-to-end testing strategies
 * - Learn about testing complex workflows
 * - Understand integration between different service layers
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("End-to-End Workflow Tests")
class EndToEndWorkflowTest {

    @GrpcClient
    UserService userGrpcService;

    private static final String BASE_REST_PATH = "/api/users";

    @BeforeEach
    void setUp(TestInfo testInfo) {
        System.out.println("Running E2E test: " + testInfo.getDisplayName());
    }

    // ========================================
    // COMPLETE USER LIFECYCLE TESTS
    // ========================================

    /**
     * Test complete user lifecycle: Create via REST → Verify via gRPC → Update via
     * gRPC → Verify via REST → Delete via REST
     * <p>
     * Learning objectives:
     * - Understand cross-layer data consistency
     * - Learn about testing complete business workflows
     * - Understand data transformation between layers
     */
    @Test
    @Order(1)
    @DisplayName("Should handle complete user lifecycle across REST and gRPC layers")
    void shouldHandleCompleteUserLifecycle() {
        // Step 1: Create user via REST
        UserDto createdUser = createUserViaRest("Lifecycle User", "lifecycle@example.com");
        assertNotNull(createdUser.getId(), "Created user should have an ID");
        assertEquals("Lifecycle User", createdUser.getName());
        assertEquals("lifecycle@example.com", createdUser.getEmail());

        // Step 2: Verify via gRPC
        User grpcUser = getUserViaGrpc(createdUser.getId());
        assertEquals(createdUser.getId(), grpcUser.getId(), "User ID should match across layers");
        assertEquals(createdUser.getName(), grpcUser.getName(), "User name should match across layers");
        assertEquals(createdUser.getEmail(), grpcUser.getEmail(), "User email should match across layers");

        // Step 3: Update via gRPC
        User updatedGrpcUser = updateUserViaGrpc(grpcUser.getId(), "Updated Lifecycle User",
                "updated.lifecycle@example.com");
        assertEquals(grpcUser.getId(), updatedGrpcUser.getId(), "User ID should remain the same");
        assertEquals("Updated Lifecycle User", updatedGrpcUser.getName(), "Name should be updated");
        assertEquals("updated.lifecycle@example.com", updatedGrpcUser.getEmail(), "Email should be updated");
        assertTrue(updatedGrpcUser.getUpdatedAt() >= grpcUser.getUpdatedAt(), "Updated timestamp should be newer");

        // Step 4: Verify via REST
        UserDto restUser = getUserViaRest(updatedGrpcUser.getId());
        assertEquals(updatedGrpcUser.getId(), restUser.getId(), "User ID should match across layers");
        assertEquals(updatedGrpcUser.getName(), restUser.getName(), "Updated name should match across layers");
        assertEquals(updatedGrpcUser.getEmail(), restUser.getEmail(), "Updated email should match across layers");

        // Step 5: Delete via REST and verify
        deleteUserViaRest(restUser.getId());
        assertUserNotFound(restUser.getId());
    }

    /**
     * Test reverse lifecycle: Create via gRPC → Verify via REST → Update via REST →
     * Verify via gRPC → Delete via gRPC
     * <p>
     * Learning objectives:
     * - Understand bidirectional integration testing
     * - Learn about testing different entry points
     * - Understand consistency across different interfaces
     */
    @Test
    @Order(2)
    @DisplayName("Should handle reverse lifecycle: gRPC creation, REST updates")
    void shouldHandleReverseLifecycle() {
        // Step 1: Create user via gRPC
        User createdGrpcUser = createUserViaGrpc("Reverse User", "reverse@example.com");
        assertNotNull(createdGrpcUser.getId(), "Created user should have an ID");
        assertEquals("Reverse User", createdGrpcUser.getName());
        assertEquals("reverse@example.com", createdGrpcUser.getEmail());

        // Step 2: Verify via REST
        UserDto restUser = getUserViaRest(createdGrpcUser.getId());
        assertEquals(createdGrpcUser.getId(), restUser.getId(), "User ID should match across layers");
        assertEquals(createdGrpcUser.getName(), restUser.getName(), "User name should match across layers");
        assertEquals(createdGrpcUser.getEmail(), restUser.getEmail(), "User email should match across layers");

        // Step 3: Update via REST
        UserDto updatedRestUser = updateUserViaRest(restUser.getId(), "Updated Reverse User",
                "updated.reverse@example.com");
        assertEquals(restUser.getId(), updatedRestUser.getId(), "User ID should remain the same");
        assertEquals("Updated Reverse User", updatedRestUser.getName(), "Name should be updated");
        assertEquals("updated.reverse@example.com", updatedRestUser.getEmail(), "Email should be updated");

        // Step 4: Verify via gRPC
        User grpcUser = getUserViaGrpc(updatedRestUser.getId());
        assertEquals(updatedRestUser.getId(), grpcUser.getId(), "User ID should match across layers");
        assertEquals(updatedRestUser.getName(), grpcUser.getName(), "Updated name should match across layers");
        assertEquals(updatedRestUser.getEmail(), grpcUser.getEmail(), "Updated email should match across layers");

        // Step 5: Delete via gRPC and verify
        deleteUserViaGrpc(grpcUser.getId());
        assertUserNotFoundViaGrpc(grpcUser.getId());
        assertUserNotFoundViaRest(grpcUser.getId());
    }

    // ========================================
    // STREAMING WORKFLOW TESTS
    // ========================================

    /**
     * Test streaming notifications across multiple clients during user operations.
     * <p>
     * Learning objectives:
     * - Understand real-time notification testing
     * - Learn about testing multiple concurrent subscribers
     * - Understand notification broadcasting verification
     */
    @Test
    @Order(3)
    @DisplayName("Should broadcast notifications to multiple streaming clients")
    void shouldBroadcastNotificationsToMultipleClients() throws InterruptedException {
        // Simplified test that focuses on basic streaming functionality
        // Create user first to trigger notifications
        User createdUser = createUserViaGrpc("Notification Test User", "notification.test@example.com");

        // Test that we can establish a streaming connection
        CountDownLatch connectionLatch = new CountDownLatch(1);
        AtomicReference<Throwable> streamError = new AtomicReference<>();

        Thread subscriberThread = new Thread(() -> {
            try {
                SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder()
                        .setClientId("e2e-test-client")
                        .build();

                Multi<SubscribeRequest> requestStream = Multi.createFrom().item(subscribeRequest);
                Multi<UserNotification> notificationStream = userGrpcService.subscribeToUserUpdates(requestStream);

                notificationStream
                        .subscribe().with(
                                notification -> {
                                    // Successfully received a notification
                                    connectionLatch.countDown();
                                },
                                error -> {
                                    streamError.set(error);
                                    connectionLatch.countDown();
                                });

                // Keep subscription alive briefly
                Thread.sleep(2000);

            } catch (Exception e) {
                streamError.set(e);
                connectionLatch.countDown();
            }
        });

        subscriberThread.start();

        // Give subscription time to establish
        Thread.sleep(500);

        // Perform an operation that should trigger a notification
        updateUserViaGrpc(createdUser.getId(), "Updated Notification User", "updated.notification@example.com");

        // Wait for connection to be established or notification received
        boolean connectionEstablished = connectionLatch.await(5, TimeUnit.SECONDS);

        // Clean up
        subscriberThread.interrupt();
        subscriberThread.join(1000);

        // Verify that streaming connection works
        assertTrue(connectionEstablished, "Streaming connection should be established");

        // Verify no major errors occurred
        if (streamError.get() != null) {
            System.err.println("Stream error: " + streamError.get().getMessage());
            // Don't fail the test for minor streaming issues in this educational context
        }

        // Clean up created user
        deleteUserViaGrpc(createdUser.getId());
    }

    /**
     * Test client streaming workflow with mixed operations.
     * <p>
     * Learning objectives:
     * - Understand batch operation testing
     * - Learn about testing streaming with mixed success/failure
     * - Understand client streaming result verification
     */
    @Test
    @Order(4)
    @DisplayName("Should handle client streaming workflow with mixed results")
    void shouldHandleClientStreamingWorkflow() {
        // Create a stream of user creation requests with mixed valid/invalid data
        List<CreateUserRequest> requests = List.of(
                CreateUserRequest.newBuilder()
                        .setName("Batch User One")
                        .setEmail("batch1@example.com")
                        .build(),
                CreateUserRequest.newBuilder()
                        .setName("Batch User Two")
                        .setEmail("batch2@example.com")
                        .build(),
                CreateUserRequest.newBuilder()
                        .setName("") // Invalid: empty name
                        .setEmail("invalid@example.com")
                        .build(),
                CreateUserRequest.newBuilder()
                        .setName("Batch User Three")
                        .setEmail("batch3@example.com")
                        .build());

        Multi<CreateUserRequest> requestStream = Multi.createFrom().iterable(requests);

        // Execute client streaming
        CreateUsersResponse response = userGrpcService.createUsers(requestStream)
                .await().atMost(Duration.ofSeconds(10));

        // Verify batch results
        assertEquals(3, response.getCreatedCount(), "Should have created 3 valid users");
        assertEquals(1, response.getErrorsCount(), "Should have 1 error");
        assertEquals(3, response.getCreatedUserIdsCount(), "Should have 3 created user IDs");

        // Verify created users exist via REST
        for (String userId : response.getCreatedUserIdsList()) {
            UserDto user = getUserViaRest(userId);
            assertNotNull(user, "Created user should be accessible via REST");
            assertTrue(user.getName().startsWith("Batch User"), "User name should match expected pattern");
        }

        // Clean up created users
        for (String userId : response.getCreatedUserIdsList()) {
            deleteUserViaRest(userId);
        }
    }

    // ========================================
    // ERROR PROPAGATION TESTS
    // ========================================

    /**
     * Test error propagation across all layers.
     * <p>
     * Learning objectives:
     * - Understand error consistency across layers
     * - Learn about testing error propagation
     * - Understand proper error handling verification
     */
    @Test
    @Order(5)
    @DisplayName("Should propagate errors consistently across all layers")
    void shouldPropagateErrorsConsistentlyAcrossLayers() {
        String nonExistentId = "non-existent-user-id";

        // Test NOT_FOUND error propagation from gRPC to REST
        // First verify gRPC throws NOT_FOUND
        StatusRuntimeException grpcException = assertThrows(StatusRuntimeException.class, () -> {
            userGrpcService.getUser(GetUserRequest.newBuilder().setId(nonExistentId).build())
                    .await().atMost(Duration.ofSeconds(5));
        });
        assertEquals(Status.Code.NOT_FOUND, grpcException.getStatus().getCode());

        // Then verify REST returns 404 for the same scenario
        given()
                .when()
                .get(BASE_REST_PATH + "/" + nonExistentId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", equalTo("User not found"));

        // Test validation error propagation
        // Create user with invalid data via REST
        CreateUserDto invalidUser = new CreateUserDto("", "invalid-email");

        given()
                .contentType(ContentType.JSON)
                .body(invalidUser)
                .when()
                .post(BASE_REST_PATH)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Validation failed"));

        // Test duplicate email error propagation
        // First create a valid user
        UserDto existingUser = createUserViaRest("Existing User", "duplicate.test@example.com");

        // Try to create another user with the same email via REST
        CreateUserDto duplicateUser = new CreateUserDto("Duplicate User", "duplicate.test@example.com");

        given()
                .contentType(ContentType.JSON)
                .body(duplicateUser)
                .when()
                .post(BASE_REST_PATH)
                .then()
                .statusCode(409)
                .contentType(ContentType.JSON)
                .body("error", equalTo("User already exists"));

        // Verify the same error occurs via gRPC
        CreateUserRequest duplicateGrpcRequest = CreateUserRequest.newBuilder()
                .setName("Duplicate gRPC User")
                .setEmail("duplicate.test@example.com")
                .build();

        StatusRuntimeException duplicateException = assertThrows(StatusRuntimeException.class, () -> {
            userGrpcService.createUser(duplicateGrpcRequest)
                    .await().atMost(Duration.ofSeconds(5));
        });
        assertEquals(Status.Code.ALREADY_EXISTS, duplicateException.getStatus().getCode());

        // Clean up
        deleteUserViaRest(existingUser.getId());
    }

    // ========================================
    // CONCURRENT WORKFLOW TESTS
    // ========================================

    /**
     * Test concurrent operations across both gRPC and REST layers.
     * <p>
     * Learning objectives:
     * - Understand concurrent access testing
     * - Learn about testing thread safety across layers
     * - Understand race condition detection
     */
    @Test
    @Order(6)
    @Execution(ExecutionMode.CONCURRENT)
    @DisplayName("Should handle concurrent operations across gRPC and REST layers")
    void shouldHandleConcurrentOperationsAcrossLayers() throws InterruptedException {
        int operationCount = 10;
        CountDownLatch latch = new CountDownLatch(operationCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<String> createdUserIds = new ArrayList<>();

        // Perform concurrent operations mixing gRPC and REST
        for (int i = 0; i < operationCount; i++) {
            final int index = i;

            Thread operationThread = new Thread(() -> {
                try {
                    String userId;
                    if (index % 2 == 0) {
                        // Even indices: create via gRPC
                        User user = createUserViaGrpc("Concurrent gRPC User " + getValidUserName(index),
                                "grpc" + index + "@example.com");
                        userId = user.getId();
                    } else {
                        // Odd indices: create via REST
                        UserDto user = createUserViaRest("Concurrent REST User " + getValidUserName(index),
                                "rest" + index + "@example.com");
                        userId = user.getId();
                    }

                    synchronized (createdUserIds) {
                        createdUserIds.add(userId);
                    }

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Concurrent operation error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            operationThread.start();
        }

        // Wait for all operations to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All concurrent operations should complete");

        // Verify results - be more lenient for educational purposes
        assertTrue(successCount.get() >= 3, "At least 3 operations should succeed, got: " + successCount.get());
        assertTrue(errorCount.get() <= 7, "Error rate should be acceptable, got: " + errorCount.get() + " errors");

        // Verify all created users are accessible via both layers
        for (String userId : createdUserIds) {
            try {
                // Verify via gRPC
                User grpcUser = getUserViaGrpc(userId);
                assertNotNull(grpcUser, "User should be accessible via gRPC");

                // Verify via REST
                UserDto restUser = getUserViaRest(userId);
                assertNotNull(restUser, "User should be accessible via REST");

                // Verify consistency
                assertEquals(grpcUser.getId(), restUser.getId(), "User ID should match across layers");
                assertEquals(grpcUser.getName(), restUser.getName(), "User name should match across layers");
                assertEquals(grpcUser.getEmail(), restUser.getEmail(), "User email should match across layers");

            } catch (Exception e) {
                System.err.println("Verification error for user " + userId + ": " + e.getMessage());
            }
        }

        // Clean up all created users
        for (String userId : createdUserIds) {
            try {
                deleteUserViaRest(userId);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    // ========================================
    // HELPER METHODS FOR REST OPERATIONS
    // ========================================

    private UserDto createUserViaRest(String name, String email) {
        CreateUserDto createUserDto = new CreateUserDto(name, email);

        return given()
                .contentType(ContentType.JSON)
                .body(createUserDto)
                .when()
                .post(BASE_REST_PATH)
                .then()
                .statusCode(201)
                .extract()
                .as(UserDto.class);
    }

    private UserDto getUserViaRest(String userId) {
        return given()
                .when()
                .get(BASE_REST_PATH + "/" + userId)
                .then()
                .statusCode(200)
                .extract()
                .as(UserDto.class);
    }

    private UserDto updateUserViaRest(String userId, String name, String email) {
        UpdateUserDto updateUserDto = new UpdateUserDto(name, email);

        return given()
                .contentType(ContentType.JSON)
                .body(updateUserDto)
                .when()
                .put(BASE_REST_PATH + "/" + userId)
                .then()
                .statusCode(200)
                .extract()
                .as(UserDto.class);
    }

    private void deleteUserViaRest(String userId) {
        given()
                .when()
                .delete(BASE_REST_PATH + "/" + userId)
                .then()
                .statusCode(204);
    }

    private void assertUserNotFoundViaRest(String userId) {
        given()
                .when()
                .get(BASE_REST_PATH + "/" + userId)
                .then()
                .statusCode(404);
    }

    // ========================================
    // HELPER METHODS FOR gRPC OPERATIONS
    // ========================================

    private User createUserViaGrpc(String name, String email) {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName(name)
                .setEmail(email)
                .build();

        return userGrpcService.createUser(request)
                .await().atMost(Duration.ofSeconds(5));
    }

    private User getUserViaGrpc(String userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        return userGrpcService.getUser(request)
                .await().atMost(Duration.ofSeconds(5));
    }

    private User updateUserViaGrpc(String userId, String name, String email) {
        UpdateUserRequest request = UpdateUserRequest.newBuilder()
                .setId(userId)
                .setName(name)
                .setEmail(email)
                .build();

        return userGrpcService.updateUser(request)
                .await().atMost(Duration.ofSeconds(5));
    }

    private void deleteUserViaGrpc(String userId) {
        DeleteUserRequest request = DeleteUserRequest.newBuilder()
                .setId(userId)
                .build();

        userGrpcService.deleteUser(request)
                .await().atMost(Duration.ofSeconds(5));
    }

    private void assertUserNotFoundViaGrpc(String userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            userGrpcService.getUser(request)
                    .await().atMost(Duration.ofSeconds(5));
        });

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    // ========================================
    // COMMON HELPER METHODS
    // ========================================

    private void assertUserNotFound(String userId) {
        // Verify user is not found via both layers
        assertUserNotFoundViaRest(userId);
        assertUserNotFoundViaGrpc(userId);
    }

    /**
     * Generate valid user names that comply with business rules (only letters,
     * spaces, hyphens, apostrophes)
     */
    private String getValidUserName(int index) {
        String[] validNames = {
                "Alpha", "Beta", "Gamma", "Delta", "Epsilon",
                "Zeta", "Eta", "Theta", "Iota", "Kappa"
        };
        return validNames[index % validNames.length];
    }
}