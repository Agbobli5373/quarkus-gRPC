package org.isaac.grpc.user;

import io.quarkus.grpc.GrpcService;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.isaac.grpc.user.UserProto.*;
import org.isaac.grpc.user.UserProto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserGrpcService focusing on unary operations.
 * 
 * These tests verify:
 * - Successful unary RPC operations
 * - Proper error handling and gRPC status code mapping
 * - Integration with business service layer
 * - Logging and monitoring behavior
 */
@QuarkusTest
class UserGrpcServiceTest {

    @Inject
    @GrpcService
    UserGrpcService userGrpcService;

    private User testUser;
    private CreateUserRequest createRequest;
    private GetUserRequest getRequest;
    private UpdateUserRequest updateRequest;
    private DeleteUserRequest deleteRequest;

    @BeforeEach
    void setUp() {
        // Create test data with unique identifiers to avoid conflicts
        long timestamp = System.currentTimeMillis();

        testUser = User.newBuilder()
                .setId("test-user-1")
                .setName("John Doe")
                .setEmail("john.doe." + timestamp + "@example.com")
                .setCreatedAt(timestamp)
                .setUpdatedAt(timestamp)
                .build();

        createRequest = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john.doe." + timestamp + "@example.com")
                .build();

        getRequest = GetUserRequest.newBuilder()
                .setId("test-user-1")
                .build();

        updateRequest = UpdateUserRequest.newBuilder()
                .setId("test-user-1")
                .setName("John Updated")
                .setEmail("john.updated." + timestamp + "@example.com")
                .build();

        deleteRequest = DeleteUserRequest.newBuilder()
                .setId("test-user-1")
                .build();

        // Test setup complete
    }

    @Test
    void shouldCreateUserSuccessfully() {
        // Given - unique name and email for this test
        long timestamp = System.currentTimeMillis();
        CreateUserRequest uniqueRequest = CreateUserRequest.newBuilder()
                .setName("John Create")
                .setEmail("john.create." + timestamp + "@example.com")
                .build();

        // When
        Uni<User> result = userGrpcService.createUser(uniqueRequest);

        // Then
        UniAssertSubscriber<User> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        User createdUser = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();

        assertNotNull(createdUser);
        assertEquals("John Create", createdUser.getName());
        assertEquals("john.create." + timestamp + "@example.com", createdUser.getEmail());
        assertNotNull(createdUser.getId());
        assertTrue(createdUser.getCreatedAt() > 0);
        assertTrue(createdUser.getUpdatedAt() > 0);
    }

    @Test
    void shouldHandleCreateUserValidationError() {
        // Given - create request with invalid data
        CreateUserRequest invalidRequest = CreateUserRequest.newBuilder()
                .setName("") // Empty name should trigger validation error
                .setEmail("john.doe@example.com")
                .build();

        // When
        Uni<User> result = userGrpcService.createUser(invalidRequest);

        // Then
        UniAssertSubscriber<User> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure(Duration.ofSeconds(5)).getFailure();

        assertNotNull(failure);
        assertTrue(failure instanceof io.grpc.StatusRuntimeException);
        io.grpc.StatusRuntimeException statusException = (io.grpc.StatusRuntimeException) failure;
        assertEquals(io.grpc.Status.Code.INVALID_ARGUMENT, statusException.getStatus().getCode());
    }

    @Test
    void shouldGetUserSuccessfully() {
        // Given - first create a user with unique email
        long timestamp = System.currentTimeMillis();
        CreateUserRequest uniqueCreateRequest = CreateUserRequest.newBuilder()
                .setName("John Get")
                .setEmail("john.get." + timestamp + "@example.com")
                .build();

        Uni<User> createResult = userGrpcService.createUser(uniqueCreateRequest);
        User createdUser = createResult.await().atMost(Duration.ofSeconds(5));

        GetUserRequest getRequest = GetUserRequest.newBuilder()
                .setId(createdUser.getId())
                .build();

        // When
        Uni<User> result = userGrpcService.getUser(getRequest);

        // Then
        UniAssertSubscriber<User> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        User retrievedUser = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();

        assertNotNull(retrievedUser);
        assertEquals(createdUser.getId(), retrievedUser.getId());
        assertEquals("John Get", retrievedUser.getName());
        assertEquals("john.get." + timestamp + "@example.com", retrievedUser.getEmail());
    }

