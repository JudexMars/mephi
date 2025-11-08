package ru.example.url.shortener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.example.url.shortener.model.ShortenedUrl;
import ru.example.url.shortener.util.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private NotificationService notificationService;
    private UUID testUserId;
    private UUID anotherUserId;

    @BeforeEach
    void setUp() {
        notificationService = NotificationService.getInstance();
        notificationService.clearAllNotifications();
        
        testUserId = UUID.randomUUID();
        anotherUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should add and retrieve notifications")
    void testAddAndRetrieveNotifications() {
        // Given
        String message1 = "Test notification 1";
        String message2 = "Test notification 2";

        // When
        notificationService.addNotification(testUserId, message1);
        notificationService.addNotification(testUserId, message2);

        // Then
        assertEquals(2, notificationService.getNotificationCount(testUserId));
        
        List<String> notifications = notificationService.getAndClearNotifications(testUserId);
        assertEquals(2, notifications.size());
        assertTrue(notifications.contains(message1));
        assertTrue(notifications.contains(message2));
        
        // After clearing, count should be 0
        assertEquals(0, notificationService.getNotificationCount(testUserId));
    }

    @Test
    @DisplayName("Should handle multiple users independently")
    void testMultipleUsersIndependently() {
        // Given
        String message1 = "Message for user 1";
        String message2 = "Message for user 2";

        // When
        notificationService.addNotification(testUserId, message1);
        notificationService.addNotification(anotherUserId, message2);

        // Then
        assertEquals(1, notificationService.getNotificationCount(testUserId));
        assertEquals(1, notificationService.getNotificationCount(anotherUserId));

        List<String> user1Notifications = notificationService.getAndClearNotifications(testUserId);
        assertEquals(1, user1Notifications.size());
        assertEquals(message1, user1Notifications.get(0));

        // User 2 should still have their notification
        assertEquals(1, notificationService.getNotificationCount(anotherUserId));
        
        List<String> user2Notifications = notificationService.getAndClearNotifications(anotherUserId);
        assertEquals(1, user2Notifications.size());
        assertEquals(message2, user2Notifications.get(0));
    }

    @Test
    @DisplayName("Should clear notifications for specific user")
    void testClearNotificationsForUser() {
        // Given
        notificationService.addNotification(testUserId, "Message 1");
        notificationService.addNotification(testUserId, "Message 2");
        notificationService.addNotification(anotherUserId, "Another message");

        // When
        notificationService.clearNotifications(testUserId);

        // Then
        assertEquals(0, notificationService.getNotificationCount(testUserId));
        assertEquals(1, notificationService.getNotificationCount(anotherUserId));
    }

    @Test
    @DisplayName("Should clear all notifications")
    void testClearAllNotifications() {
        // Given
        notificationService.addNotification(testUserId, "Message 1");
        notificationService.addNotification(testUserId, "Message 2");
        notificationService.addNotification(anotherUserId, "Another message");

        // When
        notificationService.clearAllNotifications();

        // Then
        assertEquals(0, notificationService.getNotificationCount(testUserId));
        assertEquals(0, notificationService.getNotificationCount(anotherUserId));
    }

    @Test
    @DisplayName("Should check if user has notifications")
    void testHasNotifications() {
        // Initially no notifications
        assertFalse(notificationService.hasNotifications(testUserId));

        // Add notification
        notificationService.addNotification(testUserId, "Test message");
        assertTrue(notificationService.hasNotifications(testUserId));

        // Clear and check again
        notificationService.clearNotifications(testUserId);
        assertFalse(notificationService.hasNotifications(testUserId));
    }

    @Test
    @DisplayName("Should notify URL creation")
    void testNotifyUrlCreated() {
        // Given
        ShortenedUrl url = createTestUrl();

        // When
        notificationService.notifyUrlCreated(url);

        // Then
        assertTrue(notificationService.hasNotifications(testUserId));
        List<String> notifications = notificationService.getAndClearNotifications(testUserId);
        assertEquals(1, notifications.size());
        
        String notification = notifications.get(0);
        assertTrue(notification.contains("Short URL created successfully"));
        assertTrue(notification.contains(url.getFullShortUrl()));
        assertTrue(notification.contains(url.getOriginalUrl()));
    }

    @Test
    @DisplayName("Should notify URL expiration")
    void testNotifyUrlExpired() {
        // Given
        ShortenedUrl url = createTestUrl();

        // When
        notificationService.notifyUrlExpired(url);

        // Then
        assertTrue(notificationService.hasNotifications(testUserId));
        List<String> notifications = notificationService.getAndClearNotifications(testUserId);
        assertEquals(1, notifications.size());
        
        String notification = notifications.get(0);
        assertTrue(notification.contains("has expired"));
        assertTrue(notification.contains(url.getFullShortUrl()));
    }

    @Test
    @DisplayName("Should notify click limit reached")
    void testNotifyClickLimitReached() {
        // Given
        ShortenedUrl url = createTestUrl();

        // When
        notificationService.notifyClickLimitReached(url);

        // Then
        assertTrue(notificationService.hasNotifications(testUserId));
        List<String> notifications = notificationService.getAndClearNotifications(testUserId);
        assertEquals(1, notifications.size());
        
        String notification = notifications.get(0);
        assertTrue(notification.contains("reached its click limit"));
        assertTrue(notification.contains(url.getFullShortUrl()));
    }

    @Test
    @DisplayName("Should notify URL not accessible")
    void testNotifyUrlNotAccessible() {
        // Given
        ShortenedUrl url = createTestUrl();
        String reason = "URL has expired";

        // When
        notificationService.notifyUrlNotAccessible(url, reason);

        // Then
        assertTrue(notificationService.hasNotifications(testUserId));
        List<String> notifications = notificationService.getAndClearNotifications(testUserId);
        assertEquals(1, notifications.size());
        
        String notification = notifications.get(0);
        assertTrue(notification.contains("Access denied"));
        assertTrue(notification.contains(reason));
        assertTrue(notification.contains(url.getFullShortUrl()));
    }

    @Test
    @DisplayName("Should handle empty notifications gracefully")
    void testEmptyNotifications() {
        // When getting notifications for user with no notifications
        List<String> notifications = notificationService.getAndClearNotifications(testUserId);
        
        // Then
        assertNotNull(notifications);
        assertTrue(notifications.isEmpty());
        assertEquals(0, notificationService.getNotificationCount(testUserId));
        assertFalse(notificationService.hasNotifications(testUserId));
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    void testConcurrentAccess() {
        // This test verifies thread safety by adding notifications concurrently
        // Given
        int numberOfThreads = 10;
        int messagesPerThread = 10;
        
        // When - Add notifications from multiple threads
        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    notificationService.addNotification(testUserId, 
                        "Message from thread " + threadId + ", message " + j);
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Thread was interrupted");
            }
        }
        
        // Then
        int expectedCount = numberOfThreads * messagesPerThread;
        assertEquals(expectedCount, notificationService.getNotificationCount(testUserId));
        
        List<String> notifications = notificationService.getAndClearNotifications(testUserId);
        assertEquals(expectedCount, notifications.size());
    }

    private ShortenedUrl createTestUrl() {
        return new ShortenedUrl(
            "https://example.com/test",
            "abc123",
            testUserId,
            100,
            LocalDateTime.now().plusHours(24)
        );
    }
}
