package org.isaac.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.isaac.exception.DuplicateEmailException;
import org.isaac.exception.UserNotFoundException;
import org.isaac.exception.ValidationException;
import org.isaac.grpc.user.UserProto.CreateUserRequest;
import org.isaac.grpc.user.UserProto.CreateUsersResponse;
import org.isaac.grpc.user.UserProto.UpdateUserRequest;
import org.isaac.grpc.user.UserProto.User;
import org.isaac.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserBusinessService.
 * 
 * These tests verify the business logic, service orchestration,
 * and error handling patterns in the business service layer.
 */
@QuarkusTest
class UserBusinessServiceTest {

    @Inject
    UserBusinessService userBusinessService;

    @Inject
    UserRepository userRepository;

    @Inject
    NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Clear repository before each test
        userRepository.clear().await().atMost(Duration.ofSeconds(5));

        // Clear notification service subscriptions
        notificationService.cleanup();
    }

    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .build();

        // When
        UniAssertSubscriber<User> subscriber = userBusinessService.createUser(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        User user = subscriber.awaitItem().getItem();
        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals("John Doe", user.getName());
        assertEquals("john@example.com", user.getEmail());
        assertTrue(user.getCreatedAt() > 0);
        assertTrue(user.getUpdatedAt() > 0);
    }

    @Test
    void shouldFailToCreateUserWithInvalidName() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName("") // Invalid empty name
                .setEmail("john@example.com")
                .build();

        // When
        UniAssertSubscriber<User> subscriber = userBusinessService.createUser(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        subscriber.awaitFailure();
        Throwable failure = subscriber.getFailure();
        assertInstanceOf(ValidationException.class, failure);
    }

    @Test
    void shouldFailToCreateUserWithDuplicateEmail() {
        // Given
        CreateUserRequest firstRequest = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .build();

        CreateUserRequest secondRequest = CreateUserRequest.newBuilder()
                .setName("Jane Doe")
                .setEmail("john@example.com") // Same email
                .build();

        // When - Create first user
        userBusinessService.createUser(firstRequest)
                .await().atMost(Duration.ofSeconds(5));

        // When - Try to create second user with same email
        UniAssertSubscriber<User> subscriber = userBusinessService.createUser(secondRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        subscriber.awaitFailure();
        Throwable failure = subscriber.getFailure();
        assertInstanceOf(DuplicateEmailException.class, failure);
    }

    @Test
    void shouldGetUserSuccessfully() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .build();

        User createdUser = userBusinessService.createUser(request)
                .await().atMost(Duration.ofSeconds(5));

        // When
        UniAssertSubscriber<User> subscriber = userBusinessService.getUser(createdUser.getId())
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        User retrievedUser = subscriber.awaitItem().getItem();
        assertEquals(createdUser.getId(), retrievedUser.getId());
        assertEquals(createdUser.getName(), retrievedUser.getName());
        assertEquals(createdUser.getEmail(), retrievedUser.getEmail());
    }

    @Test
    void shouldFailToGetNonExistentUser() {
        // Given
        String nonExistentId = "non-existent-id";

        // When
        UniAssertSubscriber<User> subscriber = userBusinessService.getUser(nonExistentId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        subscriber.awaitFailure();
        Throwable failure = subscriber.getFailure();
        assertInstanceOf(UserNotFoundException.class, failure);
        assertEquals("User with ID 'non-existent-id' not found", failure.getMessage());
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        // Given
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .build();

        User createdUser = userBusinessService.createUser(createRequest)
                .await().atMost(Duration.ofSeconds(5));

        UpdateUserRequest updateRequest = UpdateUserRequest.newBuilder()
                .setId(createdUser.getId())
                .setName("John Smith")
                .setEmail("john.smith@example.com")
                .build();

        // When
        UniAssertSubscriber<User> subscriber = userBusinessService.updateUser(updateRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        User updatedUser = subscriber.awaitItem().getItem();
        assertEquals(createdUser.getId(), updatedUser.getId());
        assertEquals("John Smith", updatedUser.getName());
        assertEquals("john.smith@example.com", updatedUser.getEmail());
        assertEquals(createdUser.getCreatedAt(), updatedUser.getCreatedAt());
        assertTrue(updatedUser.getUpdatedAt() >= createdUser.getUpdatedAt());
    }

    @Test
    void shouldFailToUpdateNonExistentUser() {
        // Given
        UpdateUserRequest updateRequest = UpdateUserRequest.newBuilder()
                .setId("non-existent-id")
                .setName("John Smith")
                .setEmail("john.smith@example.com")
                .build();

        // When
        UniAssertSubscriber<User> subscriber = userBusinessService.updateUser(updateRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        subscriber.awaitFailure();
        Throwable failure = subscriber.getFailure();
        assertInstanceOf(UserNotFoundException.class, failure);
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .build();

        User createdUser = userBusinessService.createUser(request)
                .await().atMost(Duration.ofSeconds(5));

        // When
        UniAssertSubscriber<Void> subscriber = userBusinessService.deleteUser(createdUser.getId())
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        subscriber.awaitItem();

        // Verify user is deleted
        UniAssertSubscriber<User> getSubscriber = userBusinessService.getUser(createdUser.getId())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        getSubscriber.awaitFailure();
        assertInstanceOf(UserNotFoundException.class, getSubscriber.getFailure());
    }

    @Test
    void shouldFailToDeleteNonExistentUser() {
        // Given
        String nonExistentId = "non-existent-id";

        // When
        UniAssertSubscriber<Void> subscriber = userBusinessService.deleteUser(nonExistentId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        subscriber.awaitFailure();
        Throwable failure = subscriber.getFailure();
        assertInstanceOf(UserNotFoundException.class, failure);
    }

    @Test
    void shouldGetAllUsersSuccessfully() {
        // Given
        CreateUserRequest request1 = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .build();

        CreateUserRequest request2 = CreateUserRequest.newBuilder()
                .setName("Jane Smith")
                .setEmail("jane@example.com")
                .build();

        userBusinessService.createUser(request1).await().atMost(Duration.ofSeconds(5));
        userBusinessService.createUser(request2).await().atMost(Duration.ofSeconds(5));

        // When
        AssertSubscriber<User> subscriber = userBusinessService.getAllUsers()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        // Then
        List<User> users = subscriber.awaitCompletion().getItems();
        assertEquals(2, users.size());

        // Verify both users are present
        assertTrue(users.stream().anyMatch(u -> "John Doe".equals(u.getName())));
        assertTrue(users.stream().anyMatch(u -> "Jane Smith".equals(u.getName())));
    }

    @Test
    void shouldGetAllUsersWhenEmpty() {
        // When
        AssertSubscriber<User> subscriber = userBusinessService.getAllUsers()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        // Then
        List<User> users = subscriber.awaitCompletion().getItems();
        assertTrue(users.isEmpty());
    }

    @Test
    void shouldCreateMultipleUsersSuccessfully() {
        // Given
        CreateUserRequest request1 = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .build();

        CreateUserRequest request2 = CreateUserRequest.newBuilder()
                .setName("Jane Smith")
                .setEmail("jane@example.com")
                .build();

        Multi<CreateUserRequest> requests = Multi.createFrom().items(request1, request2);

        // When
        UniAssertSubscriber<CreateUsersResponse> subscriber = userBusinessService.createMultipleUsers(requests)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        CreateUsersResponse response = subscriber.awaitItem().getItem();
        assertEquals(2, response.getCreatedCount());
        assertEquals(0, response.getErrorsCount());
        assertEquals(2, response.getCreatedUserIdsCount());
    }

    @Test
    void shouldCreateMultipleUsersWithPartialFailures() {
        // Given
        CreateUserRequest validRequest = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .build();

        CreateUserRequest invalidRequest = CreateUserRequest.newBuilder()
                .setName("") // Invalid empty name
                .setEmail("invalid@example.com")
                .build();

        Multi<CreateUserRequest> requests = Multi.createFrom().items(validRequest, invalidRequest);

        // When
        UniAssertSubscriber<CreateUsersResponse> subscriber = userBusinessService.createMultipleUsers(requests)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        CreateUsersResponse response = subscriber.awaitItem().getItem();
        assertEquals(1, response.getCreatedCount());
        assertEquals(1, response.getErrorsCount());
        assertEquals(1, response.getCreatedUserIdsCount());
        assertTrue(response.getErrors(0).contains("Failed to create user"));
    }

    @Test
    void shouldGetUserCountSuccessfully() {
        // Given
        CreateUserRequest request1 = CreateUserRequest.newBuilder()
                .setName("John Doe")
                .setEmail("john@example.com")
                .build();

        CreateUserRequest request2 = CreateUserRequest.newBuilder()
                .setName("Jane Smith")
                .setEmail("jane@example.com")
                .build();

        userBusinessService.createUser(request1).await().atMost(Duration.ofSeconds(5));
        userBusinessService.createUser(request2).await().atMost(Duration.ofSeconds(5));

        // When
        UniAssertSubscriber<Long> subscriber = userBusinessService.getUserCount()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        Long count = subscriber.awaitItem().getItem();
        assertEquals(2L, count);
    }

    @Test
    void shouldGetZeroUserCountWhenEmpty() {
        // When
        UniAssertSubscriber<Long> subscriber = userBusinessService.getUserCount()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        Long count = subscriber.awaitItem().getItem();
        assertEquals(0L, count);
    }
}