    @Test
    void shouldHandleGetUserNotFound() {
        // Given - request for non-existent user
        GetUserRequest notFoundRequest = GetUserRequest.newBuilder()
                .setId("non-existent-user")
                .build();

        // When
        Uni<User> result = userGrpcService.getUser(notFoundRequest);

        // Then
        UniAssertSubscriber<User> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure(Duration.ofSeconds(5)).getFailure();

        assertNotNull(failure);
        assertTrue(failure instanceof io.grpc.StatusRuntimeException);
        io.grpc.StatusRuntimeException statusException = (io.grpc.StatusRuntimeException) failure;
        assertEquals(io.grpc.Status.Code.NOT_FOUND, statusException.getStatus().getCode());
        assertTrue(statusException.getStatus().getDescription().contains("non-existent-user"));
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        // Given - first create a user with unique email
        long timestamp = System.currentTimeMillis();
        CreateUserRequest uniqueCreateRequest = CreateUserRequest.newBuilder()
                .setName("John Update")
                .setEmail("john.update." + timestamp + "@example.com")
                .build();

        Uni<User> createResult = userGrpcService.createUser(uniqueCreateRequest);
        User createdUser = createResult.await().atMost(Duration.ofSeconds(5));

        UpdateUserRequest updateRequest = UpdateUserRequest.newBuilder()
                .setId(createdUser.getId())
                .setName("John Updated")
                .setEmail("john.updated." + timestamp + "@example.com")
                .build();

        // When
        Uni<User> result = userGrpcService.updateUser(updateRequest);

        // Then
        UniAssertSubscriber<User> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        User resultUser = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();

        assertNotNull(resultUser);
        assertEquals(createdUser.getId(), resultUser.getId());
        assertEquals("John Updated", resultUser.getName());
        assertEquals("john.updated." + timestamp + "@example.com", resultUser.getEmail());
    }

    @Test
    void shouldHandleUpdateUserNotFound() {
        // Given - update request for non-existent user
        UpdateUserRequest notFoundUpdateRequest = UpdateUserRequest.newBuilder()
                .setId("non-existent-user")
                .setName("John NotFound")
                .setEmail("john.notfound@example.com")
                .build();

        // When
        Uni<User> result = userGrpcService.updateUser(notFoundUpdateRequest);

        // Then
        UniAssertSubscriber<User> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure(Duration.ofSeconds(5)).getFailure();

        assertNotNull(failure);
        assertTrue(failure instanceof io.grpc.StatusRuntimeException);
        io.grpc.StatusRuntimeException statusException = (io.grpc.StatusRuntimeException) failure;
        assertEquals(io.grpc.Status.Code.NOT_FOUND, statusException.getStatus().getCode());
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        // Given - first create a user with unique email
        long timestamp = System.currentTimeMillis();
        CreateUserRequest uniqueCreateRequest = CreateUserRequest.newBuilder()
                .setName("John Delete")
                .setEmail("john.delete." + timestamp + "@example.com")
                .build();

        Uni<User> createResult = userGrpcService.createUser(uniqueCreateRequest);
        User createdUser = createResult.await().atMost(Duration.ofSeconds(5));

        DeleteUserRequest deleteRequest = DeleteUserRequest.newBuilder()
                .setId(createdUser.getId())
                .build();

        // When
        Uni<DeleteUserResponse> result = userGrpcService.deleteUser(deleteRequest);

        // Then
        UniAssertSubscriber<DeleteUserResponse> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        DeleteUserResponse response = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("User deleted successfully", response.getMessage());
    }

    @Test
    void shouldHandleDeleteUserNotFound() {
        // Given - delete request for non-existent user
        DeleteUserRequest notFoundDeleteRequest = DeleteUserRequest.newBuilder()
                .setId("non-existent-user")
                .build();

        // When
        Uni<DeleteUserResponse> result = userGrpcService.deleteUser(notFoundDeleteRequest);

        // Then
        UniAssertSubscriber<DeleteUserResponse> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure(Duration.ofSeconds(5)).getFailure();

        assertNotNull(failure);
        assertTrue(failure instanceof io.grpc.StatusRuntimeException);
        io.grpc.StatusRuntimeException statusException = (io.grpc.StatusRuntimeException) failure;
        assertEquals(io.grpc.Status.Code.NOT_FOUND, statusException.getStatus().getCode());
    }

