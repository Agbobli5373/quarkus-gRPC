package org.isaac.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import org.isaac.grpc.user.*;
import org.isaac.grpc.user.UserProto.SubscribeRequest;
import org.isaac.grpc.user.UserProto.User;
import org.isaac.grpc.user.UserProto.UserNotification;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Real-time notification subscriber client demonstrating bidirectional streaming
 * for receiving user update notifications in real-time.
 */
public class NotificationSubscriberClient {
    
    private static final Logger logger = Logger.getLogger(NotificationSubscriberClient.class.getName());
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;
    
    private final ManagedChannel channel;
    private final MutinyUserServiceGrpc.MutinyUserServiceStub asyncStub;
    private volatile boolean running = true;
    
    public NotificationSubscriberClient() {
        this.channel = ManagedChannelBuilder.forAddress(SERVER_HOST, SERVER_PORT)
                .usePlaintext()
                .build();
        this.asyncStub = MutinyUserServiceGrpc.newMutinyStub(channel);
    }
    
    public static void main(String[] args) {
        NotificationSubscriberClient client = new NotificationSubscriberClient();
        
        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down notification subscriber...");
            client.stop();
        }));
        
        try {
            client.subscribeToNotifications();
        } catch (Exception e) {
            logger.severe("Error in notification subscriber: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
    
    /**
     * Subscribes to real-time user notifications and keeps the connection alive
     */
    public void subscribeToNotifications() {
        logger.info("=== Starting Real-time Notification Subscriber ===");
        logger.info("Subscribing to user notifications... (Press Ctrl+C to stop)");
        
        try {
            CountDownLatch latch = new CountDownLatch(1);
            
            // Create subscription request for all notification types
            Multi<SubscribeRequest> subscriptionStream = Multi.createFrom().items(
                    SubscribeRequest.newBuilder()
                            .setClientId("notification-subscriber-" + System.currentTimeMillis())
                            .addNotificationTypes(UserNotification.NotificationType.CREATED)
                            .addNotificationTypes(UserNotification.NotificationType.UPDATED)
                            .addNotificationTypes(UserNotification.NotificationType.DELETED)
                            .build()
            );
            
            // Subscribe to the notification stream
            asyncStub.subscribeToUserUpdates(subscriptionStream)
                    .subscribe().with(
                            this::handleNotification,
                            this::handleError,
                            () -> {
                                logger.info("Notification stream completed");
                                latch.countDown();
                            }
                    );
            
            logger.info("Successfully subscribed to notifications. Waiting for updates...");
            
            // Keep the client running until interrupted
            while (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.info("Subscriber interrupted");
                    break;
                }
            }
            
            // Wait a bit for any final notifications
            latch.await(2, TimeUnit.SECONDS);
            
        } catch (StatusRuntimeException e) {
            logger.severe("gRPC error in notification subscriber: " + e.getStatus());
        } catch (Exception e) {
            logger.severe("Error in notification subscriber: " + e.getMessage());
        }
        
        logger.info("=== Notification Subscriber Stopped ===");
    }
    
    /**
     * Handles incoming user notifications
     */
    private void handleNotification(UserNotification notification) {
        String timestamp = java.time.Instant.ofEpochMilli(notification.getTimestamp()).toString();
        String notificationType = notification.getType().name();
        
        logger.info(String.format("[%s] %s NOTIFICATION:", timestamp, notificationType));
        
        if (notification.hasUser()) {
            User user = notification.getUser();
            logger.info(String.format("  User: %s (ID: %s, Email: %s)", 
                    user.getName(), user.getId(), user.getEmail()));
        }
        
        if (!notification.getMessage().isEmpty()) {
            logger.info("  Message: " + notification.getMessage());
        }
        
        logger.info("  ---");
    }
    
    /**
     * Handles streaming errors
     */
    private void handleError(Throwable error) {
        if (error instanceof StatusRuntimeException) {
            StatusRuntimeException grpcError = (StatusRuntimeException) error;
            logger.severe("gRPC streaming error: " + grpcError.getStatus());
        } else {
            logger.severe("Streaming error: " + error.getMessage());
        }
        running = false;
    }
    
    /**
     * Stops the notification subscriber
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