package org.isaac.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.isaac.grpc.user.UserProto.User;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Repository for managing User entities using in-memory storage.
 * This implementation uses ConcurrentHashMap for thread-safe operations
 * and returns Mutiny reactive types (Uni/Multi) for non-blocking operations.
 * 
 * Learning objectives:
 * - Understand reactive programming with Mutiny
 * - Learn about concurrent data structures for thread safety
 * - Understand repository pattern for data access abstraction
 */
@ApplicationScoped
public class UserRepository {

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    /**
     * Saves a user to the repository.
     * If the user doesn't have an ID, generates a new UUID.
     * If the user already exists, updates the existing entry.
     * 
     * @param user the user to save
     * @return Uni<User> containing the saved user with generated ID and timestamps
     */
    public Uni<User> save(User user) {
        return Uni.createFrom().item(() -> {
            // Generate ID if not present
            String id = user.getId().isEmpty() ? UUID.randomUUID().toString() : user.getId();
            long currentTime = System.currentTimeMillis();

            // Check if this is an update (user already exists)
            User existingUser = users.get(id);

            User.Builder userBuilder = user.toBuilder()
                    .setId(id)
                    .setUpdatedAt(currentTime);

            if (existingUser != null) {
                // This is an update - preserve the original created_at
                userBuilder.setCreatedAt(existingUser.getCreatedAt());
            } else {
                // This is a new user - set created_at only if not already set
                if (user.getCreatedAt() == 0) {
                    userBuilder.setCreatedAt(currentTime);
                }
            }

            User savedUser = userBuilder.build();
            users.put(id, savedUser);
            return savedUser;
        });
    }

    /**
     * Finds a user by ID.
     * 
     * @param id the user ID to search for
     * @return Uni<User> containing the user if found, null otherwise
     */
    public Uni<User> findById(String id) {
        return Uni.createFrom().item(() -> {
            if (id == null || id.trim().isEmpty()) {
                return null;
            }
            return users.get(id);
        });
    }

    /**
     * Retrieves all users from the repository.
     * 
     * @return Multi<User> streaming all users in the repository
     */
    public Multi<User> findAll() {
        return Multi.createFrom().iterable(users.values());
    }

    /**
     * Updates an existing user in the repository.
     * 
     * @param id          the ID of the user to update
     * @param updatedUser the updated user data
     * @return Uni<User> containing the updated user, or null if user not found
     */
    public Uni<User> update(String id, User updatedUser) {
        return Uni.createFrom().item(() -> {
            if (id == null || id.trim().isEmpty()) {
                return null;
            }

            User existingUser = users.get(id);
            if (existingUser == null) {
                return null;
            }

            // Preserve original ID, created_at, and update the updated_at timestamp
            User updated = updatedUser.toBuilder()
                    .setId(id)
                    .setCreatedAt(existingUser.getCreatedAt())
                    .setUpdatedAt(System.currentTimeMillis())
                    .build();

            users.put(id, updated);
            return updated;
        });
    }

    /**
     * Deletes a user from the repository.
     * 
     * @param id the ID of the user to delete
     * @return Uni<Boolean> true if user was deleted, false if user not found
     */
    public Uni<Boolean> delete(String id) {
        return Uni.createFrom().item(() -> {
            if (id == null || id.trim().isEmpty()) {
                return false;
            }

            User removedUser = users.remove(id);
            return removedUser != null;
        });
    }

    /**
     * Checks if a user with the given email already exists.
     * Useful for validation purposes.
     * 
     * @param email the email to check
     * @return Uni<Boolean> true if email exists, false otherwise
     */
    public Uni<Boolean> existsByEmail(String email) {
        return Uni.createFrom().item(() -> {
            if (email == null || email.trim().isEmpty()) {
                return false;
            }

            return users.values().stream()
                    .anyMatch(user -> email.equalsIgnoreCase(user.getEmail()));
        });
    }

    /**
     * Checks if a user with the given email exists, excluding a specific user ID.
     * Useful for update validation to allow users to keep their own email.
     * 
     * @param email     the email to check
     * @param excludeId the user ID to exclude from the check
     * @return Uni<Boolean> true if email exists for another user, false otherwise
     */
    public Uni<Boolean> existsByEmailExcludingId(String email, String excludeId) {
        return Uni.createFrom().item(() -> {
            if (email == null || email.trim().isEmpty()) {
                return false;
            }

            return users.values().stream()
                    .anyMatch(user -> email.equalsIgnoreCase(user.getEmail())
                            && !user.getId().equals(excludeId));
        });
    }

    /**
     * Gets the total count of users in the repository.
     * 
     * @return Uni<Long> containing the count of users
     */
    public Uni<Long> count() {
        return Uni.createFrom().item(() -> (long) users.size());
    }

    /**
     * Clears all users from the repository.
     * Useful for testing purposes.
     * 
     * @return Uni<Void> indicating completion
     */
    public Uni<Void> clear() {
        return Uni.createFrom().item(() -> {
            users.clear();
            return null;
        });
    }
}