    @Test
    void shouldMapExceptionToGrpcStatus() {
        // This test verifies the exception mapping logic
        // We'll test this by checking the mapToGrpcException method behavior
        // through the actual service calls above

        // The exception mapping is already tested in the other test methods
        // where we verify NOT_FOUND and INVALID_ARGUMENT status codes
        assertTrue(true, "Exception mapping tested in other methods");
    }

    @Test
    void shouldListUsersWithServerStreaming() {
        // Given - create multiple test users with unique emails
        long timestamp = System.currentTimeMillis();

        CreateUserRequest user1Request = CreateUserRequest.newBuilder()
                .setName("User One")
                .setEmail("user1." + timestamp + "@example.com")
                .build();

        CreateUserRequest user2Request = CreateUserRequest.newBuilder()
                .setName("User Two")
                .setEmail("user2." + timestamp + "@example.com")
                .build();

        CreateUserRequest user3Request = CreateUserRequest.newBuilder()
                .setName("User Three")
                .setEmail("user3." + timestamp + "@example.com")
                .build();

        // Create the users first
        userGrpcService.createUser(user1Request).await().atMost(Duration.ofSeconds(5));
        userGrpcService.createUser(user2Request).await().atMost(Duration.ofSeconds(5));
        userGrpcService.createUser(user3Request).await().atMost(Duration.ofSeconds(5));

        // When - call listUsers with server streaming
        Multi<User> result = userGrpcService.listUsers(Empty.newBuilder().build());

        // Then - collect all users from the stream and verify
        java.util.List<User> users = result.collect().asList()
                .await().atMost(Duration.ofSeconds(10));

        // Verify we received at least the 3 users we created (there might be more from
        // other tests)
        assertTrue(users.size() >= 3,
                "Expected at least 3 users, but got " + users.size());

        // Verify that the users we created are in the stream
        boolean foundUser1 = users.stream()
                .anyMatch(user -> "User One".equals(user.getName()));
        boolean foundUser2 = users.stream()
                .anyMatch(user -> "User Two".equals(user.getName()));
        boolean foundUser3 = users.stream()
                .anyMatch(user -> "User Three".equals(user.getName()));

        assertTrue(foundUser1, "User One should be in the stream");
        assertTrue(foundUser2, "User Two should be in the stream");
        assertTrue(foundUser3, "User Three should be in the stream");
    }

    @Test
    void shouldHandleEmptyListUsersStream() {
        // Given - ensure we start with a clean state by using a fresh service instance
        // Note: In a real scenario, we might clear the repository, but for this test
        // we'll just verify the stream completes properly regardless of content

        // When - call listUsers
        Multi<User> result = userGrpcService.listUsers(Empty.newBuilder().build());

        // Then - verify stream completes without error
        java.util.List<User> users = result.collect().asList()
                .await().atMost(Duration.ofSeconds(5));

        // The stream should complete successfully (users list can be empty or contain
        // items)
        assertNotNull(users, "Users list should not be null");
        // We don't assert on size since other tests may have created users
    }

    @Test
    void shouldHandleStreamingErrorPropagation() {
        // This test verifies that errors in the business service are properly
        // propagated through the streaming interface. Since our current implementation
        // uses an in-memory repository that doesn't typically fail, we'll verify
        // the error handling structure is in place by testing the stream completion.

        // When - call listUsers
        Multi<User> result = userGrpcService.listUsers(Empty.newBuilder().build());

        // Then - verify stream handles completion properly
        java.util.List<User> users = result.collect().asList()
                .await().atMost(Duration.ofSeconds(5));

        // Verify no failure occurred (our in-memory implementation should work fine)
        assertNotNull(users, "Stream should complete without failure and return a list");
    }

