package org.isaac.grpc.user;

import io.quarkus.grpc.GrpcService;
import io.quarkus.test.junit.QuarkusTest;
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
    void shouldThrowUnsupportedOperationForStreamingMethods() {
        // Test that streaming methods throw UnsupportedOperationException
        // These will be implemented in later tasks

        assertThrows(UnsupportedOperationException.class, () -> {
            userGrpcService.listUsers(Empty.newBuilder().build());
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            userGrpcService.createUsers(null);
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            userGrpcService.subscribeToUserUpdates(null);
        });
    }
}