package org.isaac.grpc.user;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.isaac.exception.GrpcExceptionHandler;
import org.isaac.grpc.user.UserProto.*;
import org.isaac.service.NotificationService;
import org.isaac.service.UserBusinessService;

import java.util.logging.Logger;
import java.time.Instant;
import org.isaac.monitoring.LoggingUtils;

/**
 * gRPC service implementation for user management operations.
 * <p>
 * This service demonstrates unary RPC patterns and proper error handling
 * with gRPC status codes. It integrates with the business service layer
 * and handles all CRUD operations for users.
 * <p>
 * Learning objectives:
 * - Understand gRPC service implementation in Quarkus
 * - Learn about unary RPC patterns (request-response)
 * - Understand gRPC error handling with status codes
 */
@GrpcService
public class UserGrpcService implements UserService {

    private static final Logger logger = Logger.getLogger(UserGrpcService.class.getName());

    private final UserBusinessService userBusinessService;

    private final NotificationService notificationService;

    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public UserGrpcService(
            UserBusinessService userBusinessService,
            NotificationService notificationService,
            GrpcExceptionHandler exceptionHandler) {
        this.userBusinessService = userBusinessService;
        this.notificationService = notificationService;
        this.exceptionHandler = exceptionHandler;
        logger.info("UserGrpcService initialized" +
                " with UserBusinessService, NotificationService, and GrpcExceptionHandler");
    }