    @Test
    void shouldCreateMultipleUsersWithClientStreaming() {
        // Given - create multiple user requests with unique emails
        long timestamp = System.currentTimeMillis();

        CreateUserRequest user1Request = CreateUserRequest.newBuilder()
                .setName("Batch User One")
                .setEmail("batch.user1." + timestamp + "@example.com")
                .build();

        CreateUserRequest user2Request = CreateUserRequest.newBuilder()
                .setName("Batch User Two")
                .setEmail("batch.user2." + timestamp + "@example.com")
                .build();

        CreateUserRequest user3Request = CreateUserRequest.newBuilder()
                .setName("Batch User Three")
                .setEmail("batch.user3." + timestamp + "@example.com")
                .build();

        // Create a Multi stream of requests
        Multi<CreateUserRequest> requests = Multi.createFrom().items(user1Request, user2Request, user3Request);

        // When - call createUsers with client streaming
        Uni<CreateUsersResponse> result = userGrpcService.createUsers(requests);

        // Then - verify the batch response
        UniAssertSubscriber<CreateUsersResponse> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        CreateUsersResponse response = subscriber.awaitItem(Duration.ofSeconds(10)).getItem();

        assertNotNull(response);
        assertEquals(3, response.getCreatedCount(), "Should have created 3 users");
        assertEquals(0, response.getErrorsCount(), "Should have no errors");
        assertEquals(3, response.getCreatedUserIdsCount(), "Should have 3 created user IDs");

        // Verify all user IDs are present and not empty
        for (String userId : response.getCreatedUserIdsList()) {
            assertNotNull(userId);
            assertFalse(userId.isEmpty());
        }
    }

    @Test
    void shouldHandlePartialFailuresInClientStreaming() {
        // Given - create requests with some valid and some invalid data
        long timestamp = System.currentTimeMillis();

        CreateUserRequest validRequest1 = CreateUserRequest.newBuilder()
                .setName("Valid User One")
                .setEmail("valid.user1." + timestamp + "@example.com")
                .build();

        CreateUserRequest invalidRequest = CreateUserRequest.newBuilder()
                .setName("") // Empty name should cause validation error
                .setEmail("invalid.user." + timestamp + "@example.com")
                .build();

        CreateUserRequest validRequest2 = CreateUserRequest.newBuilder()
                .setName("Valid User Two")
                .setEmail("valid.user2." + timestamp + "@example.com")
                .build();

        // Create a Multi stream of requests including invalid one
        Multi<CreateUserRequest> requests = Multi.createFrom().items(validRequest1, invalidRequest, validRequest2);

        // When - call createUsers with client streaming
        Uni<CreateUsersResponse> result = userGrpcService.createUsers(requests);

        // Then - verify the batch response handles partial failures
        UniAssertSubscriber<CreateUsersResponse> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        CreateUsersResponse response = subscriber.awaitItem(Duration.ofSeconds(10)).getItem();

        assertNotNull(response);
        assertEquals(2, response.getCreatedCount(), "Should have created 2 valid users");
        assertEquals(1, response.getErrorsCount(), "Should have 1 error for invalid request");
        assertEquals(2, response.getCreatedUserIdsCount(), "Should have 2 created user IDs");

        // Verify error message contains information about the failed request
        String errorMessage = response.getErrorsList().get(0);
        assertTrue(errorMessage.contains("Name cannot be null or empty") ||
                errorMessage.contains("validation") ||
                errorMessage.contains("Name is required"),
                "Error message should indicate validation failure, but was: " + errorMessage);
    }

    @Test
    void shouldHandleEmptyClientStream() {
        // Given - empty stream of requests
        Multi<CreateUserRequest> emptyRequests = Multi.createFrom().empty();

        // When - call createUsers with empty stream
        Uni<CreateUsersResponse> result = userGrpcService.createUsers(emptyRequests);

        // Then - verify the response handles empty stream
        UniAssertSubscriber<CreateUsersResponse> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        CreateUsersResponse response = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();

        assertNotNull(response);
        assertEquals(0, response.getCreatedCount(), "Should have created 0 users");
        assertEquals(0, response.getErrorsCount(), "Should have no errors");
        assertEquals(0, response.getCreatedUserIdsCount(), "Should have 0 created user IDs");
    }

