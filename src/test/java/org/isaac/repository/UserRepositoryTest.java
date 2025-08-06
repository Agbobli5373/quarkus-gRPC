package org.isaac.repository;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.isaac.grpc.user.UserProto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserRepository.
 * Tests all CRUD operations, error handling, and edge cases.
 * 
 * Learning objectives:
 * - Understand testing reactive code with Mutiny test utilities
 * - Learn about testing concurrent operations
 * - Understand proper test setup and cleanup
 */
@QuarkusTest
class UserRepositoryTest {

        @Inject
        UserRepository userRepository;

        @BeforeEach
        void setUp() {
                // Clear repository before each test to ensure clean state
                userRepository.clear().await().atMost(Duration.ofSeconds(1));
        }

        @Test
        void shouldSaveUserWithGeneratedId() {
                // Given
                User user = User.newBuilder()
                                .setName("John Doe")
                                .setEmail("john@example.com")
                                .build();

                // When
                UniAssertSubscriber<User> subscriber = userRepository.save(user)
                                .subscribe().withSubscriber(UniAssertSubscriber.create());

                // Then
                User savedUser = subscriber.awaitItem().getItem();

                assertNotNull(savedUser.getId());
                assertFalse(savedUser.getId().isEmpty());
                assertEquals("John Doe", savedUser.getName());
                assertEquals("john@example.com", savedUser.getEmail());
                assertTrue(savedUser.getCreatedAt() > 0);
                assertTrue(savedUser.getUpdatedAt() > 0);
                assertEquals(savedUser.getCreatedAt(), savedUser.getUpdatedAt());
        }

        @Test
        void shouldSaveUserWithExistingId() {
                // Given
                User user = User.newBuilder()
                                .setId("existing-id")
                                .setName("Jane Doe")
                                .setEmail("jane@example.com")
                                .build();

                // When
                User savedUser = userRepository.save(user)
                                .await().atMost(Duration.ofSeconds(1));

                // Then
                assertEquals("existing-id", savedUser.getId());
                assertEquals("Jane Doe", savedUser.getName());
                assertEquals("jane@example.com", savedUser.getEmail());
                assertTrue(savedUser.getCreatedAt() > 0);
                assertTrue(savedUser.getUpdatedAt() > 0);
        }

        @Test
        void shouldUpdateExistingUserPreservingCreatedAt() {
                // Given - save initial user
                User initialUser = User.newBuilder()
                                .setName("Initial Name")
                                .setEmail("initial@example.com")
                                .build();

                User savedUser = userRepository.save(initialUser)
                                .await().atMost(Duration.ofSeconds(1));

                // Small delay to ensure different timestamps
                try {
                        Thread.sleep(10);
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                }

                // When - save user with same ID (update)
                User updatedUser = User.newBuilder()
                                .setId(savedUser.getId())
                                .setName("Updated Name")
                                .setEmail("updated@example.com")
                                .setCreatedAt(999999) // This should be ignored
                                .build();

                User result = userRepository.save(updatedUser)
                                .await().atMost(Duration.ofSeconds(1));

                // Then
                assertEquals(savedUser.getId(), result.getId());
                assertEquals("Updated Name", result.getName());
                assertEquals("updated@example.com", result.getEmail());
                assertEquals(savedUser.getCreatedAt(), result.getCreatedAt()); // Preserved
                assertTrue(result.getUpdatedAt() > result.getCreatedAt()); // Updated
        }

        @Test
        void shouldFindUserById() {
                // Given
                User user = User.newBuilder()
                                .setName("Find Me")
                                .setEmail("findme@example.com")
                                .build();

                User savedUser = userRepository.save(user)
                                .await().atMost(Duration.ofSeconds(1));

                // When
                User foundUser = userRepository.findById(savedUser.getId())
                                .await().atMost(Duration.ofSeconds(1));

                // Then
                assertNotNull(foundUser);
                assertEquals(savedUser.getId(), foundUser.getId());
                assertEquals("Find Me", foundUser.getName());
                assertEquals("findme@example.com", foundUser.getEmail());
        }

        @Test
        void shouldReturnNullWhenUserNotFound() {
                // When
                User foundUser = userRepository.findById("non-existent-id")
                                .await().atMost(Duration.ofSeconds(1));

                // Then
                assertNull(foundUser);
        }

        @Test
        void shouldReturnNullForNullOrEmptyId() {
                // When & Then
                assertNull(userRepository.findById(null).await().atMost(Duration.ofSeconds(1)));
                assertNull(userRepository.findById("").await().atMost(Duration.ofSeconds(1)));
                assertNull(userRepository.findById("   ").await().atMost(Duration.ofSeconds(1)));
        }