    /**
     * Creates a new user.
     * <p>
     * Unary RPC: Client sends single CreateUserRequest, server returns single User.
     * This is the most common gRPC pattern, similar to HTTP request/response.
     * 
     * @param request CreateUserRequest containing user details
     * @return Uni<User> containing the created user
     */
    @Override
    public Uni<User> createUser(CreateUserRequest request) {
        Instant startTime = Instant.now();
        LoggingUtils.logOperationStart(logger, "grpc.createUser",
                String.format("name=%s, email=%s", request.getName(), request.getEmail()));

        return userBusinessService.createUser(request)
                .onItem().invoke(user -> {
                    LoggingUtils.setUserId(user.getId());
                    LoggingUtils.logOperationEnd(logger, "grpc.createUser", startTime,
                            String.format("userId=%s", user.getId()));
                    LoggingUtils.logMetric(logger, "grpc.createUser.success", 1, "count");
                })
                .onFailure().invoke(throwable -> {
                    LoggingUtils.logOperationError(logger, "grpc.createUser", throwable,
                            String.format("name=%s", request.getName()));
                    LoggingUtils.logMetric(logger, "grpc.createUser.error", 1, "count");
                })
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "createUser"));
    }

    /**
     * Retrieves a user by ID.
     * <p>
     * Unary RPC: Client sends single GetUserRequest, server returns single User.
     * 
     * @param request GetUserRequest containing user ID
     * @return Uni<User> containing the requested user
     */
    @Override
    public Uni<User> getUser(GetUserRequest request) {
        Instant startTime = Instant.now();
        LoggingUtils.setUserId(request.getId());
        LoggingUtils.logOperationStart(logger, "grpc.getUser",
                String.format("userId=%s", request.getId()));

        return userBusinessService.getUser(request.getId())
                .onItem().invoke(user -> {
                    LoggingUtils.logOperationEnd(logger, "grpc.getUser", startTime,
                            String.format("userId=%s, found=true", user.getId()));
                    LoggingUtils.logMetric(logger, "grpc.getUser.success", 1, "count");
                })
                .onFailure().invoke(throwable -> {
                    LoggingUtils.logOperationError(logger, "grpc.getUser", throwable,
                            String.format("userId=%s", request.getId()));
                    LoggingUtils.logMetric(logger, "grpc.getUser.error", 1, "count");
                })
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "getUser"));
    }

    /**
     * Updates an existing user.
     * <p>
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
     * <p>
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
     * <p>
     * Server Streaming RPC: Client sends single Empty request, server returns
     * stream of User responses.
     * This pattern is useful for large datasets or real-time updates where the
     * server needs to
     * send multiple responses for a single client request.
     * <p>
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
    public Multi<User> listUsers(Empty request) {
        LoggingUtils.logStreamingEvent(logger, "started", "server", "listUsers operation");
        Instant startTime = Instant.now();
        final int[] userCount = { 0 };

        return userBusinessService.getAllUsers()
                .onItem().invoke(user -> {
                    userCount[0]++;
                    LoggingUtils.logStreamingEvent(logger, "item", "server",
                            String.format("streaming user %s (%s)", user.getId(), user.getName()));
                })
                .onCompletion().invoke(() -> {
                    LoggingUtils.logStreamingEvent(logger, "completed", "server",
                            String.format("streamed %d users", userCount[0]));
                    LoggingUtils.logMetric(logger, "grpc.listUsers.streamed_count", userCount[0], "count");
                    LoggingUtils.logMetric(logger, "grpc.listUsers.duration",
                            java.time.Duration.between(startTime, Instant.now()).toMillis(), "ms");
                })
                .onFailure().invoke(error -> {
                    LoggingUtils.logStreamingEvent(logger, "error", "server",
                            String.format("listUsers failed: %s", error.getMessage()));
                    LoggingUtils.logMetric(logger, "grpc.listUsers.error", 1, "count");
                })
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "listUsers"));
    }

    /**
     * Creates multiple users using client streaming.
     * <p>
     * Client Streaming RPC: Client sends stream of CreateUserRequest, server
     * returns single CreateUsersResponse.
     * This pattern is useful for bulk operations where the client has multiple
     * items to process
     * and wants a summary response after all items are processed.
     * <p>
     * Key concepts demonstrated:
     * - Multi<T> for receiving streaming requests
     * - Batch processing with error collection
     * - Graceful handling of partial failures
     * - Summary response generation
     * <p>
     * The implementation processes each request individually, collecting both
     * successes and failures,
     * then returns a comprehensive summary to the client.
     * 
     * @param requests Multi stream of CreateUserRequest from client
     * @return Uni<CreateUsersResponse> containing batch operation summary
     */
    @Override
    public Uni<CreateUsersResponse> createUsers(Multi<CreateUserRequest> requests) {
        LoggingUtils.logStreamingEvent(logger, "started", "client", "createUsers batch operation");
        Instant startTime = Instant.now();

        return userBusinessService.createMultipleUsers(requests)
                .invoke(response -> {
                    LoggingUtils.logStreamingEvent(logger, "completed", "client",
                            String.format("created: %d, errors: %d",
                                    response.getCreatedCount(), response.getErrorsCount()));
                    LoggingUtils.logMetric(logger, "grpc.createUsers.created_count",
                            response.getCreatedCount(), "count");
                    LoggingUtils.logMetric(logger, "grpc.createUsers.error_count",
                            response.getErrorsCount(), "count");
                    LoggingUtils.logMetric(logger, "grpc.createUsers.duration",
                            java.time.Duration.between(startTime, Instant.now()).toMillis(), "ms");
                })
                .onFailure().invoke(error -> {
                    LoggingUtils.logStreamingEvent(logger, "error", "client",
                            String.format("createUsers failed: %s", error.getMessage()));
                    LoggingUtils.logMetric(logger, "grpc.createUsers.batch_error", 1, "count");
                })
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "createUsers"));
    }

    /**
     * Subscribes to real-time user updates using bidirectional streaming.
     * <p>
     * Bidirectional Streaming RPC: Client sends stream of SubscribeRequest, server
     * returns stream of UserNotification.
     * This is the most complex gRPC pattern, enabling real-time, full-duplex
     * communication.
     * <p>
     * Key concepts demonstrated:
     * - Bidirectional streaming with Multi<T> for both input and output
     * - Real-time subscription management
     * - Client lifecycle handling (connection/disconnection)
     * - Resource cleanup on stream cancellation
     * - Integration with notification service for broadcasting
     * <p>
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
    public Multi<UserNotification> subscribeToUserUpdates(
            Multi<SubscribeRequest> requests) {
        LoggingUtils.logStreamingEvent(logger, "started", "bidirectional",
                "subscribeToUserUpdates operation");

        return requests
                .onItem().transformToMultiAndConcatenate(this::handleSubscriptionRequest)
                .onCancellation().invoke(() -> {
                    LoggingUtils.logStreamingEvent(logger, "cancelled", "bidirectional",
                            "client disconnected, cleaning up subscriptions");
                    LoggingUtils.logMetric(logger, "grpc.subscriptions.cancelled", 1, "count");
                })
                .onFailure().invoke(error -> {
                    LoggingUtils.logStreamingEvent(logger, "error", "bidirectional",
                            String.format("subscribeToUserUpdates failed: %s", error.getMessage()));
                    LoggingUtils.logMetric(logger, "grpc.subscriptions.error", 1, "count");
                })
                .onFailure().transform(throwable -> exceptionHandler.mapException(throwable, "subscribeToUserUpdates"));
    }

    /**
     * Handles individual subscription requests from clients.
     * <p>
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
    private Multi<UserNotification> handleSubscriptionRequest(SubscribeRequest subscribeRequest) {
        String clientId = subscribeRequest.getClientId();

        // Generate client ID if not provided
        boolean generated = false;
        if (clientId.trim().isEmpty()) {
            clientId = notificationService.generateClientId();
            generated = true;
        }

        LoggingUtils.setClientId(clientId);

        if (generated) {
            LoggingUtils.logStreamingEvent(logger, "client-generated", "bidirectional",
                    String.format("generated client ID: %s", clientId));
        } else {
            LoggingUtils.logStreamingEvent(logger, "client-connected", "bidirectional",
                    String.format("processing subscription for client: %s", clientId));
        }

        // Log the notification types the client is interested in
        if (subscribeRequest.getNotificationTypesCount() > 0) {
            LoggingUtils.logStreamingEvent(logger, "subscription-filter", "bidirectional",
                    String.format("client %s subscribing to types: %s", clientId,
                            subscribeRequest.getNotificationTypesList()));
        } else {
            LoggingUtils.logStreamingEvent(logger, "subscription-all", "bidirectional",
                    String.format("client %s subscribing to all notification types", clientId));
        }

        // Subscribe the client to notifications
        final String finalClientId = clientId;
        return notificationService.subscribe(finalClientId)
                .onItem().invoke(notification -> {
                    LoggingUtils.logStreamingEvent(logger, "notification-sent", "bidirectional",
                            String.format("client %s: %s for user %s", finalClientId,
                                    notification.getType(), notification.getUser().getId()));
                    LoggingUtils.logMetric(logger, "grpc.notifications.sent", 1, "count");
                })
                .onCompletion().invoke(() -> {
                    LoggingUtils.logStreamingEvent(logger, "subscription-completed", "bidirectional",
                            String.format("client: %s", finalClientId));
                })
                .onCancellation().invoke(() -> {
                    LoggingUtils.logStreamingEvent(logger, "subscription-cancelled", "bidirectional",
                            String.format("client: %s", finalClientId));
                    notificationService.unsubscribe(finalClientId);
                    LoggingUtils.logMetric(logger, "grpc.subscriptions.unsubscribed", 1, "count");
                })
                .onFailure().invoke(error -> {
                    LoggingUtils.logStreamingEvent(logger, "subscription-error", "bidirectional",
                            String.format("client %s: %s", finalClientId, error.getMessage()));
                    notificationService.unsubscribe(finalClientId);
                    LoggingUtils.logMetric(logger, "grpc.subscriptions.error", 1, "count");
                });
    }

}