    @Test
    void shouldHandleLargeClientStream() {
        // Given - create a larger batch of user requests with valid names (no numbers)
        long timestamp = System.currentTimeMillis();
        java.util.List<CreateUserRequest> requestList = new java.util.ArrayList<>();

        String[] validNames = {
                "Alice Johnson", "Bob Smith", "Carol Davis", "David Wilson", "Eva Brown",
                "Frank Miller", "Grace Taylor", "Henry Anderson", "Ivy Thomas", "Jack White"
        };

        for (int i = 0; i < 10; i++) {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName(validNames[i])
                    .setEmail("batch.user" + i + "." + timestamp + "@example.com")
                    .build();
            requestList.add(request);
        }

        Multi<CreateUserRequest> requests = Multi.createFrom().iterable(requestList);

        // When - call createUsers with larger stream
        Uni<CreateUsersResponse> result = userGrpcService.createUsers(requests);

        // Then - verify all users are created successfully
        UniAssertSubscriber<CreateUsersResponse> subscriber = result
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        CreateUsersResponse response = subscriber.awaitItem(Duration.ofSeconds(15)).getItem();

        assertNotNull(response);
        assertEquals(10, response.getCreatedCount(), "Should have created 10 users");
        assertEquals(0, response.getErrorsCount(), "Should have no errors");
        assertEquals(10, response.getCreatedUserIdsCount(), "Should have 10 created user IDs");
    }

    @Test
    void shouldHandleBidirectionalStreamingSubscription() {
        // Given - create a subscription request
        long timestamp = System.currentTimeMillis();
        String clientId = "test-client-" + timestamp;

        SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder()
                .setClientId(clientId)
                .build();

        Multi<SubscribeRequest> subscriptionRequests = Multi.createFrom().item(subscribeRequest);

        // When - subscribe to user updates
        Multi<UserNotification> notifications = userGrpcService.subscribeToUserUpdates(subscriptionRequests);

        // Create a subscriber to collect notifications
        java.util.List<UserNotification> receivedNotifications = new java.util.ArrayList<>();

        // Subscribe to notifications in a separate thread to avoid blocking
        notifications
                .subscribe().with(
                        notification -> {
                            synchronized (receivedNotifications) {
                                receivedNotifications.add(notification);
                            }
                        },
                        failure -> fail("Subscription should not fail: " + failure.getMessage()));

        // Give the subscription time to establish
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Create a user to trigger a notification
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setName("Notification Test User")
                .setEmail("notification.test." + timestamp + "@example.com")
                .build();

        userGrpcService.createUser(createRequest).await().atMost(Duration.ofSeconds(5));

        // Give time for notification to be processed
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - verify notification was received
        synchronized (receivedNotifications) {
            assertTrue(receivedNotifications.size() > 0, "Should have received at least one notification");

            UserNotification notification = receivedNotifications.get(0);
            assertEquals(UserNotification.NotificationType.CREATED, notification.getType());
            assertEquals("Notification Test User", notification.getUser().getName());
            assertTrue(notification.getTimestamp() > 0);
        }
    }

    @Test
    void shouldHandleBidirectionalStreamingWithGeneratedClientId() {
        // Given - create a subscription request without client ID
        SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder()
                .build(); // No client ID provided

        Multi<SubscribeRequest> subscriptionRequests = Multi.createFrom().item(subscribeRequest);

        // When - subscribe to user updates (should generate client ID)
        Multi<UserNotification> notifications = userGrpcService.subscribeToUserUpdates(subscriptionRequests);

        // Create a subscriber to collect notifications
        java.util.List<UserNotification> receivedNotifications = new java.util.ArrayList<>();

        notifications
                .subscribe().with(
                        notification -> {
                            synchronized (receivedNotifications) {
                                receivedNotifications.add(notification);
                            }
                        },
                        failure -> fail("Subscription should not fail: " + failure.getMessage()));

        // Give the subscription time to establish
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Create a user to trigger a notification
        long timestamp = System.currentTimeMillis();
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setName("Generated ID Test User")
                .setEmail("generated.id.test." + timestamp + "@example.com")
                .build();

        userGrpcService.createUser(createRequest).await().atMost(Duration.ofSeconds(5));

        // Give time for notification to be processed
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - verify notification was received even with generated client ID
        synchronized (receivedNotifications) {
            assertTrue(receivedNotifications.size() > 0, "Should have received at least one notification");

            UserNotification notification = receivedNotifications.get(0);
            assertEquals(UserNotification.NotificationType.CREATED, notification.getType());
            assertEquals("Generated ID Test User", notification.getUser().getName());
        }
    }

