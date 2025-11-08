package ru.example.url.shortener.util;

import ru.example.url.shortener.model.ShortenedUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NotificationService {
    private static NotificationService instance;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedQueue<String>> userNotifications;

    private NotificationService() {
        this.userNotifications = new ConcurrentHashMap<>();
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Adds a notification for a specific user
     */
    public void addNotification(UUID userId, String message) {
        userNotifications.computeIfAbsent(userId, _ -> new ConcurrentLinkedQueue<>()).offer(message);
    }

    /**
     * Gets all pending notifications for a user and clears them
     */
    public List<String> getAndClearNotifications(UUID userId) {
        ConcurrentLinkedQueue<String> notifications = userNotifications.get(userId);
        List<String> result = new ArrayList<>();
        
        if (notifications != null) {
            String notification;
            while ((notification = notifications.poll()) != null) {
                result.add(notification);
            }
        }
        
        return result;
    }

    /**
     * Gets pending notifications count for a user
     */
    public int getNotificationCount(UUID userId) {
        ConcurrentLinkedQueue<String> notifications = userNotifications.get(userId);
        return notifications != null ? notifications.size() : 0;
    }

    /**
     * Notifies user about URL expiration
     */
    public void notifyUrlExpired(ShortenedUrl url) {
        String message = String.format(
                """
                        ⚠️  Your shortened URL '%s' has expired and is no longer accessible.
                           Original URL: %s
                           Created: %s
                           Clicks used: %d/%d""",
            url.getFullShortUrl(),
            url.getOriginalUrl(),
            url.getCreatedAt().toString(),
            url.getClickCount(),
            url.getMaxClicks()
        );
        addNotification(url.getUserId(), message);
    }

    /**
     * Notifies user about click limit being reached
     */
    public void notifyClickLimitReached(ShortenedUrl url) {
        String message = String.format(
                """
                        🚫 Your shortened URL '%s' has reached its click limit and is no longer accessible.
                           Original URL: %s
                           Click limit: %d clicks
                           Created: %s""",
            url.getFullShortUrl(),
            url.getOriginalUrl(),
            url.getMaxClicks(),
            url.getCreatedAt().toString()
        );
        addNotification(url.getUserId(), message);
    }

    /**
     * Notifies user about URL access attempt when it's not accessible
     */
    public void notifyUrlNotAccessible(ShortenedUrl url, String reason) {
        String message = String.format(
                """
                        ❌ Access denied to '%s': %s
                           Original URL: %s
                           Status: %s
                           Clicks: %d/%d""",
            url.getFullShortUrl(),
            reason,
            url.getOriginalUrl(),
            url.getStatus(),
            url.getClickCount(),
            url.getMaxClicks()
        );
        addNotification(url.getUserId(), message);
    }

    /**
     * Notifies user about successful URL creation
     */
    public void notifyUrlCreated(ShortenedUrl url) {
        String message = String.format(
                """
                        ✅ Short URL created successfully!
                           Short URL: %s
                           Original URL: %s
                           Click limit: %d
                           Expires: %s""",
            url.getFullShortUrl(),
            url.getOriginalUrl(),
            url.getMaxClicks(),
            url.getExpiresAt().toString()
        );
        addNotification(url.getUserId(), message);
    }

    /**
     * Displays all pending notifications for a user
     */
    public void displayNotifications(UUID userId) {
        List<String> notifications = getAndClearNotifications(userId);
        if (!notifications.isEmpty()) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📢 NOTIFICATIONS");
            System.out.println("=".repeat(60));
            
            for (String notification : notifications) {
                System.out.println(notification);
                System.out.println("-".repeat(60));
            }
            System.out.println();
        }
    }

    /**
     * Checks if user has pending notifications
     */
    public boolean hasNotifications(UUID userId) {
        return getNotificationCount(userId) > 0;
    }

    /**
     * Clears all notifications for a user
     */
    public void clearNotifications(UUID userId) {
        ConcurrentLinkedQueue<String> notifications = userNotifications.get(userId);
        if (notifications != null) {
            notifications.clear();
        }
    }

    /**
     * Clears all notifications for all users
     */
    public void clearAllNotifications() {
        userNotifications.clear();
    }
}