        @Test
        void shouldFindAllUsers() {
                // Given
                User user1 = User.newBuilder().setName("User 1").setEmail("user1@example.com").build();
                User user2 = User.newBuilder().setName("User 2").setEmail("user2@example.com").build();
                User user3 = User.newBuilder().setName("User 3").setEmail("user3@example.com").build();

                userRepository.save(user1).await().atMost(Duration.ofSeconds(1));
                userRepository.save(user2).await().atMost(Duration.ofSeconds(1));
                userRepository.save(user3).await().atMost(Duration.ofSeconds(1));

                // When
                AssertSubscriber<User> subscriber = userRepository.findAll()
                                .subscribe().withSubscriber(AssertSubscriber.create(3));

                // Then
                List<User> users = subscriber.awaitCompletion().getItems();
                assertEquals(3, users.size());

                // Verify all users are present (order may vary)
                assertTrue(users.stream().anyMatch(u -> "User 1".equals(u.getName())));
                assertTrue(users.stream().anyMatch(u -> "User 2".equals(u.getName())));
                assertTrue(users.stream().anyMatch(u -> "User 3".equals(u.getName())));
        }

        @Test
        void shouldReturnEmptyStreamWhenNoUsers() {
                // When
                AssertSubscriber<User> subscriber = userRepository.findAll()
                                .subscribe().withSubscriber(AssertSubscriber.create(0));

                // Then
                List<User> users = subscriber.awaitCompletion().getItems();
                assertTrue(users.isEmpty());
        }

        @Test
        void shouldUpdateExistingUser() {
                // Given
                User user = User.newBuilder()
                                .setName("Original Name")
                                .setEmail("original@example.com")
                                .build();

                User savedUser = userRepository.save(user)
                                .await().atMost(Duration.ofSeconds(1));

                // Small delay to ensure different timestamps
                try {
                        Thread.sleep(10);
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                }

                // When
                User updatedUser = User.newBuilder()
                                .setName("Updated Name")
                                .setEmail("updated@example.com")
                                .build();

                User result = userRepository.update(savedUser.getId(), updatedUser)
                                .await().atMost(Duration.ofSeconds(1));

                // Then
                assertNotNull(result);
                assertEquals(savedUser.getId(), result.getId());
                assertEquals("Updated Name", result.getName());
                assertEquals("updated@example.com", result.getEmail());
                assertEquals(savedUser.getCreatedAt(), result.getCreatedAt()); // Preserved
                assertTrue(result.getUpdatedAt() > result.getCreatedAt()); // Updated
        }

        @Test
        void shouldReturnNullWhenUpdatingNonExistentUser() {
                // Given
                User updatedUser = User.newBuilder()
                                .setName("Updated Name")
                                .setEmail("updated@example.com")
                                .build();

                // When
                User result = userRepository.update("non-existent-id", updatedUser)
                                .await().atMost(Duration.ofSeconds(1));

                // Then
                assertNull(result);
        }

        @Test
        void shouldReturnNullWhenUpdatingWithNullOrEmptyId() {
                // Given
                User updatedUser = User.newBuilder()
                                .setName("Updated Name")
                                .setEmail("updated@example.com")
                                .build();

                // When & Then
                assertNull(userRepository.update(null, updatedUser).await().atMost(Duration.ofSeconds(1)));
                assertNull(userRepository.update("", updatedUser).await().atMost(Duration.ofSeconds(1)));
                assertNull(userRepository.update("   ", updatedUser).await().atMost(Duration.ofSeconds(1)));
        }

        @Test
        void shouldDeleteExistingUser() {
                // Given
                User user = User.newBuilder()
                                .setName("Delete Me")
                                .setEmail("deleteme@example.com")
                                .build();

                User savedUser = userRepository.save(user)
                                .await().atMost(Duration.ofSeconds(1));

                // When
                Boolean deleted = userRepository.delete(savedUser.getId())
                                .await().atMost(Duration.ofSeconds(1));

                // Then
                assertTrue(deleted);

                // Verify user is actually deleted
                User foundUser = userRepository.findById(savedUser.getId())
                                .await().atMost(Duration.ofSeconds(1));
                assertNull(foundUser);
        }

        @Test
        void shouldReturnFalseWhenDeletingNonExistentUser() {
                // When
                Boolean deleted = userRepository.delete("non-existent-id")
                                .await().atMost(Duration.ofSeconds(1));

                // Then
                assertFalse(deleted);
        }

        @Test
        void shouldReturnFalseWhenDeletingWithNullOrEmptyId() {
                // When & Then
                assertFalse(userRepository.delete(null).await().atMost(Duration.ofSeconds(1)));
                assertFalse(userRepository.delete("").await().atMost(Duration.ofSeconds(1)));
                assertFalse(userRepository.delete("   ").await().atMost(Duration.ofSeconds(1)));
        }

