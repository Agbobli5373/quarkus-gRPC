package org.isaac.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.isaac.grpc.user.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Standalone gRPC client that demonstrates all four types of gRPC operations
 * without any Quarkus dependencies. This client can be run independently
 * and connects to the gRPC Learning Service.
 * <p> <p>
 * Key differences from the Quarkus-integrated client:
 * - Uses standard gRPC Java libraries (no Mutiny reactive types)
 * - Uses blocking and async stubs instead of Mutiny stubs
 * - Can be packaged and distributed independently
 * - No dependency on server project classes
 */
public class StandaloneUserClient {

    private static final Logger logger = Logger.getLogger(StandaloneUserClient.class.getName());
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;

    private final ManagedChannel channel;
    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;
    private final UserServiceGrpc.UserServiceStub asyncStub;

    public StandaloneUserClient() {
        // Create gRPC channel - this is the connection to the server
        this.channel = ManagedChannelBuilder.forAddress(SERVER_HOST, SERVER_PORT)
                .usePlaintext() // Use plaintext for local development (no TLS)
                .build();

        // Create stubs for different interaction patterns
        this.blockingStub = UserServiceGrpc.newBlockingStub(channel); // For synchronous calls
        this.asyncStub = UserServiceGrpc.newStub(channel); // For asynchronous/streaming calls
    }

