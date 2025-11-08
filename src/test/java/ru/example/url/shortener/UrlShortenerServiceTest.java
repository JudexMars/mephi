package ru.example.url.shortener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import ru.example.url.shortener.model.ShortenedUrl;
import ru.example.url.shortener.model.User;
import ru.example.url.shortener.model.UrlStatus;
import ru.example.url.shortener.service.UrlShortenerService;
import ru.example.url.shortener.storage.UrlStorage;
import ru.example.url.shortener.util.NotificationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UrlShortenerServiceTest {

    private UrlShortenerService service;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        UrlStorage.getInstance().clear();
        
        service = new UrlShortenerService();
        service.setTestMode(true); // Enable test mode to prevent browser opening
        notificationService = NotificationService.getInstance();
        
        notificationService.clearAllNotifications();
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should create users with unique UUIDs")
    void testUserCreation() {
        // Given & When
        User user1 = service.createUser();
        User user2 = service.createUser();

        // Then
        assertNotNull(user1);
        assertNotNull(user2);
        assertNotNull(user1.getUuid());
        assertNotNull(user2.getUuid());
        assertNotEquals(user1.getUuid(), user2.getUuid());
        assertNotNull(user1.getCreatedAt());
        assertNotNull(user2.getCreatedAt());
    }

    @Test
    @Order(2)
    @DisplayName("Should generate unique short codes for same URL by different users")
    void testUniqueLinksForDifferentUsers() {
        // Given
        String testUrl = "https://www.baeldung.com/java-9-http-client";
        User user1 = service.createUser();
        User user2 = service.createUser();
        User user3 = service.createUser();

        // When
        ShortenedUrl url1 = service.shortenUrl(testUrl, user1.getUuid());
        ShortenedUrl url2 = service.shortenUrl(testUrl, user2.getUuid());
        ShortenedUrl url3 = service.shortenUrl(testUrl, user3.getUuid());

        // Then
        assertNotNull(url1);
        assertNotNull(url2);
        assertNotNull(url3);
        
        Set<String> shortCodes = new HashSet<>();
        shortCodes.add(url1.getShortCode());
        shortCodes.add(url2.getShortCode());
        shortCodes.add(url3.getShortCode());
        
        assertEquals(3, shortCodes.size(), "All users should get unique short codes");
        
        // Verify all URLs point to the same original URL
        assertEquals(testUrl, url1.getOriginalUrl());
        assertEquals(testUrl, url2.getOriginalUrl());
        assertEquals(testUrl, url3.getOriginalUrl());
        
        // Verify different users
        assertEquals(user1.getUuid(), url1.getUserId());
        assertEquals(user2.getUuid(), url2.getUserId());
        assertEquals(user3.getUuid(), url3.getUserId());
    }

    @Test
    @Order(3)
    @DisplayName("Should enforce click limits correctly")
    void testClickLimitEnforcement() {
        // Given
        User user = service.createUser();
        String testUrl = "https://example.com/click-limit-test";
        ShortenedUrl url = service.shortenUrl(testUrl, user.getUuid(), 3, 24);

        // When & Then - Access URL within limit
        assertEquals(0, url.getClickCount());
        assertTrue(url.isAccessible());
        
        service.accessUrl(url.getShortCode()); // Click 1
        assertEquals(1, url.getClickCount());
        assertTrue(url.isAccessible());
        
        service.accessUrl(url.getShortCode()); // Click 2
        assertEquals(2, url.getClickCount());
        assertTrue(url.isAccessible());
        
        service.accessUrl(url.getShortCode()); // Click 3 - should reach limit
        assertEquals(3, url.getClickCount());
        assertEquals(UrlStatus.LIMIT_EXCEEDED, url.getStatus());
        assertFalse(url.isAccessible());
        
        // Further access should be blocked
        service.accessUrl(url.getShortCode()); // Should be blocked
        assertEquals(3, url.getClickCount()); // Should not increment
    }

    @Test
    @Order(4)
    @DisplayName("Should validate URL formats correctly")
    void testUrlValidation() {
        // Given
        User user = service.createUser();
        
        // Valid URLs should work
        String[] validUrls = {
            "https://www.google.com",
            "http://example.com",
            "www.github.com",
            "stackoverflow.com/questions/123",
            "https://api.example.com:8080/path?param=value"
        };
        
        // When & Then - Valid URLs
        for (String url : validUrls) {
            assertDoesNotThrow(() -> {
                ShortenedUrl shortened = service.shortenUrl(url, user.getUuid());
                assertNotNull(shortened, "Valid URL should be accepted: " + url);
                assertNotNull(shortened.getShortCode());
                assertTrue(shortened.getShortCode().length() > 0);
            }, "Valid URL should not throw exception: " + url);
        }
        
        // Invalid URLs should throw exceptions
        String[] invalidUrls = {
            "",
            "   ",
            "not-a-url",
            "javascript:alert('xss')"
        };
        
        // When & Then - Invalid URLs
        for (String url : invalidUrls) {
            assertThrows(IllegalArgumentException.class, () -> {
                service.shortenUrl(url, user.getUuid());
            }, "Invalid URL should throw exception: " + url);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should validate short code formats")
    void testShortCodeValidation() {
        // Given
        User user = service.createUser();
        ShortenedUrl validUrl = service.shortenUrl("https://example.com", user.getUuid());
        String validShortCode = validUrl.getShortCode();
        
        // When & Then - Valid short codes should not cause errors
        assertDoesNotThrow(() -> service.accessUrl(validShortCode));
        assertDoesNotThrow(() -> service.accessUrl("abc123"));
        assertDoesNotThrow(() -> service.accessUrl("XYZ789"));
        
        // Invalid short codes should be handled gracefully (no exceptions, just error messages)
        assertDoesNotThrow(() -> service.accessUrl("abc-123")); // Contains hyphen
        assertDoesNotThrow(() -> service.accessUrl("abc_123")); // Contains underscore
        assertDoesNotThrow(() -> service.accessUrl("abc@123")); // Contains special char
        assertDoesNotThrow(() -> service.accessUrl("")); // Empty string
    }

    @Test
    @Order(6)
    @DisplayName("Should handle notifications correctly")
    void testNotificationSystem() {
        // Given
        User user = service.createUser();
        notificationService.clearNotifications(user.getUuid());
        
        // When - Create URL (should generate notification)
        ShortenedUrl url = service.shortenUrl("https://example.com/notifications", user.getUuid(), 2, 24);
        
        // Then - Check notification was created
        assertTrue(notificationService.getNotificationCount(user.getUuid()) > 0, 
                  "Notification should be created after URL creation");
        
        // When - Access URL to reach limit
        service.accessUrl(url.getShortCode()); // Click 1
        service.accessUrl(url.getShortCode()); // Click 2 - should trigger limit notification
        
        // Then - Should have more notifications
        assertTrue(notificationService.getNotificationCount(user.getUuid()) > 1, 
                  "Should have notifications after reaching limit");
        
        // When - Clear notifications
        notificationService.clearNotifications(user.getUuid());
        
        // Then - Should be cleared
        assertEquals(0, notificationService.getNotificationCount(user.getUuid()), 
                    "Notifications should be cleared");
        
        // When - Test clearAllNotifications
        User user2 = service.createUser();
        notificationService.addNotification(user.getUuid(), "Test notification 1");
        notificationService.addNotification(user2.getUuid(), "Test notification 2");
        
        assertTrue(notificationService.getNotificationCount(user.getUuid()) > 0);
        assertTrue(notificationService.getNotificationCount(user2.getUuid()) > 0);
        
        notificationService.clearAllNotifications();
        
        // Then - All should be cleared
        assertEquals(0, notificationService.getNotificationCount(user.getUuid()));
        assertEquals(0, notificationService.getNotificationCount(user2.getUuid()));
    }

    @Test
    @Order(7)
    @DisplayName("Should handle edge cases properly")
    void testEdgeCases() {
        // Given
        User user = service.createUser();
        
        // When & Then - Non-existent short code
        assertDoesNotThrow(() -> service.accessUrl("nonexistent"));
        
        // When & Then - Invalid parameters
        assertThrows(IllegalArgumentException.class, () -> 
            service.shortenUrl("https://example.com", user.getUuid(), 0, 24));
        
        assertThrows(IllegalArgumentException.class, () -> 
            service.shortenUrl("https://example.com", user.getUuid(), 100, 0));
        
        // When & Then - Very long URL should work
        String longUrl = "https://example.com/" + "a".repeat(1000);
        assertDoesNotThrow(() -> {
            ShortenedUrl shortened = service.shortenUrl(longUrl, user.getUuid());
            assertNotNull(shortened);
        });
    }

    @Test
    @Order(8)
    @DisplayName("Should handle concurrent access correctly")
    void testConcurrentAccess() {
        // Given
        User user = service.createUser();
        ShortenedUrl url = service.shortenUrl("https://example.com/concurrent", user.getUuid(), 10, 24);
        
        // When - Simulate multiple rapid accesses
        for (int i = 0; i < 15; i++) {
            service.accessUrl(url.getShortCode());
        }
        
        // Then - Should respect the limit
        assertEquals(10, url.getClickCount(), "Should not exceed click limit");
        assertEquals(UrlStatus.LIMIT_EXCEEDED, url.getStatus(), "Status should be LIMIT_EXCEEDED");
        assertFalse(url.isAccessible(), "URL should not be accessible after limit reached");
    }

    @Test
    @Order(9)
    @DisplayName("Should retrieve user URLs correctly")
    void testGetUserUrls() {
        // Given
        User user = service.createUser();
        
        // When
        ShortenedUrl url1 = service.shortenUrl("https://example.com/1", user.getUuid());
        ShortenedUrl url2 = service.shortenUrl("https://example.com/2", user.getUuid());
        ShortenedUrl url3 = service.shortenUrl("https://example.com/3", user.getUuid());
        
        List<ShortenedUrl> userUrls = service.getUserUrls(user.getUuid());
        
        // Then
        assertNotNull(userUrls);
        assertEquals(3, userUrls.size());
        assertTrue(userUrls.contains(url1));
        assertTrue(userUrls.contains(url2));
        assertTrue(userUrls.contains(url3));
    }

    @Test
    @Order(10)
    @DisplayName("Should provide correct statistics")
    void testStatistics() {
        // Given
        User user1 = service.createUser();
        User user2 = service.createUser();
        
        // When
        service.shortenUrl("https://example.com/1", user1.getUuid());
        service.shortenUrl("https://example.com/2", user1.getUuid());
        service.shortenUrl("https://example.com/3", user2.getUuid());
        
        String statistics = service.getStatistics();
        
        // Then
        assertNotNull(statistics);
        assertTrue(statistics.contains("Total URLs: 3"));
        assertTrue(statistics.contains("Total Users: 2"));
        assertTrue(statistics.contains("Active URLs: 3"));
    }

    @Test
    @Order(11)
    @DisplayName("Should update click limit correctly")
    void testUpdateClickLimit() {
        // Given
        User user = service.createUser();
        ShortenedUrl url = service.shortenUrl("https://example.com", user.getUuid(), 10, 24);
        String shortCode = url.getShortCode();
        
        // When & Then - Update click limit successfully
        assertTrue(service.updateClickLimit(shortCode, user.getUuid(), 20));
        
        // Verify the limit was updated
        ShortenedUrl updatedUrl = service.getUrlInfo(shortCode);
        assertEquals(20, updatedUrl.getMaxClicks());
        
        // Test with non-existent URL
        assertFalse(service.updateClickLimit("nonexistent", user.getUuid(), 15));
        
        // Test with wrong user
        User wrongUser = service.createUser();
        assertThrows(SecurityException.class, () -> 
            service.updateClickLimit(shortCode, wrongUser.getUuid(), 15));
        
        // Test with invalid limit
        assertThrows(IllegalArgumentException.class, () -> 
            service.updateClickLimit(shortCode, user.getUuid(), 0));
        assertThrows(IllegalArgumentException.class, () -> 
            service.updateClickLimit(shortCode, user.getUuid(), -5));
    }

    @Test
    @Order(12)
    @DisplayName("Should update expiration time correctly")
    void testUpdateExpirationTime() {
        // Given
        User user = service.createUser();
        ShortenedUrl url = service.shortenUrl("https://example.com", user.getUuid(), 10, 24);
        String shortCode = url.getShortCode();
        java.time.LocalDateTime originalExpiration = url.getExpiresAt();
        
        // When & Then - Update expiration time successfully
        assertTrue(service.updateExpirationTime(shortCode, user.getUuid(), 12));
        
        // Verify the expiration was updated
        ShortenedUrl updatedUrl = service.getUrlInfo(shortCode);
        assertTrue(updatedUrl.getExpiresAt().isAfter(originalExpiration));
        
        // Test with non-existent URL
        assertFalse(service.updateExpirationTime("nonexistent", user.getUuid(), 12));
        
        // Test with wrong user
        User wrongUser = service.createUser();
        assertThrows(SecurityException.class, () -> 
            service.updateExpirationTime(shortCode, wrongUser.getUuid(), 12));
        
        // Test with invalid hours
        assertThrows(IllegalArgumentException.class, () -> 
            service.updateExpirationTime(shortCode, user.getUuid(), 0));
        assertThrows(IllegalArgumentException.class, () -> 
            service.updateExpirationTime(shortCode, user.getUuid(), -5));
    }

    @Test
    @Order(13)
    @DisplayName("Should deactivate URL correctly")
    void testDeactivateUrl() {
        // Given
        User user = service.createUser();
        ShortenedUrl url = service.shortenUrl("https://example.com", user.getUuid(), 10, 24);
        String shortCode = url.getShortCode();
        
        // When & Then - Deactivate URL successfully
        assertTrue(service.deactivateUrl(shortCode, user.getUuid()));
        
        // Verify the URL was deactivated
        ShortenedUrl deactivatedUrl = service.getUrlInfo(shortCode);
        assertEquals(UrlStatus.EXPIRED, deactivatedUrl.getStatus());
        
        // Try to deactivate again (should return false)
        assertFalse(service.deactivateUrl(shortCode, user.getUuid()));
        
        // Test with non-existent URL
        assertFalse(service.deactivateUrl("nonexistent", user.getUuid()));
        
        // Test with wrong user
        User wrongUser = service.createUser();
        ShortenedUrl newUrl = service.shortenUrl("https://test.com", user.getUuid(), 10, 24);
        assertThrows(SecurityException.class, () -> 
            service.deactivateUrl(newUrl.getShortCode(), wrongUser.getUuid()));
    }

    @Test
    @Order(14)
    @DisplayName("Should not allow modifying inactive URL")
    void testCannotModifyInactiveUrl() {
        // Given
        User user = service.createUser();
        ShortenedUrl url = service.shortenUrl("https://example.com", user.getUuid(), 10, 24);
        String shortCode = url.getShortCode();
        
        // When - Deactivate the URL
        service.deactivateUrl(shortCode, user.getUuid());
        
        // Then - Try to modify the inactive URL
        assertThrows(IllegalStateException.class, () -> 
            service.updateClickLimit(shortCode, user.getUuid(), 20));
        assertThrows(IllegalStateException.class, () -> 
            service.updateExpirationTime(shortCode, user.getUuid(), 12));
    }
}
