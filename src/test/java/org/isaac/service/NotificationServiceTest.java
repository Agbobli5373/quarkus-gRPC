package org.isaac.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.isaac.grpc.user.UserProto.User;
import org.isaac.grpc.user.UserProto.UserNotification;
import org.isaac.grpc.user.UserProto.UserNotification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class NotificationServiceTest {

    @Inject
    NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Clean up any existing subscriptions before each test
        notificationService.cleanup();
    }

    @Test
    void shouldSubscribeAndReceiveNotifications() {
        // Given
        String clientId = "test-client-1";

        // When
        Multi<UserNotification> notifications = notificationService.subscribe(clientId);
        AssertSubscriber<UserNotification> subscriber = notifications.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        // Then
        assertTrue(notificationService.isSubscribed(clientId));
        assertEquals(1, notificationService.getSubscriberCount());

        // Cleanup
        notificationService.unsubscribe(clientId);
    }

    @Test
    void shouldUnsubscribeClient() {
        // Given
        String clientId = "test-client-2";
        notificationService.subscribe(clientId);
        assertTrue(notificationService.isSubscribed(clientId));

        // When
        notificationService.unsubscribe(clientId);

        // Then
        assertFalse(notificationService.isSubscribed(clientId));
        assertEquals(0, notificationService.getSubscriberCount());
    }

    @Test
    void shouldBroadcastUserCreatedNotification() {
        // Given
        String clientId = "test-client-3";
        Multi<UserNotification> notifications = notificationService.subscribe(clientId);
        AssertSubscriber<UserNotification> subscriber = notifications.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        User user = User.newBuilder()
                .setId("user-1")
                .setName("John Doe")
                .setEmail("john@example.com")
                .setCreatedAt(System.currentTimeMillis())
                .build();

        // When
        notificationService.broadcastUserCreated(user);

        // Then
        subscriber.awaitItems(1, Duration.ofSeconds(5));
        List<UserNotification> items = subscriber.getItems();
        assertEquals(1, items.size());

        UserNotification notification = items.get(0);
        assertEquals(NotificationType.CREATED, notification.getType());
        assertEquals(user.getId(), notification.getUser().getId());
        assertEquals(user.getName(), notification.getUser().getName());
        assertEquals(user.getEmail(), notification.getUser().getEmail());
        assertTrue(notification.getMessage().contains("User created"));
        assertTrue(notification.getTimestamp() > 0);

        // Cleanup
        notificationService.unsubscribe(clientId);
    }

    @Test
    void shouldBroadcastUserUpdatedNotification() {
        // Given
        String clientId = "test-client-4";
        Multi<UserNotification> notifications = notificationService.subscribe(clientId);
        AssertSubscriber<UserNotification> subscriber = notifications.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        User user = User.newBuilder()
                .setId("user-2")
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setUpdatedAt(System.currentTimeMillis())
                .build();

        // When
        notificationService.broadcastUserUpdated(user);

        // Then
        subscriber.awaitItems(1, Duration.ofSeconds(5));
        List<UserNotification> items = subscriber.getItems();
        assertEquals(1, items.size());

        UserNotification notification = items.get(0);
        assertEquals(NotificationType.UPDATED, notification.getType());
        assertEquals(user.getId(), notification.getUser().getId());
        assertEquals(user.getName(), notification.getUser().getName());
        assertTrue(notification.getMessage().contains("User updated"));

        // Cleanup
        notificationService.unsubscribe(clientId);
    }

    @Test
    void shouldBroadcastUserDeletedNotification() {
        // Given
        String clientId = "test-client-5";
        Multi<UserNotification> notifications = notificationService.subscribe(clientId);
        AssertSubscriber<UserNotification> subscriber = notifications.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        String userId = "user-3";

        // When
        notificationService.broadcastUserDeleted(userId);

        // Then
        subscriber.awaitItems(1, Duration.ofSeconds(5));
        List<UserNotification> items = subscriber.getItems();
        assertEquals(1, items.size());

        UserNotification notification = items.get(0);
        assertEquals(NotificationType.DELETED, notification.getType());
        assertEquals(userId, notification.getUser().getId());
        assertTrue(notification.getMessage().contains("User deleted"));

        // Cleanup
        notificationService.unsubscribe(clientId);
    }

    @Test
    void shouldBroadcastToMultipleSubscribers() throws InterruptedException {
        // Given
        String clientId1 = "test-client-6";
        String clientId2 = "test-client-7";

        Multi<UserNotification> notifications1 = notificationService.subscribe(clientId1);
        Multi<UserNotification> notifications2 = notificationService.subscribe(clientId2);

        AssertSubscriber<UserNotification> subscriber1 = notifications1.subscribe()
                .withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<UserNotification> subscriber2 = notifications2.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        assertEquals(2, notificationService.getSubscriberCount());

        User user = User.newBuilder()
                .setId("user-4")
                .setName("Bob Smith")
                .setEmail("bob@example.com")
                .build();

        // When
        notificationService.broadcastUserCreated(user);

        // Then
        subscriber1.awaitItems(1, Duration.ofSeconds(5));
        subscriber2.awaitItems(1, Duration.ofSeconds(5));

        // Both subscribers should receive the notification
        assertEquals(1, subscriber1.getItems().size());
        assertEquals(1, subscriber2.getItems().size());

        UserNotification notification1 = subscriber1.getItems().get(0);
        UserNotification notification2 = subscriber2.getItems().get(0);

        assertEquals(notification1.getType(), notification2.getType());
        assertEquals(notification1.getUser().getId(), notification2.getUser().getId());

        // Cleanup
        notificationService.unsubscribe(clientId1);
        notificationService.unsubscribe(clientId2);
    }

    @Test
    void shouldHandleMultipleNotificationsToSameSubscriber() {
        // Given
        String clientId = "test-client-8";
        Multi<UserNotification> notifications = notificationService.subscribe(clientId);
        AssertSubscriber<UserNotification> subscriber = notifications.subscribe()
                .withSubscriber(AssertSubscriber.create(3));

        User user1 = User.newBuilder()
                .setId("user-5")
                .setName("Alice")
                .setEmail("alice@example.com")
                .build();

        User user2 = User.newBuilder()
                .setId("user-6")
                .setName("Charlie")
                .setEmail("charlie@example.com")
                .build();

        // When
        notificationService.broadcastUserCreated(user1);
        notificationService.broadcastUserUpdated(user2);
        notificationService.broadcastUserDeleted("user-7");

        // Then
        subscriber.awaitItems(3, Duration.ofSeconds(5));
        List<UserNotification> items = subscriber.getItems();
        assertEquals(3, items.size());

        assertEquals(NotificationType.CREATED, items.get(0).getType());
        assertEquals(NotificationType.UPDATED, items.get(1).getType());
        assertEquals(NotificationType.DELETED, items.get(2).getType());

        // Cleanup
        notificationService.unsubscribe(clientId);
    }

    @Test
    void shouldGenerateUniqueClientIds() {
        // When
        String clientId1 = notificationService.generateClientId();
        String clientId2 = notificationService.generateClientId();
        String clientId3 = notificationService.generateClientId();

        // Then
        assertNotEquals(clientId1, clientId2);
        assertNotEquals(clientId2, clientId3);
        assertNotEquals(clientId1, clientId3);

        assertTrue(clientId1.startsWith("client-"));
        assertTrue(clientId2.startsWith("client-"));
        assertTrue(clientId3.startsWith("client-"));
    }

    @Test
    void shouldCleanupAllSubscriptions() {
        // Given
        String clientId1 = "test-client-9";
        String clientId2 = "test-client-10";

        notificationService.subscribe(clientId1);
        notificationService.subscribe(clientId2);

        assertEquals(2, notificationService.getSubscriberCount());

        // When
        notificationService.cleanup();

        // Then
        assertEquals(0, notificationService.getSubscriberCount());
        assertFalse(notificationService.isSubscribed(clientId1));
        assertFalse(notificationService.isSubscribed(clientId2));
    }

    @Test
    void shouldHandleSubscriptionCancellation() {
        // Given
        String clientId = "test-client-11";
        Multi<UserNotification> notifications = notificationService.subscribe(clientId);
        AssertSubscriber<UserNotification> subscriber = notifications.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        assertTrue(notificationService.isSubscribed(clientId));
        assertEquals(1, notificationService.getSubscriberCount());

        // When
        subscriber.cancel();

        // Give some time for the cancellation to be processed
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        // The subscription should be automatically cleaned up on cancellation
        assertEquals(0, notificationService.getSubscriberCount());
        assertFalse(notificationService.isSubscribed(clientId));
    }

    @Test
    void shouldHandleConcurrentSubscriptions() throws InterruptedException {
        // Given
        int numberOfClients = 10;
        CountDownLatch latch = new CountDownLatch(numberOfClients);
        AtomicInteger successfulSubscriptions = new AtomicInteger(0);

        // When - Create multiple concurrent subscriptions
        for (int i = 0; i < numberOfClients; i++) {
            final String clientId = "concurrent-client-" + i;
            new Thread(() -> {
                try {
                    notificationService.subscribe(clientId);
                    successfulSubscriptions.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Wait for all threads to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Then
        assertEquals(numberOfClients, successfulSubscriptions.get());
        assertEquals(numberOfClients, notificationService.getSubscriberCount());

        // Cleanup
        notificationService.cleanup();
    }
}