    public static void main(String[] args) {
        StandaloneUserClient client = new StandaloneUserClient();

        try {
            logger.info("=== Starting Standalone gRPC Client ===");

            // Demonstrate different gRPC patterns
            client.demonstrateUnaryOperations();
            client.demonstrateServerStreaming();
            client.demonstrateClientStreaming();
            client.demonstrateBidirectionalStreaming();

            logger.info("=== Standalone gRPC Client Complete ===");

        } catch (Exception e) {
            logger.severe("Error in client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    /**
     * Demonstrates unary RPC calls using blocking stub
     * This is the simplest pattern - one request, one response
     */
    public void demonstrateUnaryOperations() {
        logger.info("\n--- Demonstrating Unary Operations (Blocking) ---");

        try {
            // Create a user
            logger.info("Creating a new user...");
            UserProto.CreateUserRequest createRequest = UserProto.CreateUserRequest.newBuilder()
                    .setName("Standalone Client User")
                    .setEmail("standalone@example.com")
                    .build();

            UserProto.User createdUser = blockingStub.createUser(createRequest);
            logger.info("Created user: " + formatUser(createdUser));
            String userId = createdUser.getId();

            // Get the user
            logger.info("Retrieving user by ID...");
            UserProto.GetUserRequest getRequest = UserProto.GetUserRequest.newBuilder()
                    .setId(userId)
                    .build();

            UserProto.User retrievedUser = blockingStub.getUser(getRequest);
            logger.info("Retrieved user: " + formatUser(retrievedUser));

            // Update the user
            logger.info("Updating user...");
            UserProto.UpdateUserRequest updateRequest = UserProto.UpdateUserRequest.newBuilder()
                    .setId(userId)
                    .setName("Updated Standalone User")
                    .setEmail("updated.standalone@example.com")
                    .build();

            UserProto.User updatedUser = blockingStub.updateUser(updateRequest);
            logger.info("Updated user: " + formatUser(updatedUser));

            // Delete the user
            logger.info("Deleting user...");
            UserProto.DeleteUserRequest deleteRequest = UserProto.DeleteUserRequest.newBuilder()
                    .setId(userId)
                    .build();

            UserProto.DeleteUserResponse deleteResponse = blockingStub.deleteUser(deleteRequest);
            logger.info("Delete response: success=" + deleteResponse.getSuccess() +
                    ", message=" + deleteResponse.getMessage());

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during unary operations: " + e.getStatus());
        }
    }

    /**
     * Demonstrates server streaming using async stub
     * Server sends multiple responses for one request
     */
    public void demonstrateServerStreaming() {
        logger.info("\n--- Demonstrating Server Streaming ---");

        try {
            // First, create some test users
            logger.info("Creating test users for streaming demonstration...");
            for (int i = 1; i <= 3; i++) {
                UserProto.CreateUserRequest request = UserProto.CreateUserRequest.newBuilder()
                        .setName("Stream User " + i)
                        .setEmail("stream" + i + "@example.com")
                        .build();

                blockingStub.createUser(request);
            }

            // Now demonstrate server streaming
            logger.info("Starting server streaming - listing all users...");
            UserProto.Empty empty = UserProto.Empty.newBuilder().build();

            CountDownLatch latch = new CountDownLatch(1);

            // Use async stub for streaming
            asyncStub.listUsers(empty, new StreamObserver<UserProto.User>() {
                @Override
                public void onNext(UserProto.User user) {
                    logger.info("Received user from stream: " + formatUser(user));
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.severe("Error in server streaming: " + throwable.getMessage());
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    logger.info("Server streaming completed");
                    latch.countDown();
                }
            });

            // Wait for streaming to complete
            latch.await(10, TimeUnit.SECONDS);

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during server streaming: " + e.getStatus());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Interrupted during server streaming");
        }
    }

    /**
     * Demonstrates client streaming using async stub
     * Client sends multiple requests, server sends one response
     */
    public void demonstrateClientStreaming() {
        logger.info("\n--- Demonstrating Client Streaming ---");

        try {
            CountDownLatch latch = new CountDownLatch(1);

            // Create a stream observer to handle the response
            StreamObserver<UserProto.CreateUsersResponse> responseObserver = new StreamObserver<UserProto.CreateUsersResponse>() {
                @Override
                public void onNext(UserProto.CreateUsersResponse response) {
                    logger.info("Client streaming completed:");
                    logger.info("  Created count: " + response.getCreatedCount());
                    logger.info("  Created user IDs: " + response.getCreatedUserIdsList());
                    if (response.getErrorsCount() > 0) {
                        logger.info("  Errors: " + response.getErrorsList());
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.severe("Error in client streaming: " + throwable.getMessage());
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    logger.info("Client streaming response completed");
                    latch.countDown();
                }
            };

            // Get the request stream observer
            StreamObserver<UserProto.CreateUserRequest> requestObserver = asyncStub.createUsers(responseObserver);

            try {
                // Send multiple requests
                logger.info("Sending multiple user creation requests...");
                String[] names = { "Alice Standalone", "Bob Standalone", "Carol Standalone" };
                String[] emails = { "alice.standalone@example.com", "bob.standalone@example.com",
                        "carol.standalone@example.com" };

                for (int i = 0; i < names.length; i++) {
                    UserProto.CreateUserRequest request = UserProto.CreateUserRequest.newBuilder()
                            .setName(names[i])
                            .setEmail(emails[i])
                            .build();

                    requestObserver.onNext(request);
                    logger.info("Sent request for: " + names[i]);

                    // Small delay between requests
                    Thread.sleep(100);
                }

                // Complete the request stream
                requestObserver.onCompleted();

            } catch (RuntimeException e) {
                // Cancel RPC
                requestObserver.onError(e);
                throw e;
            }

            // Wait for response
            latch.await(10, TimeUnit.SECONDS);

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during client streaming: " + e.getStatus());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Interrupted during client streaming");
        }
    }

    /**
     * Demonstrates bidirectional streaming using async stub
     * Both client and server can send multiple messages
     */
    public void demonstrateBidirectionalStreaming() {
        logger.info("\n--- Demonstrating Bidirectional Streaming ---");

        try {
            CountDownLatch latch = new CountDownLatch(1);

            // Create observer for incoming notifications
            StreamObserver<UserProto.UserNotification> notificationObserver = new StreamObserver<UserProto.UserNotification>() {
                @Override
                public void onNext(UserProto.UserNotification notification) {
                    logger.info("Received notification: " + formatNotification(notification));
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.severe("Error in bidirectional streaming: " + throwable.getMessage());
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    logger.info("Notification stream completed");
                    latch.countDown();
                }
            };

            // Start bidirectional streaming
            StreamObserver<UserProto.SubscribeRequest> subscriptionObserver = asyncStub
                    .subscribeToUserUpdates(notificationObserver);

            try {
                // Send subscription request
                UserProto.SubscribeRequest subscribeRequest = UserProto.SubscribeRequest.newBuilder()
                        .setClientId("standalone-client-" + System.currentTimeMillis())
                        .addNotificationTypes(UserProto.UserNotification.NotificationType.CREATED)
                        .addNotificationTypes(UserProto.UserNotification.NotificationType.UPDATED)
                        .addNotificationTypes(UserProto.UserNotification.NotificationType.DELETED)
                        .build();

                subscriptionObserver.onNext(subscribeRequest);
                logger.info("Sent subscription request");

                // Give some time for subscription to be established
                Thread.sleep(1000);

                // Create some users to trigger notifications
                logger.info("Creating users to trigger notifications...");
                for (int i = 1; i <= 2; i++) {
                    UserProto.CreateUserRequest request = UserProto.CreateUserRequest.newBuilder()
                            .setName("Notification User " + i)
                            .setEmail("notification" + i + "@standalone.com")
                            .build();

                    // Use blocking stub to create users (this will trigger notifications)
                    blockingStub.createUser(request);
                    Thread.sleep(500);
                }

                // Keep subscription alive for a bit to receive notifications
                Thread.sleep(2000);

                // Complete the subscription stream
                subscriptionObserver.onCompleted();

            } catch (RuntimeException e) {
                subscriptionObserver.onError(e);
                throw e;
            }

            // Wait for notifications or timeout
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                logger.info("Bidirectional streaming timed out");
            }

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during bidirectional streaming: " + e.getStatus());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Interrupted during bidirectional streaming");
        }
    }

    /**
     * Formats a User object for logging
     */
    private String formatUser(UserProto.User user) {
        return String.format("User{id='%s', name='%s', email='%s', createdAt=%d, updatedAt=%d}",
                user.getId(), user.getName(), user.getEmail(),
                user.getCreatedAt(), user.getUpdatedAt());
    }

    /**
     * Formats a UserNotification object for logging
     */
    private String formatNotification(UserProto.UserNotification notification) {
        return String.format("UserNotification{type=%s, user=%s, timestamp=%d, message='%s'}",
                notification.getType(),
                notification.hasUser() ? formatUser(notification.getUser()) : "null",
                notification.getTimestamp(),
                notification.getMessage());
    }

    /**
     * Shuts down the gRPC channel
     */
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            logger.info("gRPC channel shut down successfully");
        } catch (InterruptedException e) {
            logger.warning("Error shutting down gRPC channel: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}