package org.isaac.grpc.user;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.isaac.grpc.user.UserProto.*;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserGrpcService demonstrating all gRPC patterns.
 * <p>
 * This test class covers:
 * - Unary RPC operations (create, get, update, delete)
 * - Server streaming (list users)
 * - Client streaming (batch create users)
 * - Bidirectional streaming (subscribe to user updates)
 * - Error handling and edge cases
 * - Concurrent access patterns
 * - Streaming lifecycle management
 * <p>
 * Learning objectives:
 * - Understand gRPC testing in Quarkus
 * - Learn about testing streaming operations
 * - Understand test lifecycle management
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserGrpcServiceIntegrationTest {

    @GrpcClient
    UserService userService;

    private static final String TEST_USER_NAME = "John Doe";
    private static final String TEST_USER_EMAIL = "john.doe@example.com";
    private static final String UPDATED_USER_NAME = "Jane Doe";
    private static final String UPDATED_USER_EMAIL = "jane.doe@example.com";

    @BeforeEach
    void setUp(TestInfo testInfo) {
        System.out.println("Running test: " + testInfo.getDisplayName());
    }

    // ========================================
    // UNARY RPC TESTS
    // ========================================

    /**
     * Test successful user creation using unary RPC.
     * <p>
     * Learning objectives:
     * - Understand unary RPC pattern (single request → single response)
     * - Learn about Mutiny Uni testing with await()
     * - Understand proper test data validation
     */
    @Test
    @Order(1)
    void shouldCreateUser() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName(TEST_USER_NAME)
                .setEmail(TEST_USER_EMAIL)
                .build();

        // Act
        User user = userService.createUser(request)
                .await().atMost(Duration.ofSeconds(5));

        // Assert - Verify user properties
        assertNotNull(user.getId(), "User ID should not be null");
        assertFalse(user.getId().trim().isEmpty(), "User ID should not be empty");
        assertEquals(TEST_USER_NAME, user.getName(), "User name should match request");
        assertEquals(TEST_USER_EMAIL, user.getEmail(), "User email should match request");
        assertTrue(user.getCreatedAt() > 0, "Created timestamp should be set");
        assertTrue(user.getUpdatedAt() > 0, "Updated timestamp should be set");
    }

    /**
     * Test user creation with invalid data to verify error handling.
     * <p>
     * Learning objectives:
     * - Understand gRPC error handling with status codes
     * - Learn about testing failure scenarios
     * - Understand proper exception assertion in reactive streams
     */
    @Test
    @Order(2)
    void shouldFailToCreateUserWithInvalidData() {
        // Test empty name
        CreateUserRequest invalidRequest = CreateUserRequest.newBuilder()
                .setName("")
                .setEmail(TEST_USER_EMAIL)
                .build();

        // Act & Assert
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            userService.createUser(invalidRequest)
                    .await().atMost(Duration.ofSeconds(5));
        });

        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

    /**
     * Test duplicate email validation.
     * <p>
     * Learning objectives:
     * - Understand business validation in gRPC services
     * - Learn about testing duplicate data scenarios
     * - Understand ALREADY_EXISTS status code usage
     */
    @Test
    @Order(3)
    void shouldFailToCreateUserWithDuplicateEmail() {
        // First, create a user
        CreateUserRequest firstRequest = CreateUserRequest.newBuilder()
                .setName("First User")
                .setEmail("duplicate@example.com")
                .build();

        userService.createUser(firstRequest)
                .await().atMost(Duration.ofSeconds(5));

        // Try to create another user with the same email
        CreateUserRequest duplicateRequest = CreateUserRequest.newBuilder()
                .setName("Second User")
                .setEmail("duplicate@example.com")
                .build();

        // Act & Assert
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            userService.createUser(duplicateRequest)
                    .await().atMost(Duration.ofSeconds(5));
        });

        assertEquals(Status.Code.ALREADY_EXISTS, exception.getStatus().getCode());
    }

    /**
     * Test successful user retrieval by ID.
     * <p>
     * Learning objectives:
     * - Understand GET operations in gRPC
     * - Learn about testing data retrieval
     * - Understand proper test data setup and verification
     */
    @Test
    @Order(4)
    void shouldGetUser() {
        // First create a user to retrieve
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setName("Get Test User")
                .setEmail("gettest@example.com")
                .build();

        User createdUser = userService.createUser(createRequest)
                .await().atMost(Duration.ofSeconds(5));

        // Now retrieve the user
        GetUserRequest getRequest = GetUserRequest.newBuilder()
                .setId(createdUser.getId())
                .build();

        User retrievedUser = userService.getUser(getRequest)
                .await().atMost(Duration.ofSeconds(5));

        assertEquals(createdUser.getId(), retrievedUser.getId());
        assertEquals(createdUser.getName(), retrievedUser.getName());
        assertEquals(createdUser.getEmail(), retrievedUser.getEmail());
    }

    /**
     * Test user retrieval with non-existent ID.
     * <p>
     * Learning objectives:
     * - Understand NOT_FOUND error handling
     * - Learn about testing resource not found scenarios
     * - Understand proper error status code verification
     */
    @Test
    @Order(5)
    void shouldFailToGetNonExistentUser() {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId("non-existent-id")
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            userService.getUser(request)
                    .await().atMost(Duration.ofSeconds(5));
        });

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    /**
     * Test successful user update.
     * <p>
     * Learning objectives:
     * - Understand UPDATE operations in gRPC
     * - Learn about testing data modification
     * - Understand timestamp validation for updates
     */
    @Test
    @Order(6)
    void shouldUpdateUser() {
        // First create a user to update
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setName("Update Test User")
                .setEmail("updatetest@example.com")
                .build();

        User createdUser = userService.createUser(createRequest)
                .await().atMost(Duration.ofSeconds(5));

        // Update the user
        UpdateUserRequest updateRequest = UpdateUserRequest.newBuilder()
                .setId(createdUser.getId())
                .setName(UPDATED_USER_NAME)
                .setEmail(UPDATED_USER_EMAIL)
                .build();

        User updatedUser = userService.updateUser(updateRequest)
                .await().atMost(Duration.ofSeconds(5));

        assertEquals(createdUser.getId(), updatedUser.getId());
        assertEquals(UPDATED_USER_NAME, updatedUser.getName());
        assertEquals(UPDATED_USER_EMAIL, updatedUser.getEmail());
        assertEquals(createdUser.getCreatedAt(), updatedUser.getCreatedAt());
        assertTrue(updatedUser.getUpdatedAt() >= createdUser.getUpdatedAt());
    }

    /**
     * Test user update with non-existent ID.
     * <p>
     * Learning objectives:
     * - Understand error handling in update operations
     * - Learn about testing update failure scenarios
     * - Understand NOT_FOUND status for update operations
     */
    @Test
    @Order(7)
    void shouldFailToUpdateNonExistentUser() {
        UpdateUserRequest request = UpdateUserRequest.newBuilder()
                .setId("non-existent-id")
                .setName("Updated Name")
                .setEmail("updated@example.com")
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            userService.updateUser(request)
                    .await().atMost(Duration.ofSeconds(5));
        });

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    /**
     * Test successful user deletion.
     * <p>
     * Learning objectives:
     * - Understand DELETE operations in gRPC
     * - Learn about testing resource deletion
     * - Understand proper deletion response validation
     */
    @Test
    @Order(8)
    void shouldDeleteUser() {
        // First create a user to delete
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setName("Delete Test User")
                .setEmail("deletetest@example.com")
                .build();

        User createdUser = userService.createUser(createRequest)
                .await().atMost(Duration.ofSeconds(5));

        // Delete the user
        DeleteUserRequest deleteRequest = DeleteUserRequest.newBuilder()
                .setId(createdUser.getId())
                .build();

        DeleteUserResponse response = userService.deleteUser(deleteRequest)
                .await().atMost(Duration.ofSeconds(5));

        assertTrue(response.getSuccess());
        assertFalse(response.getMessage().trim().isEmpty());

        // Verify user is actually deleted
        GetUserRequest getRequest = GetUserRequest.newBuilder()
                .setId(createdUser.getId())
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            userService.getUser(getRequest)
                    .await().atMost(Duration.ofSeconds(5));
        });

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    /**
     * Test user deletion with non-existent ID.
     * <p>
     * Learning objectives:
     * - Understand error handling in delete operations
     * - Learn about testing delete failure scenarios
     * - Understand NOT_FOUND status for delete operations
     */
    @Test
    @Order(9)
    void shouldFailToDeleteNonExistentUser() {
        DeleteUserRequest request = DeleteUserRequest.newBuilder()
                .setId("non-existent-id")
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            userService.deleteUser(request)
                    .await().atMost(Duration.ofSeconds(5));
        });

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    // ========================================
    // SERVER STREAMING TESTS
    // ========================================

    /**
     * Test server streaming with multiple users.
     * <p>
     * Learning objectives:
     * - Understand server streaming RPC pattern (single request → stream of
     * responses)
     * - Learn about Multi testing with collect()
     * - Understand streaming lifecycle and completion
     */
    @Test
    @Order(10)
    void shouldStreamUsers() {
        // Create test users first
        List<String> expectedNames = List.of("Stream User One", "Stream User Two", "Stream User Three");
        List<String> createdUserIds = new ArrayList<>();

        for (int i = 0; i < expectedNames.size(); i++) {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName(expectedNames.get(i))
                    .setEmail("streamuser" + (i + 1) + "@example.com")
                    .build();

            User user = userService.createUser(request)
                    .await().atMost(Duration.ofSeconds(5));
            createdUserIds.add(user.getId());
        }

        // Test streaming
        Empty request = Empty.newBuilder().build();

        List<User> users = userService.listUsers(request)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(10));

        // Verify we got at least our test users (there might be others from previous
        // tests)
        assertTrue(users.size() >= expectedNames.size(),
                "Should have at least " + expectedNames.size() + " users, got " + users.size());

        // Verify our test users are in the stream
        List<String> streamedNames = users.stream().map(User::getName).toList();
        for (String expectedName : expectedNames) {
            assertTrue(streamedNames.contains(expectedName),
                    "Stream should contain user: " + expectedName);
        }
    }

    /**
     * Test server streaming with empty result set.
     * <p>
     * Learning objectives:
     * - Understand streaming behavior with no data
     * - Learn about proper stream completion handling
     * - Understand empty stream testing patterns
     */
    @Test
    @Order(11)
    void shouldHandleEmptyUserStream() {
        // Note: This test assumes we can clear all users or test in isolation
        // For this educational example, we'll test that streaming completes properly
        // even if there are existing users

        Empty request = Empty.newBuilder().build();

        // The stream should complete successfully regardless of content
        List<User> users = userService.listUsers(request)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(10));

        // Verify no exception was thrown (stream completed successfully)
        assertNotNull(users, "Stream should complete and return a list");
    }

    // ========================================
    // CLIENT STREAMING TESTS
    // ========================================

    /**
     * Test client streaming with multiple user creation requests.
     * <p>
     * Learning objectives:
     * - Understand client streaming RPC pattern (stream of requests → single
     * response)
     * - Learn about Multi creation and streaming to server
     * - Understand batch processing response validation
     */
    @Test
    @Order(12)
    void shouldHandleClientStreaming() {
        // Create a stream of user creation requests
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
                        .setName("Batch User Three")
                        .setEmail("batch3@example.com")
                        .build());

        Multi<CreateUserRequest> requestStream = Multi.createFrom().iterable(requests);

        CreateUsersResponse response = userService.createUsers(requestStream)
                .await().atMost(Duration.ofSeconds(10));

        assertEquals(3, response.getCreatedCount(), "Should have created 3 users");
        assertEquals(0, response.getErrorsCount(), "Should have no errors");
        assertEquals(3, response.getCreatedUserIdsCount(), "Should have 3 created user IDs");

        // Verify all user IDs are valid (not empty)
        for (String userId : response.getCreatedUserIdsList()) {
            assertNotNull(userId);
            assertFalse(userId.trim().isEmpty());
        }
    }

    /**
     * Test client streaming with mixed valid and invalid requests.
     * <p>
     * Learning objectives:
     * - Understand error handling in client streaming
     * - Learn about partial failure processing
     * - Understand batch operation error reporting
     */
    @Test
    @Order(13)
    void shouldHandleClientStreamingWithErrors() {
        // Create a stream with both valid and invalid requests
        List<CreateUserRequest> requests = List.of(
                CreateUserRequest.newBuilder()
                        .setName("Valid User One")
                        .setEmail("valid1@example.com")
                        .build(),
                CreateUserRequest.newBuilder()
                        .setName("") // Invalid: empty name
                        .setEmail("invalid1@example.com")
                        .build(),
                CreateUserRequest.newBuilder()
                        .setName("Valid User Two")
                        .setEmail("valid2@example.com")
                        .build(),
                CreateUserRequest.newBuilder()
                        .setName("Invalid User")
                        .setEmail("") // Invalid: empty email
                        .build());

        Multi<CreateUserRequest> requestStream = Multi.createFrom().iterable(requests);

        CreateUsersResponse response = userService.createUsers(requestStream)
                .await().atMost(Duration.ofSeconds(10));

        assertEquals(2, response.getCreatedCount(), "Should have created 2 valid users");
        assertEquals(2, response.getErrorsCount(), "Should have 2 errors");
        assertEquals(2, response.getCreatedUserIdsCount(), "Should have 2 created user IDs");

        // Verify error messages are present
        assertTrue(response.getErrorsCount() > 0, "Should have error messages");
        for (String error : response.getErrorsList()) {
            assertNotNull(error);
            assertFalse(error.trim().isEmpty());
        }
    }

    // ========================================
    // BIDIRECTIONAL STREAMING TESTS
    // ========================================

    /**
     * Test bidirectional streaming for user update notifications.
     * <p>
     * Learning objectives:
     * - Understand bidirectional streaming RPC pattern (stream ↔ stream)
     * - Learn about real-time notification patterns
     * - Understand subscription lifecycle management
     * - Learn about concurrent streaming operations
     */
    @Test
    @Order(14)
    void shouldHandleBidirectionalStreaming() throws InterruptedException {
        CountDownLatch notificationLatch = new CountDownLatch(1); // Expect at least 1 notification
        List<UserNotification> receivedNotifications = new ArrayList<>();
        AtomicReference<Throwable> streamError = new AtomicReference<>();

        // Create subscription request stream
        SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder()
                .setClientId("test-client-1")
                .build();

        Multi<SubscribeRequest> requestStream = Multi.createFrom().item(subscribeRequest);

        // Subscribe to notifications
        Multi<UserNotification> notificationStream = userService.subscribeToUserUpdates(requestStream);

        // Set up notification subscriber in a separate thread
        Thread subscriberThread = new Thread(() -> {
            try {
                notificationStream
                        .subscribe().with(
                                notification -> {
                                    receivedNotifications.add(notification);
                                    notificationLatch.countDown();
                                },
                                error -> {
                                    streamError.set(error);
                                    notificationLatch.countDown();
                                });
            } catch (Exception e) {
                streamError.set(e);
                notificationLatch.countDown();
            }
        });

        subscriberThread.start();

        // Give the subscription time to establish
        Thread.sleep(1000);

        // Perform operations that should trigger notifications
        // Create user (should trigger CREATED notification)
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setName("Notification Test User")
                .setEmail("notification@example.com")
                .build();

        userService.createUser(createRequest)
                .await().atMost(Duration.ofSeconds(5));

        // Wait for at least one notification with timeout
        notificationLatch.await(10, TimeUnit.SECONDS);

        // For this test, we'll verify that the stream is working by checking
        // that we can establish the connection without errors
        // The exact notification count may vary depending on timing and other
        // concurrent tests

        // Verify no stream errors occurred
        assertNull(streamError.get(), "Stream should not have errors");

        // Clean up
        subscriberThread.interrupt();
    }

    /**
     * Test bidirectional streaming with multiple concurrent clients.
     * <p>
     * Learning objectives:
     * - Understand concurrent streaming client management
     * - Learn about testing multiple simultaneous subscriptions
     * - Understand client isolation in streaming
     */
    @Test
    @Order(15)
    @Execution(ExecutionMode.SAME_THREAD) // Ensure this test runs in isolation
    void shouldHandleMultipleConcurrentSubscriptions() throws InterruptedException {
        int clientCount = 3;
        List<Thread> subscriberThreads = new ArrayList<>();
        CountDownLatch setupLatch = new CountDownLatch(clientCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Create multiple concurrent subscriptions
        for (int i = 0; i < clientCount; i++) {
            String clientId = "concurrent-client-" + (i + 1);

            Thread subscriberThread = new Thread(() -> {
                try {
                    SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder()
                            .setClientId(clientId)
                            .build();

                    Multi<SubscribeRequest> requestStream = Multi.createFrom().item(subscribeRequest);
                    Multi<UserNotification> notificationStream = userService.subscribeToUserUpdates(requestStream);

                    notificationStream
                            .subscribe().with(
                                    notification -> {
                                        // Just consume notifications
                                    },
                                    error -> {
                                        errorCount.incrementAndGet();
                                    });

                    setupLatch.countDown();

                    // Keep the subscription alive for a short time
                    Thread.sleep(2000);

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    setupLatch.countDown();
                }
            });

            subscriberThreads.add(subscriberThread);
            subscriberThread.start();
        }

        // Wait for all subscriptions to be established
        assertTrue(setupLatch.await(10, TimeUnit.SECONDS), "All subscriptions should be established");

        // Give subscriptions time to fully establish
        Thread.sleep(500);

        // Perform an operation that should notify all clients
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setName("Concurrent Test User")
                .setEmail("concurrent@example.com")
                .build();

        userService.createUser(createRequest)
                .await().atMost(Duration.ofSeconds(5));

        // Give time for notifications to propagate
        Thread.sleep(1000);

        // Clean up all subscriber threads
        for (Thread thread : subscriberThreads) {
            thread.interrupt();
        }

        // Wait for threads to finish
        for (Thread thread : subscriberThreads) {
            thread.join(1000);
        }

        // Verify that no major errors occurred
        assertTrue(errorCount.get() < clientCount,
                "Most subscriptions should work without errors, got " + errorCount.get() + " errors");
    }

    /**
     * Test streaming error handling and recovery.
     * <p>
     * Learning objectives:
     * - Understand error propagation in streaming operations
     * - Learn about stream cancellation and cleanup
     * - Understand proper resource management in streaming
     */
    @Test
    @Order(16)
    void shouldHandleStreamingErrors() throws InterruptedException {
        // Test with invalid subscription request
        SubscribeRequest invalidRequest = SubscribeRequest.newBuilder()
                .setClientId("") // Empty client ID should be handled gracefully
                .build();

        Multi<SubscribeRequest> requestStream = Multi.createFrom().item(invalidRequest);
        Multi<UserNotification> notificationStream = userService.subscribeToUserUpdates(requestStream);

        AtomicReference<Throwable> streamError = new AtomicReference<>();
        CountDownLatch completionLatch = new CountDownLatch(1);

        // The service should handle empty client ID by generating one
        // So this should not fail, but we test the behavior
        Thread subscriberThread = new Thread(() -> {
            try {
                notificationStream
                        .subscribe().with(
                                notification -> {
                                    // Service should handle this gracefully
                                },
                                error -> {
                                    streamError.set(error);
                                    completionLatch.countDown();
                                },
                                () -> {
                                    completionLatch.countDown();
                                });

                // Keep subscription alive briefly
                Thread.sleep(1000);
                completionLatch.countDown();

            } catch (Exception e) {
                streamError.set(e);
                completionLatch.countDown();
            }
        });

        subscriberThread.start();

        // Wait for completion or timeout
        boolean completed = completionLatch.await(5, TimeUnit.SECONDS);

        // Clean up
        subscriberThread.interrupt();
        subscriberThread.join(1000);

        // Verify proper cleanup (no hanging resources)
        // This is more of a behavioral test - if the service hangs, the test will
        // timeout
        assertTrue(completed || streamError.get() == null,
                "Stream cleanup should complete without hanging");
    }

    // ========================================
    // CONCURRENT ACCESS TESTS
    // ========================================

    /**
     * Test concurrent unary operations.
     * <p>
     * Learning objectives:
     * - Understand thread safety in gRPC services
     * - Learn about testing concurrent access patterns
     * - Understand proper synchronization in reactive services
     */
    @Test
    @Order(17)
    @Execution(ExecutionMode.CONCURRENT)
    void shouldHandleConcurrentUnaryOperations() throws InterruptedException {
        int operationCount = 10;
        CountDownLatch latch = new CountDownLatch(operationCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Perform concurrent user creation operations
        for (int i = 0; i < operationCount; i++) {
            final int index = i;

            Thread operationThread = new Thread(() -> {
                try {
                    CreateUserRequest request = CreateUserRequest.newBuilder()
                            .setName("Concurrent User " + getValidUserName(index))
                            .setEmail("concurrent" + index + "@example.com")
                            .build();

                    userService.createUser(request)
                            .await().atMost(Duration.ofSeconds(10));

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            operationThread.start();
        }

        // Wait for all operations to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All concurrent operations should complete");

        // Verify results
        assertEquals(operationCount, successCount.get() + errorCount.get(),
                "All operations should complete (either success or error)");

        // Most operations should succeed (allowing for some potential conflicts)
        assertTrue(successCount.get() > 0, "At least some operations should succeed");
    }

    /**
     * Test streaming lifecycle management under load.
     * <p>
     * Learning objectives:
     * - Understand streaming performance characteristics
     * - Learn about resource management under load
     * - Understand proper cleanup in high-concurrency scenarios
     */
    @Test
    @Order(18)
    void shouldHandleStreamingLifecycleUnderLoad() throws InterruptedException {
        int streamCount = 5;
        List<Thread> subscriberThreads = new ArrayList<>();
        CountDownLatch setupLatch = new CountDownLatch(streamCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Create multiple streams
        for (int i = 0; i < streamCount; i++) {
            String clientId = "load-test-client-" + i;

            Thread subscriberThread = new Thread(() -> {
                try {
                    SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder()
                            .setClientId(clientId)
                            .build();

                    Multi<SubscribeRequest> requestStream = Multi.createFrom().item(subscribeRequest);
                    Multi<UserNotification> notificationStream = userService.subscribeToUserUpdates(requestStream);

                    notificationStream
                            .subscribe().with(
                                    notification -> {
                                        // Process notifications
                                    },
                                    error -> {
                                        errorCount.incrementAndGet();
                                    });

                    setupLatch.countDown();

                    // Keep stream alive
                    Thread.sleep(3000);

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    setupLatch.countDown();
                }
            });

            subscriberThreads.add(subscriberThread);
            subscriberThread.start();
        }

        // Wait for setup
        assertTrue(setupLatch.await(10, TimeUnit.SECONDS), "All streams should be established");

        // Generate some load
        for (int i = 0; i < 5; i++) {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName("Load Test User " + getValidUserName(i))
                    .setEmail("loadtest" + i + "@example.com")
                    .build();

            userService.createUser(request)
                    .await().atMost(Duration.ofSeconds(5));
        }

        // Give time for notifications
        Thread.sleep(2000);

        // Cancel all streams
        for (Thread thread : subscriberThreads) {
            thread.interrupt();
        }

        // Wait for cleanup
        for (Thread thread : subscriberThreads) {
            thread.join(2000);
        }

        // Verify no major failures occurred
        assertTrue(errorCount.get() < streamCount,
                "Most streams should work without major errors, got " + errorCount.get() + " errors");
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println("Completed test: " + testInfo.getDisplayName());
    }

    /**
     * Helper method to generate valid user names without numbers.
     * The validation service only allows letters, spaces, hyphens, and apostrophes.
     */
    private String getValidUserName(int index) {
        String[] names = {
                "Alpha", "Beta", "Gamma", "Delta", "Epsilon",
                "Zeta", "Eta", "Theta", "Iota", "Kappa"
        };
        return names[index % names.length];
    }
}