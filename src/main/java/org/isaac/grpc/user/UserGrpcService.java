package org.isaac.grpc.user;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.isaac.exception.GrpcExceptionHandler;
import org.isaac.exception.UserNotFoundException;
import org.isaac.exception.ValidationException;
import org.isaac.grpc.user.UserProto.*;
import org.isaac.service.NotificationService;
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

    @Inject
    NotificationService notificationService;

    @Inject
    GrpcExceptionHandler exceptionHandler;

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
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "createUser"));
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
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "getUser"));
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
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "updateUser"));
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
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "deleteUser"));
    }

    // Note: The following methods are for streaming operations and will be
    // implemented in later tasks

    /**
     * Lists all users using server streaming.
     * 
     * Server Streaming RPC: Client sends single Empty request, server returns
     * stream of User responses.
     * This pattern is useful for large datasets or real-time updates where the
     * server needs to
     * send multiple responses for a single client request.
     * 
     * Key concepts demonstrated:
     * - Multi<T> for streaming responses
     * - Proper error handling in streams
     * - Stream lifecycle management
     * - Backpressure handling (handled automatically by Mutiny)
     * 
     * @param request Empty request (no parameters needed)
     * @return Multi<User> streaming all users individually
     */
    @Override
    public io.smallrye.mutiny.Multi<User> listUsers(Empty request) {
        logger.info("gRPC listUsers called - starting server streaming");

        return userBusinessService.getAllUsers()
                .onItem().invoke(user -> logger.fine("Streaming user: " + user.getId() + " (" + user.getName() + ")"))
                .onCompletion().invoke(() -> logger.info("gRPC listUsers streaming completed"))
                .onFailure().invoke(error -> logger.severe("Streaming error in listUsers: " + error.getMessage()))
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "listUsers"));
    }

    /**
     * Creates multiple users using client streaming.
     * 
     * Client Streaming RPC: Client sends stream of CreateUserRequest, server
     * returns single CreateUsersResponse.
     * This pattern is useful for bulk operations where the client has multiple
     * items to process
     * and wants a summary response after all items are processed.
     * 
     * Key concepts demonstrated:
     * - Multi<T> for receiving streaming requests
     * - Batch processing with error collection
     * - Graceful handling of partial failures
     * - Summary response generation
     * 
     * The implementation processes each request individually, collecting both
     * successes and failures,
     * then returns a comprehensive summary to the client.
     * 
     * @param requests Multi stream of CreateUserRequest from client
     * @return Uni<CreateUsersResponse> containing batch operation summary
     */
    @Override
    public Uni<CreateUsersResponse> createUsers(io.smallrye.mutiny.Multi<CreateUserRequest> requests) {
        logger.info("gRPC createUsers called - starting client streaming");

        return userBusinessService.createMultipleUsers(requests)
                .invoke(response -> logger.info(String.format(
                        "gRPC createUsers completed - created: %d, errors: %d",
                        response.getCreatedCount(),
                        response.getErrorsCount())))
                .onFailure()
                .invoke(error -> logger.severe("Client streaming error in createUsers: " + error.getMessage()))
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "createUsers"));
    }

    /**
     * Subscribes to real-time user updates using bidirectional streaming.
     * 
     * Bidirectional Streaming RPC: Client sends stream of SubscribeRequest, server
     * returns stream of UserNotification.
     * This is the most complex gRPC pattern, enabling real-time, full-duplex
     * communication.
     * 
     * Key concepts demonstrated:
     * - Bidirectional streaming with Multi<T> for both input and output
     * - Real-time subscription management
     * - Client lifecycle handling (connection/disconnection)
     * - Resource cleanup on stream cancellation
     * - Integration with notification service for broadcasting
     * 
     * The implementation:
     * 1. Processes incoming subscription requests from clients
     * 2. Manages client subscriptions through the notification service
     * 3. Returns a stream of notifications for subscribed clients
     * 4. Handles client disconnections and cleanup automatically
     * 
     * @param requests Multi stream of SubscribeRequest from clients
     * @return Multi<UserNotification> stream of notifications to clients
     */
    @Override
    public io.smallrye.mutiny.Multi<UserNotification> subscribeToUserUpdates(
            io.smallrye.mutiny.Multi<SubscribeRequest> requests) {
        logger.info("gRPC subscribeToUserUpdates called - starting bidirectional streaming");

        return requests
                .onItem().transformToMultiAndConcatenate(this::handleSubscriptionRequest)
                .onCancellation().invoke(() -> {
                    logger.info("gRPC subscribeToUserUpdates - client disconnected, cleaning up");
                })
                .onFailure().invoke(error -> {
                    logger.severe("Bidirectional streaming error in subscribeToUserUpdates: " + error.getMessage());
                })
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "subscribeToUserUpdates"));
    }

    /**
     * Handles individual subscription requests from clients.
     * 
     * This method processes each SubscribeRequest and establishes a subscription
     * with the notification service. It demonstrates:
     * - Client ID management (generate if not provided)
     * - Subscription lifecycle management
     * - Integration with notification service
     * - Proper logging for debugging and learning
     * 
     * @param subscribeRequest the subscription request from a client
     * @return Multi<UserNotification> stream of notifications for this client
     */
    private io.smallrye.mutiny.Multi<UserNotification> handleSubscriptionRequest(SubscribeRequest subscribeRequest) {
        String clientId = subscribeRequest.getClientId();

        // Generate client ID if not provided
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = notificationService.generateClientId();
            logger.info("Generated client ID for subscription: " + clientId);
        } else {
            logger.info("Processing subscription request for client: " + clientId);
        }

        // Log the notification types the client is interested in
        if (subscribeRequest.getNotificationTypesCount() > 0) {
            logger.info("Client " + clientId + " subscribing to notification types: " +
                    subscribeRequest.getNotificationTypesList());
        } else {
            logger.info("Client " + clientId + " subscribing to all notification types");
        }

        // Subscribe the client to notifications
        final String finalClientId = clientId;
        return notificationService.subscribe(finalClientId)
                .onItem().invoke(notification -> {
                    logger.fine("Sending notification to client " + finalClientId + ": " +
                            notification.getType() + " for user " + notification.getUser().getId());
                })
                .onCompletion().invoke(() -> {
                    logger.info("Subscription completed for client: " + finalClientId);
                })
                .onCancellation().invoke(() -> {
                    logger.info("Subscription cancelled for client: " + finalClientId);
                    notificationService.unsubscribe(finalClientId);
                })
                .onFailure().invoke(error -> {
                    logger.warning("Subscription error for client " + finalClientId + ": " + error.getMessage());
                    notificationService.unsubscribe(finalClientId);
                });
    }

}