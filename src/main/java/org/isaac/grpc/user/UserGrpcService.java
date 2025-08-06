package org.isaac.grpc.user;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.isaac.exception.UserNotFoundException;
import org.isaac.exception.ValidationException;
import org.isaac.grpc.user.UserProto.*;
import org.isaac.service.UserBusinessService;

import java.util.logging.Logger;

/**
 * gRPC service implementation for user management operations.
 * 
 * This service demonstrates unary RPC patterns and proper error handling
 * with gRPC status codes. It integrates with the business service layer
 * and handles all CRUD operations for users.
 * 
 * Learning objectives:
 * - Understand gRPC service implementation in Quarkus
 * - Learn about unary RPC patterns (request-response)
 * - Understand gRPC error handling with status codes
 */
@GrpcService
public class UserGrpcService implements UserService {

    private static final Logger logger = Logger.getLogger(UserGrpcService.class.getName());

    @Inject
    UserBusinessService userBusinessService;

    /**
     * Creates a new user.
     * 
     * Unary RPC: Client sends single CreateUserRequest, server returns single User.
     * This is the most common gRPC pattern, similar to HTTP request/response.
     * 
     * @param request CreateUserRequest containing user details
     * @return Uni<User> containing the created user
     */
    @Override
    public Uni<User> createUser(CreateUserRequest request) {
        logger.info("gRPC createUser called for: " + request.getName());

        return userBusinessService.createUser(request)
                .onItem().invoke(user -> logger.info("gRPC createUser completed for user: " + user.getId()))
                .onFailure().transform(this::mapToGrpcException);
    }

    /**
     * Retrieves a user by ID.
     * 
     * Unary RPC: Client sends single GetUserRequest, server returns single User.
     * 
     * @param request GetUserRequest containing user ID
     * @return Uni<User> containing the requested user
     */
    @Override
    public Uni<User> getUser(GetUserRequest request) {
        logger.info("gRPC getUser called for ID: " + request.getId());

        return userBusinessService.getUser(request.getId())
                .onItem().invoke(user -> logger.info("gRPC getUser completed for user: " + user.getId()))
                .onFailure().transform(this::mapToGrpcException);
    }

    /**
     * Updates an existing user.
     * 
     * Unary RPC: Client sends single UpdateUserRequest, server returns single User.
     * 
     * @param request UpdateUserRequest containing updated user details
     * @return Uni<User> containing the updated user
     */
    @Override
    public Uni<User> updateUser(UpdateUserRequest request) {
        logger.info("gRPC updateUser called for ID: " + request.getId());

        return userBusinessService.updateUser(request)
                .onItem().invoke(user -> logger.info("gRPC updateUser completed for user: " + user.getId()))
                .onFailure().transform(this::mapToGrpcException);
    }

    /**
     * Deletes a user by ID.
     * 
     * Unary RPC: Client sends single DeleteUserRequest, server returns single
     * DeleteUserResponse.
     * 
     * @param request DeleteUserRequest containing user ID to delete
     * @return Uni<DeleteUserResponse> containing deletion confirmation
     */
    @Override
    public Uni<DeleteUserResponse> deleteUser(DeleteUserRequest request) {
        logger.info("gRPC deleteUser called for ID: " + request.getId());

        return userBusinessService.deleteUser(request.getId())
                .onItem().transform(ignored -> {
                    logger.info("gRPC deleteUser completed for ID: " + request.getId());
                    return DeleteUserResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("User deleted successfully")
                            .build();
                })
                .onFailure().transform(this::mapToGrpcException);
    }

    // Note: The following methods are for streaming operations and will be
    // implemented in later tasks

    @Override
    public io.smallrye.mutiny.Multi<User> listUsers(Empty request) {
        // This will be implemented in task 9 (server streaming)
        throw new UnsupportedOperationException("Server streaming not yet implemented");
    }

    @Override
    public Uni<CreateUsersResponse> createUsers(io.smallrye.mutiny.Multi<CreateUserRequest> request) {
        // This will be implemented in task 10 (client streaming)
        throw new UnsupportedOperationException("Client streaming not yet implemented");
    }

    @Override
    public io.smallrye.mutiny.Multi<UserNotification> subscribeToUserUpdates(
            io.smallrye.mutiny.Multi<SubscribeRequest> request) {
        // This will be implemented in task 11 (bidirectional streaming)
        throw new UnsupportedOperationException("Bidirectional streaming not yet implemented");
    }

    /**
     * Maps business exceptions to appropriate gRPC status exceptions.
     * 
     * This method demonstrates proper error handling in gRPC services by:
     * - Converting business exceptions to gRPC status codes
     * - Providing appropriate error messages
     * - Following gRPC error handling best practices
     * 
     * @param throwable the business exception to map
     * @return RuntimeException with appropriate gRPC status
     */
    private RuntimeException mapToGrpcException(Throwable throwable) {
        logger.warning("Mapping exception to gRPC status: " + throwable.getClass().getSimpleName() + " - "
                + throwable.getMessage());

        if (throwable instanceof UserNotFoundException) {
            return io.grpc.Status.NOT_FOUND
                    .withDescription(throwable.getMessage())
                    .asRuntimeException();
        }

        if (throwable instanceof ValidationException) {
            return io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(throwable.getMessage())
                    .asRuntimeException();
        }

        // For any other exception, return INTERNAL error
        return io.grpc.Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException();
    }
}