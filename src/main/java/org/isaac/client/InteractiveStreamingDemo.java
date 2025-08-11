package org.isaac.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import org.isaac.grpc.user.*;
import org.isaac.grpc.user.UserProto.CreateUserRequest;
import org.isaac.grpc.user.UserProto.CreateUsersResponse;
import org.isaac.grpc.user.UserProto.Empty;
import org.isaac.grpc.user.UserProto.SubscribeRequest;
import org.isaac.grpc.user.UserProto.User;
import org.isaac.grpc.user.UserProto.UserNotification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Interactive streaming demo that allows users to interact with all gRPC
 * streaming patterns
 * through a command-line interface.
 */
public class InteractiveStreamingDemo {

    private static final Logger logger = Logger.getLogger(InteractiveStreamingDemo.class.getName());
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;

    private final ManagedChannel channel;
    private final MutinyUserServiceGrpc.MutinyUserServiceStub asyncStub;
    private final BufferedReader reader;
    private volatile boolean running = true;

    public InteractiveStreamingDemo() {
        this.channel = ManagedChannelBuilder.forAddress(SERVER_HOST, SERVER_PORT)
                .usePlaintext()
                .build();
        this.asyncStub = MutinyUserServiceGrpc.newMutinyStub(channel);
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public static void main(String[] args) {
        InteractiveStreamingDemo demo = new InteractiveStreamingDemo();

        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down interactive demo...");
            demo.stop();
        }));

        try {
            demo.runInteractiveDemo();
        } catch (Exception e) {
            logger.severe("Error in interactive demo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            demo.shutdown();
        }
    }

    /**
     * Runs the interactive demo with a command-line menu
     */
    public void runInteractiveDemo() throws IOException {
        logger.info("=== Interactive gRPC Streaming Demo ===");
        logger.info("This demo allows you to interact with all gRPC streaming patterns.");

        while (running) {
            printMenu();
            String choice = reader.readLine();

            if (choice == null || choice.trim().isEmpty()) {
                continue;
            }

            try {
                switch (choice.trim()) {
                    case "1":
                        demonstrateUnaryCall();
                        break;
                    case "2":
                        demonstrateServerStreaming();
                        break;
                    case "3":
                        demonstrateClientStreaming();
                        break;
                    case "4":
                        demonstrateBidirectionalStreaming();
                        break;
                    case "5":
                        listAllUsers();
                        break;
                    case "6":
                        createSampleData();
                        break;
                    case "0":
                        logger.info("Exiting interactive demo...");
                        running = false;
                        break;
                    default:
                        logger.info("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                logger.severe("Error executing command: " + e.getMessage());
            }

            if (running) {
                logger.info("\nPress Enter to continue...");
                reader.readLine();
            }
        }
    }

    /**
     * Prints the interactive menu
     */
    private void printMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Interactive gRPC Streaming Demo");
        System.out.println("=".repeat(50));
        System.out.println("1. Unary Call (Create User)");
        System.out.println("2. Server Streaming (List Users)");
        System.out.println("3. Client Streaming (Batch Create Users)");
        System.out.println("4. Bidirectional Streaming (Subscribe to Notifications)");
        System.out.println("5. List All Users (Quick View)");
        System.out.println("6. Create Sample Data");
        System.out.println("0. Exit");
        System.out.println("=".repeat(50));
        System.out.print("Choose an option: ");
    }

    /**
     * Demonstrates unary call by creating a user
     */
    private void demonstrateUnaryCall() throws IOException {
        logger.info("\n--- Unary Call Demo: Create User ---");

        System.out.print("Enter user name: ");
        String name = reader.readLine();
        System.out.print("Enter user email: ");
        String email = reader.readLine();

        try {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName(name)
                    .setEmail(email)
                    .build();

            User createdUser = asyncStub.createUser(request)
                    .await().atMost(Duration.ofSeconds(5));

            logger.info("Successfully created user:");
            logger.info("  ID: " + createdUser.getId());
            logger.info("  Name: " + createdUser.getName());
            logger.info("  Email: " + createdUser.getEmail());

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error: " + e.getStatus());
        }
    }

    /**
     * Demonstrates server streaming by listing users
     */
    private void demonstrateServerStreaming() {
        logger.info("\n--- Server Streaming Demo: List Users ---");

        try {
            Empty empty = Empty.newBuilder().build();

            logger.info("Starting server streaming...");
            asyncStub.listUsers(empty)
                    .subscribe().with(
                            user -> logger.info("Received user: " + formatUser(user)),
                            failure -> logger.severe("Streaming error: " + failure.getMessage()),
                            () -> logger.info("Server streaming completed"));

            // Wait for streaming to complete
            Thread.sleep(2000);

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error: " + e.getStatus());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Demonstrates client streaming by batch creating users
     */
    private void demonstrateClientStreaming() throws IOException {
        logger.info("\n--- Client Streaming Demo: Batch Create Users ---");

        System.out.print("How many users would you like to create? ");
        String countStr = reader.readLine();

        try {
            int count = Integer.parseInt(countStr);
            if (count <= 0 || count > 20) {
                logger.info("Please enter a number between 1 and 20");
                return;
            }

            Multi<CreateUserRequest> requestStream = Multi.createFrom().range(1, count + 1)
                    .map(i -> CreateUserRequest.newBuilder()
                            .setName("Batch User " + i)
                            .setEmail("batch.user" + i + "@demo.com")
                            .build());

            logger.info("Starting client streaming with " + count + " users...");

            CreateUsersResponse response = asyncStub.createUsers(requestStream)
                    .await().atMost(Duration.ofSeconds(15));

            logger.info("Client streaming completed:");
            logger.info("  Created: " + response.getCreatedCount() + " users");
            logger.info("  Errors: " + response.getErrorsCount());

        } catch (NumberFormatException e) {
            logger.info("Invalid number format");
        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error: " + e.getStatus());
        }
    }

    /**
     * Demonstrates bidirectional streaming with notifications
          * @throws InterruptedException 
          */
         private void demonstrateBidirectionalStreaming() throws IOException, InterruptedException {
        logger.info("\n--- Bidirectional Streaming Demo: User Notifications ---");
        logger.info("This will subscribe to user notifications for 10 seconds.");
        logger.info("Create/update/delete users in another client to see notifications.");

        try {
            CountDownLatch latch = new CountDownLatch(1);

            Multi<SubscribeRequest> subscriptionStream = Multi.createFrom().items(
                    SubscribeRequest.newBuilder()
                            .setClientId("interactive-demo-" + System.currentTimeMillis())
                            .addNotificationTypes(UserNotification.NotificationType.CREATED)
                            .addNotificationTypes(UserNotification.NotificationType.UPDATED)
                            .addNotificationTypes(UserNotification.NotificationType.DELETED)
                            .build());

            logger.info("Subscribing to notifications...");

            asyncStub.subscribeToUserUpdates(subscriptionStream)
                    .subscribe().with(
                            notification -> {
                                logger.info("📢 NOTIFICATION: " + notification.getType() +
                                        " - " + formatUser(notification.getUser()));
                            },
                            failure -> {
                                logger.severe("Streaming error: " + failure.getMessage());
                                latch.countDown();
                            },
                            () -> {
                                logger.info("Notification stream completed");
                                latch.countDown();
                            });

            // Wait for 10 seconds
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                logger.info("Notification subscription timed out after 10 seconds");
            }

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error: " + e.getStatus());
        }
    }

    /**
     * Lists all users using server streaming
     */
    private void listAllUsers() {
        logger.info("\n--- Quick User List ---");

        try {
            Empty empty = Empty.newBuilder().build();

            asyncStub.listUsers(empty)
                    .collect().asList()
                    .subscribe().with(
                            users -> {
                                if (users.isEmpty()) {
                                    logger.info("No users found");
                                } else {
                                    logger.info("Found " + users.size() + " users:");
                                    for (int i = 0; i < users.size(); i++) {
                                        logger.info("  " + (i + 1) + ". " + formatUser(users.get(i)));
                                    }
                                }
                            },
                            failure -> logger.severe("Error listing users: " + failure.getMessage()));

            Thread.sleep(1000); // Wait for completion

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error: " + e.getStatus());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates sample data for demonstration
     */
    private void createSampleData() {
        logger.info("\n--- Creating Sample Data ---");

        try {
            String[] names = { "Alice Johnson", "Bob Smith", "Carol Williams", "David Brown" };
            String[] emails = { "alice@demo.com", "bob@demo.com", "carol@demo.com", "david@demo.com" };

            for (int i = 0; i < names.length; i++) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                        .setName(names[i])
                        .setEmail(emails[i])
                        .build();

                User user = asyncStub.createUser(request)
                        .await().atMost(Duration.ofSeconds(5));

                logger.info("Created: " + formatUser(user));
            }

            logger.info("Sample data creation completed");

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error: " + e.getStatus());
        }
    }

    /**
     * Formats a User object for display
     */
    private String formatUser(User user) {
        return String.format("%s <%s> (ID: %s)", user.getName(), user.getEmail(), user.getId());
    }

    /**
     * Stops the interactive demo
     */
    public void stop() {
        running = false;
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