        @Test
        void shouldCheckIfEmailExists() {
                // Given
                User user = User.newBuilder()
                                .setName("Test User")
                                .setEmail("test@example.com")
                                .build();

                userRepository.save(user).await().atMost(Duration.ofSeconds(1));

                // When & Then
                assertTrue(userRepository.existsByEmail("test@example.com")
                                .await().atMost(Duration.ofSeconds(1)));
                assertTrue(userRepository.existsByEmail("TEST@EXAMPLE.COM") // Case insensitive
                                .await().atMost(Duration.ofSeconds(1)));
                assertFalse(userRepository.existsByEmail("nonexistent@example.com")
                                .await().atMost(Duration.ofSeconds(1)));
        }

        @Test
        void shouldReturnFalseForNullOrEmptyEmailExists() {
                // When & Then
                assertFalse(userRepository.existsByEmail(null).await().atMost(Duration.ofSeconds(1)));
                assertFalse(userRepository.existsByEmail("").await().atMost(Duration.ofSeconds(1)));
                assertFalse(userRepository.existsByEmail("   ").await().atMost(Duration.ofSeconds(1)));
        }

        @Test
        void shouldCheckEmailExistsExcludingId() {
                // Given
                User user1 = User.newBuilder().setName("User 1").setEmail("user1@example.com").build();
                User user2 = User.newBuilder().setName("User 2").setEmail("user2@example.com").build();

                User savedUser1 = userRepository.save(user1).await().atMost(Duration.ofSeconds(1));
                userRepository.save(user2).await().atMost(Duration.ofSeconds(1));

                // When & Then
                // Should return false when checking user's own email
                assertFalse(userRepository.existsByEmailExcludingId("user1@example.com", savedUser1.getId())
                                .await().atMost(Duration.ofSeconds(1)));

                // Should return true when checking another user's email
                assertTrue(userRepository.existsByEmailExcludingId("user2@example.com", savedUser1.getId())
                                .await().atMost(Duration.ofSeconds(1)));

                // Should return false for non-existent email
                assertFalse(userRepository.existsByEmailExcludingId("nonexistent@example.com", savedUser1.getId())
                                .await().atMost(Duration.ofSeconds(1)));
        }

        @Test
        void shouldCountUsers() {
                // Given - initially empty
                Long initialCount = userRepository.count().await().atMost(Duration.ofSeconds(1));
                assertEquals(0L, initialCount);

                // When - add users
                User user1 = User.newBuilder().setName("User 1").setEmail("user1@example.com").build();
                User user2 = User.newBuilder().setName("User 2").setEmail("user2@example.com").build();

                userRepository.save(user1).await().atMost(Duration.ofSeconds(1));
                userRepository.save(user2).await().atMost(Duration.ofSeconds(1));

                // Then
                Long finalCount = userRepository.count().await().atMost(Duration.ofSeconds(1));
                assertEquals(2L, finalCount);
        }

        @Test
        void shouldClearAllUsers() {
                // Given
                User user1 = User.newBuilder().setName("User 1").setEmail("user1@example.com").build();
                User user2 = User.newBuilder().setName("User 2").setEmail("user2@example.com").build();

                userRepository.save(user1).await().atMost(Duration.ofSeconds(1));
                userRepository.save(user2).await().atMost(Duration.ofSeconds(1));

                // Verify users exist
                assertEquals(2L, userRepository.count().await().atMost(Duration.ofSeconds(1)));

                // When
                userRepository.clear().await().atMost(Duration.ofSeconds(1));

                // Then
                assertEquals(0L, userRepository.count().await().atMost(Duration.ofSeconds(1)));

                AssertSubscriber<User> subscriber = userRepository.findAll()
                                .subscribe().withSubscriber(AssertSubscriber.create(0));
                assertTrue(subscriber.awaitCompletion().getItems().isEmpty());
        }

        @Test
        void shouldHandleConcurrentOperations() {
                // This test demonstrates thread safety of ConcurrentHashMap
                // Given - create a user with a fixed ID to test concurrent updates
                User user = User.newBuilder()
                                .setId("concurrent-user-id")
                                .setName("Concurrent User")
                                .setEmail("concurrent@example.com")
                                .build();

                // When - perform concurrent saves with the same ID
                Uni<User> save1 = userRepository.save(user);
                Uni<User> save2 = userRepository.save(user.toBuilder().setName("Updated Name 1").build());
                Uni<User> save3 = userRepository.save(user.toBuilder().setName("Updated Name 2").build());

                // Combine all operations
                Uni<List<User>> combinedSaves = Uni.combine().all().unis(save1, save2, save3)
                                .asTuple()
                                .onItem()
                                .transform(tuple -> List.of(tuple.getItem1(), tuple.getItem2(), tuple.getItem3()));

                // Then
                List<User> results = combinedSaves.await().atMost(Duration.ofSeconds(5));
                assertEquals(3, results.size());

                // All should have the same ID since we're using the same ID
                String expectedId = "concurrent-user-id";
                assertTrue(results.stream().allMatch(u -> u.getId().equals(expectedId)));

                // Verify that only one user exists in the repository (concurrent updates to
                // same ID)
                Long count = userRepository.count().await().atMost(Duration.ofSeconds(1));
                assertEquals(1L, count);
        }
}