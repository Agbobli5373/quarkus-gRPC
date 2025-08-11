package org.isaac.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.isaac.grpc.user.*;
import org.isaac.grpc.user.UserProto.CreateUserRequest;
import org.isaac.grpc.user.UserProto.CreateUsersResponse;
import org.isaac.grpc.user.UserProto.DeleteUserRequest;
import org.isaac.grpc.user.UserProto.DeleteUserResponse;
import org.isaac.grpc.user.UserProto.Empty;
import org.isaac.grpc.user.UserProto.GetUserRequest;
import org.isaac.grpc.user.UserProto.SubscribeRequest;
import org.isaac.grpc.user.UserProto.UpdateUserRequest;
import org.isaac.grpc.user.UserProto.User;
import org.isaac.grpc.user.UserProto.UserNotification;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Comprehensive gRPC client demonstrating all four types of gRPC operations:
 * - Unary calls (single request → single response)
 * - Server streaming (single request → stream of responses)
 * - Client streaming (stream of requests → single response)
 * - Bidirectional streaming (stream of requests ↔ stream of responses)
 */
public class UserGrpcClient {

    private static final Logger logger = Logger.getLogger(UserGrpcClient.class.getName());
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;

    private final ManagedChannel channel;
    private final MutinyUserServiceGrpc.MutinyUserServiceStub asyncStub;

    public UserGrpcClient() {
        this.channel = ManagedChannelBuilder.forAddress(SERVER_HOST, SERVER_PORT)
                .usePlaintext()
                .build();
        this.asyncStub = MutinyUserServiceGrpc.newMutinyStub(channel);
    }

