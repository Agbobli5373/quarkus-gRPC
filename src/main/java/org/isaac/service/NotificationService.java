package org.isaac.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.isaac.grpc.user.UserProto.User;
import org.isaac.grpc.user.UserProto.UserNotification;
import org.isaac.grpc.user.UserProto.UserNotification.NotificationType;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Service for managing user update subscriptions and broadcasting
 * notifications.
 * Implements publish-subscribe pattern for real-time user updates.
 */
@ApplicationScoped
public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    // Map to store active subscribers with their broadcast processors
    private final ConcurrentHashMap<String, BroadcastProcessor<UserNotification>> subscribers = new ConcurrentHashMap<>();

    // Counter for generating unique client IDs if not provided
    private final AtomicLong clientIdCounter = new AtomicLong(0);

    /**
     * Subscribe a client to user notifications.
     * 
     * @param clientId unique identifier for the client
     * @return Multi stream of UserNotification for the subscriber
     */
    public Multi<UserNotification> subscribe(String clientId) {
        logger.info("Client subscribing: " + clientId);

        // Create a broadcast processor for this subscriber
        BroadcastProcessor<UserNotification> processor = BroadcastProcessor.create();

        // Store the processor for this client
        subscribers.put(clientId, processor);

        // Return the Multi stream that will receive notifications
        return processor
                .onCancellation().invoke(() -> {
                    logger.info("Client unsubscribed: " + clientId);
                    unsubscribe(clientId);
                })
                .onFailure().invoke(throwable -> {
                    logger.warning("Error in subscription for client " + clientId + ": " + throwable.getMessage());
                    unsubscribe(clientId);
                });
    }

    /**
     * Unsubscribe a client from notifications.
     * 
     * @param clientId unique identifier for the client to unsubscribe
     */
    public void unsubscribe(String clientId) {
        BroadcastProcessor<UserNotification> processor = subscribers.remove(clientId);
        if (processor != null) {
            processor.onComplete();
            logger.info("Client unsubscribed: " + clientId);
        }
    }

    /**
     * Broadcast a user created notification to all subscribers.
     * 
     * @param user the user that was created
     */
    public void broadcastUserCreated(User user) {
        UserNotification notification = UserNotification.newBuilder()
                .setType(NotificationType.CREATED)
                .setUser(user)
                .setTimestamp(System.currentTimeMillis())
                .setMessage("User created: " + user.getName())
                .build();

        broadcastNotification(notification);
        logger.info("Broadcasted user created notification for user: " + user.getId());
    }

    /**
     * Broadcast a user updated notification to all subscribers.
     * 
     * @param user the user that was updated
     */
    public void broadcastUserUpdated(User user) {
        UserNotification notification = UserNotification.newBuilder()
                .setType(NotificationType.UPDATED)
                .setUser(user)
                .setTimestamp(System.currentTimeMillis())
                .setMessage("User updated: " + user.getName())
                .build();

        broadcastNotification(notification);
        logger.info("Broadcasted user updated notification for user: " + user.getId());
    }

    /**
     * Broadcast a user deleted notification to all subscribers.
     * 
     * @param userId the ID of the user that was deleted
     */
    public void broadcastUserDeleted(String userId) {
        // Create a minimal user object for the notification
        User deletedUser = User.newBuilder()
                .setId(userId)
                .setName("") // Name not available after deletion
                .setEmail("") // Email not available after deletion
                .build();

        UserNotification notification = UserNotification.newBuilder()
                .setType(NotificationType.DELETED)
                .setUser(deletedUser)
                .setTimestamp(System.currentTimeMillis())
                .setMessage("User deleted: " + userId)
                .build();

        broadcastNotification(notification);
        logger.info("Broadcasted user deleted notification for user: " + userId);
    }

    /**
     * Broadcast a notification to all active subscribers.
     * 
     * @param notification the notification to broadcast
     */
    private void broadcastNotification(UserNotification notification) {
        int subscriberCount = subscribers.size();
        logger.info("Broadcasting notification to " + subscriberCount + " subscribers");

        // Send notification to all active subscribers
        subscribers.forEach((clientId, processor) -> {
            try {
                processor.onNext(notification);
            } catch (Exception e) {
                logger.warning("Failed to send notification to client " + clientId + ": " + e.getMessage());
                // Remove failed subscriber
                unsubscribe(clientId);
            }
        });
    }

    /**
     * Get the number of active subscribers.
     * 
     * @return number of active subscribers
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }

    /**
     * Generate a unique client ID.
     * 
     * @return unique client ID
     */
    public String generateClientId() {
        return "client-" + clientIdCounter.incrementAndGet();
    }

    /**
     * Check if a client is subscribed.
     * 
     * @param clientId the client ID to check
     * @return true if the client is subscribed, false otherwise
     */
    public boolean isSubscribed(String clientId) {
        return subscribers.containsKey(clientId);
    }

    /**
     * Cleanup all subscriptions (useful for testing or shutdown).
     */
    public void cleanup() {
        logger.info("Cleaning up all subscriptions");
        subscribers.forEach((clientId, processor) -> {
            processor.onComplete();
        });
        subscribers.clear();
    }
}