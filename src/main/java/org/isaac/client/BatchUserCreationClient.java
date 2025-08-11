package org.isaac.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import org.isaac.grpc.user.*;
import org.isaac.grpc.user.UserProto.CreateUserRequest;
import org.isaac.grpc.user.UserProto.CreateUsersResponse;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Batch user creation client demonstrating client streaming
 * for efficiently creating multiple users in a single operation.
 */
public class BatchUserCreationClient {

    private static final Logger logger = Logger.getLogger(BatchUserCreationClient.class.getName());
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;

    private final ManagedChannel channel;
    private final MutinyUserServiceGrpc.MutinyUserServiceStub asyncStub;

    public BatchUserCreationClient() {
        this.channel = ManagedChannelBuilder.forAddress(SERVER_HOST, SERVER_PORT)
                .usePlaintext()
                .build();
        this.asyncStub = MutinyUserServiceGrpc.newMutinyStub(channel);
    }

    public static void main(String[] args) {
        BatchUserCreationClient client = new BatchUserCreationClient();

        try {
            logger.info("=== Starting Batch User Creation Client ===");

            // Demonstrate batch creation with predefined users
            client.createPredefinedUsers();

            // Demonstrate batch creation with generated users
            client.createGeneratedUsers(10);

            // Demonstrate batch creation with mixed valid/invalid data
            client.createMixedValidityUsers();

            logger.info("=== Batch User Creation Complete ===");

        } catch (Exception e) {
            logger.severe("Error during batch user creation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    /**
     * Creates a batch of predefined users
     */
    public void createPredefinedUsers() {
        logger.info("\n--- Creating Predefined Users ---");

        try {
            List<CreateUserRequest> userRequests = Arrays.asList(
                    CreateUserRequest.newBuilder()
                            .setName("Alice Johnson")
                            .setEmail("alice.johnson@company.com")
                            .build(),
                    CreateUserRequest.newBuilder()
                            .setName("Bob Smith")
                            .setEmail("bob.smith@company.com")
                            .build(),
                    CreateUserRequest.newBuilder()
                            .setName("Carol Williams")
                            .setEmail("carol.williams@company.com")
                            .build(),
                    CreateUserRequest.newBuilder()
                            .setName("David Brown")
                            .setEmail("david.brown@company.com")
                            .build(),
                    CreateUserRequest.newBuilder()
                            .setName("Eva Davis")
                            .setEmail("eva.davis@company.com")
                            .build());

            Multi<CreateUserRequest> requestStream = Multi.createFrom().iterable(userRequests);

            logger.info("Sending " + userRequests.size() + " user creation requests...");

            CreateUsersResponse response = asyncStub.createUsers(requestStream)
                    .await().atMost(Duration.ofSeconds(15));

            logBatchResult("Predefined Users", response);

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during predefined user creation: " + e.getStatus());
        } catch (Exception e) {
            logger.severe("Error during predefined user creation: " + e.getMessage());
        }
    }

    /**
     * Creates a batch of generated users
     */
    public void createGeneratedUsers(int count) {
        logger.info("\n--- Creating " + count + " Generated Users ---");

        try {
            Multi<CreateUserRequest> requestStream = Multi.createFrom().range(1, count + 1)
                    .map(i -> CreateUserRequest.newBuilder()
                            .setName("Generated User " + i)
                            .setEmail("generated.user" + i + "@example.com")
                            .build());

            logger.info("Sending " + count + " generated user creation requests...");

            CreateUsersResponse response = asyncStub.createUsers(requestStream)
                    .await().atMost(Duration.ofSeconds(20));

            logBatchResult("Generated Users", response);

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during generated user creation: " + e.getStatus());
        } catch (Exception e) {
            logger.severe("Error during generated user creation: " + e.getMessage());
        }
    }

    /**
     * Creates a batch with mixed valid and invalid users to demonstrate error
     * handling
     */
    public void createMixedValidityUsers() {
        logger.info("\n--- Creating Mixed Validity Users (Error Handling Demo) ---");

        try {
            List<CreateUserRequest> userRequests = Arrays.asList(
                    // Valid users
                    CreateUserRequest.newBuilder()
                            .setName("Valid User 1")
                            .setEmail("valid1@example.com")
                            .build(),
                    CreateUserRequest.newBuilder()
                            .setName("Valid User 2")
                            .setEmail("valid2@example.com")
                            .build(),
                    // Invalid users (empty name)
                    CreateUserRequest.newBuilder()
                            .setName("")
                            .setEmail("invalid1@example.com")
                            .build(),
                    // Invalid users (invalid email)
                    CreateUserRequest.newBuilder()
                            .setName("Invalid User 2")
                            .setEmail("not-an-email")
                            .build(),
                    // Valid user
                    CreateUserRequest.newBuilder()
                            .setName("Valid User 3")
                            .setEmail("valid3@example.com")
                            .build());

            Multi<CreateUserRequest> requestStream = Multi.createFrom().iterable(userRequests);

            logger.info("Sending " + userRequests.size() + " mixed validity user creation requests...");

            CreateUsersResponse response = asyncStub.createUsers(requestStream)
                    .await().atMost(Duration.ofSeconds(15));

            logBatchResult("Mixed Validity Users", response);

        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error during mixed validity user creation: " + e.getStatus());
        } catch (Exception e) {
            logger.severe("Error during mixed validity user creation: " + e.getMessage());
        }
    }

    /**
     * Logs the result of a batch operation
     */
    private void logBatchResult(String operationName, CreateUsersResponse response) {
        logger.info(operationName + " - Batch Creation Results:");
        logger.info("  Successfully created: " + response.getCreatedCount() + " users");

        if (response.getCreatedUserIdsCount() > 0) {
            logger.info("  Created user IDs: " + response.getCreatedUserIdsList());
        }

        if (response.getErrorsCount() > 0) {
            logger.info("  Errors encountered: " + response.getErrorsCount());
            for (String error : response.getErrorsList()) {
                logger.info("    - " + error);
            }
        }

        double successRate = response.getCreatedCount() > 0
                ? (double) response.getCreatedCount() / (response.getCreatedCount() + response.getErrorsCount()) * 100
                : 0;
        logger.info("  Success rate: " + String.format("%.1f%%", successRate));
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