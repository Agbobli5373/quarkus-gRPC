package org.isaac.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.isaac.exception.UserNotFoundException;
import org.isaac.grpc.user.UserProto.CreateUserRequest;
import org.isaac.grpc.user.UserProto.CreateUsersResponse;
import org.isaac.grpc.user.UserProto.UpdateUserRequest;
import org.isaac.grpc.user.UserProto.User;
import org.isaac.repository.UserRepository;
import org.isaac.validation.UserValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Business service layer for user operations.
 * <p>
 * This service orchestrates between repository, validator, and notification
 * services
 * to handle complex business workflows and manage error propagation across
 * service boundaries.
 * <p>
 * Learning objectives:
 * - Understand service layer architecture
 * - Learn about orchestrating multiple services
 * - Understand transaction-like operations in reactive programming
 */
@ApplicationScoped
public class UserBusinessService {

    private static final Logger logger = Logger.getLogger(UserBusinessService.class.getName());


    private final UserRepository userRepository;

    private final  UserValidator userValidator;

    private final  NotificationService notificationService;
    @Inject
    public UserBusinessService(
            UserRepository userRepository,
            UserValidator userValidator,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.userValidator = userValidator;
        this.notificationService = notificationService;
    }

    /**
     * Creates a new user with validation and notification.
     * <p>
     * This method demonstrates service orchestration by:
     * 1. Validating the request using the validator
     * 2. Building and saving the user via repository
     * 3. Broadcasting creation notification
     * 
     * @param request the create user request
     * @return Uni<User> containing the created user
     */
    public Uni<User> createUser(CreateUserRequest request) {
        logger.info("Creating user with name: " + request.getName());

        return userValidator.validateCreateRequest(request)
                .chain(() -> {
                    User user = buildUser(request);
                    return userRepository.save(user);
                })
                .invoke(user -> {
                    logger.info("User created successfully: " + user.getId());
                    notificationService.broadcastUserCreated(user);
                })
                .onFailure().invoke(throwable -> logger.warning("Failed to create user: " + throwable.getMessage()));
    }

    /**
     * Retrieves a user by ID.
     * 
     * @param id the user ID to retrieve
     * @return Uni<User> containing the user if found
     * @throws UserNotFoundException if user is not found
     */
    public Uni<User> getUser(String id) {
        logger.info("Retrieving user with ID: " + id);

        return userRepository.findById(id)
                .onItem().ifNull().failWith(() -> new UserNotFoundException(id))
                .invoke(user -> logger.info("User retrieved successfully: " + user.getId()))
                .onFailure()
                .invoke(throwable -> logger.warning("Failed to retrieve user " + id + ": " + throwable.getMessage()));
    }

    /**
     * Updates an existing user with validation and notification.
     * <p>
     * This method demonstrates error handling and service orchestration by:
     * 1. Validating the update request
     * 2. Updating the user via repository
     * 3. Broadcasting update notification
     * 
     * @param request the update user request
     * @return Uni<User> containing the updated user
     * @throws UserNotFoundException if user is not found
     */
    public Uni<User> updateUser(UpdateUserRequest request) {
        logger.info("Updating user with ID: " + request.getId());

        return userValidator.validateUpdateRequest(request)
                .chain(() -> {
                    User updatedUser = User.newBuilder()
                            .setId(request.getId())
                            .setName(request.getName())
                            .setEmail(request.getEmail())
                            .build();
                    return userRepository.update(request.getId(), updatedUser);
                })
                .onItem().ifNull().failWith(() -> new UserNotFoundException(request.getId()))
                .invoke(user -> {
                    logger.info("User updated successfully: " + user.getId());
                    notificationService.broadcastUserUpdated(user);
                })
                .onFailure().invoke(throwable -> logger
                        .warning("Failed to update user " + request.getId() + ": " + throwable.getMessage()));
    }

    /**
     * Deletes a user by ID with notification.
     * 
     * @param id the user ID to delete
     * @return Uni<Void> indicating completion
     * @throws UserNotFoundException if user is not found
     */
    public Uni<Void> deleteUser(String id) {
        logger.info("Deleting user with ID: " + id);

        return userRepository.delete(id)
                .chain(deleted -> {
                    if (!deleted) {
                        return Uni.createFrom().failure(new UserNotFoundException(id));
                    }
                    return Uni.createFrom().voidItem();
                })
                .invoke(() -> {
                    logger.info("User deleted successfully: " + id);
                    notificationService.broadcastUserDeleted(id);
                })
                .onFailure()
                .invoke(throwable -> logger.warning("Failed to delete user " + id + ": " + throwable.getMessage()));
    }

    /**
     * Retrieves all users from the repository.
     * 
     * @return Multi<User> streaming all users
     */
    public Multi<User> getAllUsers() {
        logger.info("Retrieving all users");

        return userRepository.findAll()
                .invoke(user -> logger.fine("Streaming user: " + user.getId()))
                .onFailure().invoke(throwable -> logger.warning("Failed to retrieve users: " + throwable.getMessage()));
    }

    /**
     * Creates multiple users from a stream of requests.
     * <p>
     * This method demonstrates batch processing with error collection:
     * 1. Processes each request individually
     * 2. Collects both successes and failures
     * 3. Returns a summary response with counts and errors
     * 
     * @param requests Multi stream of create user requests
     * @return Uni<CreateUsersResponse> containing summary of the batch operation
     */
    public Uni<CreateUsersResponse> createMultipleUsers(Multi<CreateUserRequest> requests) {
        logger.info("Starting batch user creation");

        List<String> createdUserIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        return requests
                .onItem().transformToUniAndConcatenate(request -> createUser(request)
                        .onItem().invoke(user -> {
                            synchronized (createdUserIds) {
                                createdUserIds.add(user.getId());
                            }
                        })
                        .onFailure().recoverWithItem(throwable -> {
                            synchronized (errors) {
                                errors.add(
                                        "Failed to create user '" + request.getName() + "': " + throwable.getMessage());
                            }
                            return null; // Continue processing other requests
                        }))
                .collect().asList()
                .onItem().transform(results -> {
                    CreateUsersResponse response = CreateUsersResponse.newBuilder()
                            .setCreatedCount(createdUserIds.size())
                            .addAllErrors(errors)
                            .addAllCreatedUserIds(createdUserIds)
                            .build();

                    logger.info(String.format("Batch creation completed: %d created, %d errors",
                            createdUserIds.size(), errors.size()));

                    return response;
                })
                .onFailure()
                .invoke(throwable -> logger.warning("Batch user creation failed: " + throwable.getMessage()));
    }

    /**
     * Gets the total count of users in the system.
     * 
     * @return Uni<Long> containing the user count
     */
    public Uni<Long> getUserCount() {
        return userRepository.count()
                .invoke(count -> logger.info("Total user count: " + count))
                .onFailure().invoke(throwable -> logger.warning("Failed to get user count: " + throwable.getMessage()));
    }

    /**
     * Builds a User proto message from a CreateUserRequest.
     * 
     * @param request the create user request
     * @return User proto message ready for persistence
     */
    private User buildUser(CreateUserRequest request) {
        long currentTime = System.currentTimeMillis();

        return User.newBuilder()
                .setName(request.getName())
                .setEmail(request.getEmail())
                .setCreatedAt(currentTime)
                .setUpdatedAt(currentTime)
                .build();
    }
}