    public static void main(String[] args) {
        UserGrpcClient client = new UserGrpcClient();

        try {
            logger.info("=== Starting gRPC Client Demonstration ===");

            // Demonstrate unary calls
            client.demonstrateUnaryOperations();

            // Demonstrate server streaming
            client.demonstrateServerStreaming();

            // Demonstrate client streaming
            client.demonstrateClientStreaming();

            // Demonstrate bidirectional streaming
            client.demonstrateBidirectionalStreaming();

            logger.info("=== gRPC Client Demonstration Complete ===");

        } catch (Exception e) {
            logger.severe("Error during demonstration: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    /**
     * Demonstrates unary RPC calls - single request to single response
     */
    public void demonstrateUnaryOperations() {
        logger.info("\n--- Demonstrating Unary Operations ---");

        try {
            // Create a user
            logger.info("Creating a new user...");
            CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                    .setName("John Doe")
                    .setEmail("john.doe@example.com")
                    .build();

            User createdUser = asyncStub.createUser(createRequest)
                    .await().atMost(Duration.ofSeconds(5));

            logger.info("Created user: " + formatUser(createdUser));
            String userId = createdUser.getId();

            // Get the user
            logger.info("Retrieving user by ID...");
            GetUserRequest getRequest = GetUserRequest.newBuilder()
                    .setId(userId)
                    .build();

            User retrievedUser = asyncStub.getUser(getRequest)
                    .await().atMost(Duration.ofSeconds(5));

            logger.info("Retrieved user: " + formatUser(retrievedUser));

            // Update the user
            logger.info("Updating user...");
            UpdateUserRequest updateRequest = UpdateUserRequest.newBuilder()
                    .setId(userId)
                    .setName("John Smith")
                    .setEmail("john.smith@example.com")
                    .build();

            User updatedUser = asyncStub.updateUser(updateRequest)
                    .await().atMost(Duration.ofSeconds(5));

            logger.info("Updated user: " + formatUser(updatedUser));

            // Delete the user
            logger.info("Deleting user...");
            DeleteUserRequest deleteRequest = DeleteUserRequest.newBuilder()
                    .setId(userId)
                    .build();

            DeleteUserResponse deleteResponse = asyncStub.deleteUser(deleteRequest)
                    .await().atMost(Duration.ofSeconds(5));

            logger.info("Delete response: success=" + deleteResponse.getSuccess() +
                    ", message=" + deleteResponse.getMessage());

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during unary operations: " + e.getStatus());
        } catch (Exception e) {
            logger.severe("Error during unary operations: " + e.getMessage());
        }
    }

    /**
     * Demonstrates server streaming - single request to stream of responses
     */
    public void demonstrateServerStreaming() {
        logger.info("\n--- Demonstrating Server Streaming ---");

        try {
            // First, create some test users
            logger.info("Creating test users for streaming demonstration...");
            for (int i = 1; i <= 3; i++) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                        .setName("User " + i)
                        .setEmail("user" + i + "@example.com")
                        .build();

                asyncStub.createUser(request)
                        .await().atMost(Duration.ofSeconds(5));
            }

            // Now demonstrate server streaming
            logger.info("Starting server streaming - listing all users...");
            Empty empty = Empty.newBuilder().build();

            List<User> users = asyncStub.listUsers(empty)
                    .collect().asList()
                    .await().atMost(Duration.ofSeconds(10));

            logger.info("Received " + users.size() + " users via server streaming:");
            for (User user : users) {
                logger.info("  - " + formatUser(user));
            }

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during server streaming: " + e.getStatus());
        } catch (Exception e) {
            logger.severe("Error during server streaming: " + e.getMessage());
        }
    }

    /**
     * Demonstrates client streaming - stream of requests to single response
     */
    public void demonstrateClientStreaming() {
        logger.info("\n--- Demonstrating Client Streaming ---");
        
        try {
            logger.info("Starting client streaming - creating multiple users...");
            
            // Create a stream of user creation requests
            Multi<CreateUserRequest> requestStream = Multi.createFrom().items(
                    CreateUserRequest.newBuilder()
                            .setName("Alice Johnson")
                            .setEmail("alice@example.com")
                            .build(),
                    CreateUserRequest.newBuilder()
                            .setName("Bob Wilson")
                            .setEmail("bob@example.com")
                            .build(),
                    CreateUserRequest.newBuilder()
                            .setName("Carol Brown")
                            .setEmail("carol@example.com")
                            .build(),
                    CreateUserRequest.newBuilder()
                            .setName("David Davis")
                            .setEmail("david@example.com")
                            .build()
            );
            
            // Send the stream and get the response
            CreateUsersResponse response = asyncStub.createUsers(requestStream)
                    .await().atMost(Duration.ofSeconds(10));
            
            logger.info("Client streaming completed:");
            logger.info("  Created count: " + response.getCreatedCount());
            logger.info("  Created user IDs: " + response.getCreatedUserIdsList());
            if (response.getErrorsCount() > 0) {
                logger.info("  Errors: " + response.getErrorsList());
            }
            
        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during client streaming: " + e.getStatus());
        } catch (Exception e) {
            logger.severe("Error during client streaming: " + e.getMessage());
        }
    }

/**
     * Demonstrates bidirectional streaming - stream of requests ↔ stream of responses
     */
    public void demonstrateBidirectionalStreaming() {
        logger.info("\n--- Demonstrating Bidirectional Streaming ---");
        
        try {
            logger.info("Starting bidirectional streaming - subscribing to user updates...");
            
            CountDownLatch latch = new CountDownLatch(1);
            
            // Create subscription request stream
            Multi<SubscribeRequest> subscriptionStream = Multi.createFrom().items(
         SubscribeRequest.newBuilder()
                            .setClientId("demo-client-1")
                            .addNotificationTypes(UserNotification.NotificationType.CREATED)
                            .addNotificationTypes(UserNotification.NotificationType.UPDATED)
                            .addNotificationTypes(UserNotification.NotificationType.DELETED)
                            .build()
            );
            
            // Subscribe to notifications
            asyncStub.subscribeToUserUpdates(subscriptionStream)
                    .subscribe().with(
                            notification -> {
                                logger.info("Received notification: " + formatNotification(notification));
                            },
                            failure -> {
                                logger.severe("Streaming error: " + failure.getMessage());
                                latch.countDown();
                            },
                            () -> {
                                logger.info("Notification stream completed");
                                latch.countDown();
                            }
                    );
            
            // Give some time for the subscription to be established
            Thread.sleep(1000);
            
            // Create some users to trigger notifications
            logger.info("Creating users to trigger notifications...");
            for (int i = 1; i <= 2; i++) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                        .setName("Notification User " + i)
                        .setEmail("notification" + i + "@example.com")
                        .build();
                
                asyncStub.createUser(request)
                        .await().atMost(Duration.ofSeconds(5));
                
                Thread.sleep(500); // Small delay between creations
            }
            
            // Wait for notifications or timeout
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                logger.info("Bidirectional streaming demonstration completed (timeout)");
            }
            
        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during bidirectional streaming: " + e.getStatus());
        } catch (Exception e) {
            logger.severe("Error during bidirectional streaming: " + e.getMessage());
        }
    }
    
    /**
     * Formats a User object for logging
     */
    private String formatUser(User user) {
        return String.format("User{id='%s', name='%s', email='%s', createdAt=%d, updatedAt=%d}",
                user.getId(), user.getName(), user.getEmail(), 
                user.getCreatedAt(), user.getUpdatedAt());
    }
    
    /**
     * Formats a UserNotification object for logging
     */
    private String formatNotification(UserNotification notification) {
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