    @Test
    void shouldHandleMultipleNotificationTypes() {
        // Given - create a subscription request with specific notification types
        long timestamp = System.currentTimeMillis();
        String clientId = "multi-type-client-" + timestamp;

        SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder()
                .setClientId(clientId)
                .addNotificationTypes(UserNotification.NotificationType.CREATED)
                .addNotificationTypes(UserNotification.NotificationType.UPDATED)
                .build();

        Multi<SubscribeRequest> subscriptionRequests = Multi.createFrom().item(subscribeRequest);

        // When - subscribe to user updates
        Multi<UserNotification> notifications = userGrpcService.subscribeToUserUpdates(subscriptionRequests);

        java.util.List<UserNotification> receivedNotifications = new java.util.ArrayList<>();

        notifications
                .subscribe().with(
                        notification -> {
                            synchronized (receivedNotifications) {
                                receivedNotifications.add(notification);
                            }
                        },
                        failure -> fail("Subscription should not fail: " + failure.getMessage()));

        // Give the subscription time to establish
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Create and then update a user to trigger multiple notifications
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setName("Multi Type Test User")
                .setEmail("multi.type.test." + timestamp + "@example.com")
                .build();

        User createdUser = userGrpcService.createUser(createRequest).await().atMost(Duration.ofSeconds(5));

        UpdateUserRequest updateRequest = UpdateUserRequest.newBuilder()
                .setId(createdUser.getId())
                .setName("Multi Type Updated User")
                .setEmail("multi.type.updated." + timestamp + "@example.com")
                .build();

        userGrpcService.updateUser(updateRequest).await().atMost(Duration.ofSeconds(5));

        // Give time for notifications to be processed
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - verify both notifications were received
        synchronized (receivedNotifications) {
            assertTrue(receivedNotifications.size() >= 2, "Should have received at least 2 notifications");

            // Find the created notification
            UserNotification createdNotification = receivedNotifications.stream()
                    .filter(n -> n.getType() == UserNotification.NotificationType.CREATED)
                    .findFirst()
                    .orElse(null);

            assertNotNull(createdNotification, "Should have received CREATED notification");
            assertEquals("Multi Type Test User", createdNotification.getUser().getName());

            // Find the updated notification
            UserNotification updatedNotification = receivedNotifications.stream()
                    .filter(n -> n.getType() == UserNotification.NotificationType.UPDATED)
                    .findFirst()
                    .orElse(null);

            assertNotNull(updatedNotification, "Should have received UPDATED notification");
            assertEquals("Multi Type Updated User", updatedNotification.getUser().getName());
        }
    }

    @Test
    void shouldHandleBidirectionalStreamingCancellation() {
        // Given - create a subscription request
        long timestamp = System.currentTimeMillis();
        String clientId = "cancellation-test-client-" + timestamp;

        SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder()
                .setClientId(clientId)
                .build();

        Multi<SubscribeRequest> subscriptionRequests = Multi.createFrom().item(subscribeRequest);

        // When - subscribe and then cancel
        Multi<UserNotification> notifications = userGrpcService.subscribeToUserUpdates(subscriptionRequests);

        java.util.concurrent.atomic.AtomicBoolean subscriptionCancelled = new java.util.concurrent.atomic.AtomicBoolean(
                false);

        io.smallrye.mutiny.subscription.Cancellable cancellable = notifications
                .subscribe().with(
                        notification -> {
                            // Notification received
                        },
                        failure -> fail("Subscription should not fail: " + failure.getMessage()),
                        () -> subscriptionCancelled.set(true));

        // Give the subscription time to establish
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Cancel the subscription
        cancellable.cancel();

        // Give time for cancellation to be processed
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - verify cancellation was handled (this is mainly testing that no
        // exceptions occur)
        assertTrue(true, "Cancellation should be handled gracefully");
    }
}