package ru.example.url.shortener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.example.url.shortener.service.UrlGenerator;
import ru.example.url.shortener.storage.UrlStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for URL Generator
 */
class UrlGeneratorTest {

    private UrlGenerator urlGenerator;

    @BeforeEach
    void setUp() {
        UrlStorage storage = UrlStorage.getInstance();
        storage.clear();
        urlGenerator = new UrlGenerator(storage);
    }

    @Test
    @DisplayName("Should generate valid short codes")
    void testGenerateValidShortCodes() {
        // Given
        String url = "https://example.com";
        UUID userId = UUID.randomUUID();

        // When
        String shortCode = urlGenerator.generateShortCode(url, userId);

        // Then
        assertNotNull(shortCode);
        assertFalse(shortCode.isEmpty());
        assertEquals(7, shortCode.length(), "Short code should be 7 characters long");
        assertTrue(urlGenerator.isValidShortCode(shortCode), "Generated code should be valid");
    }

    @Test
    @DisplayName("Should generate unique codes for different users")
    void testUniqueCodesForDifferentUsers() {
        // Given
        String url = "https://example.com";
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();

        // When
        String code1 = urlGenerator.generateShortCode(url, user1);
        String code2 = urlGenerator.generateShortCode(url, user2);
        String code3 = urlGenerator.generateShortCode(url, user3);

        // Then
        Set<String> codes = new HashSet<>();
        codes.add(code1);
        codes.add(code2);
        codes.add(code3);

        assertEquals(3, codes.size(), "All codes should be unique");
    }

    @Test
    @DisplayName("Should generate unique codes for same user with different URLs")
    void testUniqueCodesForDifferentUrls() {
        // Given
        UUID userId = UUID.randomUUID();
        String url1 = "https://example.com/page1";
        String url2 = "https://example.com/page2";
        String url3 = "https://different-site.com";

        // When
        String code1 = urlGenerator.generateShortCode(url1, userId);
        String code2 = urlGenerator.generateShortCode(url2, userId);
        String code3 = urlGenerator.generateShortCode(url3, userId);

        // Then
        Set<String> codes = new HashSet<>();
        codes.add(code1);
        codes.add(code2);
        codes.add(code3);

        assertEquals(3, codes.size(), "All codes should be unique for different URLs");
    }

    @Test
    @DisplayName("Should validate short code formats correctly")
    void testShortCodeValidation() {
        // Valid short codes
        String[] validCodes = {
            "abc123",
            "XYZ789",
            "aB3xY7z",
            "1234567",
            "abcdefg",
            "ABCDEFG",
            "a1B2c3D"
        };

        for (String code : validCodes) {
            assertTrue(urlGenerator.isValidShortCode(code), 
                      "Should be valid: " + code);
        }

        // Invalid short codes
        String[] invalidCodes = {
            "abc-123",      // Contains hyphen
            "abc_123",      // Contains underscore
            "abc@123",      // Contains special character
            "abc 123",      // Contains space
            "abc#123",      // Contains hash
            "",             // Empty string
            null            // Null
        };

        for (String code : invalidCodes) {
            assertFalse(urlGenerator.isValidShortCode(code), 
                       "Should be invalid: " + code);
        }
    }

    @Test
    @DisplayName("Should handle collision resolution")
    void testCollisionResolution() {
        // This test is harder to implement reliably since we can't easily force collisions
        // But we can test that the generator handles multiple requests without issues
        
        // Given
        UUID userId = UUID.randomUUID();
        String baseUrl = "https://example.com/test";
        Set<String> generatedCodes = new HashSet<>();

        // When - Generate many codes
        for (int i = 0; i < 100; i++) {
            String url = baseUrl + i;
            String code = urlGenerator.generateShortCode(url, userId);
            generatedCodes.add(code);
        }

        // Then - All codes should be unique
        assertEquals(100, generatedCodes.size(), 
                    "All generated codes should be unique");
    }

    @Test
    @DisplayName("Should generate consistent length codes")
    void testConsistentLength() {
        // Given
        UUID userId = UUID.randomUUID();
        String[] testUrls = {
            "https://short.com",
            "https://very-long-domain-name-with-many-subdomains.example.com/very/long/path/with/many/segments",
            "https://example.com/path?param1=value1&param2=value2&param3=value3",
            "https://unicode-тест.com/путь",
            "https://numbers123.com/456/789"
        };

        // When & Then
        for (String url : testUrls) {
            String code = urlGenerator.generateShortCode(url, userId);
            assertEquals(7, code.length(), 
                        "Code length should always be 7 for URL: " + url);
            assertTrue(urlGenerator.isValidShortCode(code), 
                      "Generated code should be valid for URL: " + url);
        }
    }

    @Test
    @DisplayName("Should handle edge case inputs")
    void testEdgeCaseInputs() {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then - Very long URL
        String longUrl = "https://example.com/" + "a".repeat(1000);
        assertDoesNotThrow(() -> {
            String code = urlGenerator.generateShortCode(longUrl, userId);
            assertNotNull(code);
            assertEquals(7, code.length());
        });

        // When & Then - URL with special characters
        String specialUrl = "https://example.com/path?query=test&other=value#anchor";
        assertDoesNotThrow(() -> {
            String code = urlGenerator.generateShortCode(specialUrl, userId);
            assertNotNull(code);
            assertEquals(7, code.length());
        });

        // When & Then - Minimal URL
        String minimalUrl = "a.co";
        assertDoesNotThrow(() -> {
            String code = urlGenerator.generateShortCode(minimalUrl, userId);
            assertNotNull(code);
            assertEquals(7, code.length());
        });
    }

    @Test
    @DisplayName("Should generate different codes at different times")
    void testTimeBasedUniqueness() throws InterruptedException {
        // Given
        UUID userId = UUID.randomUUID();
        String url = "https://example.com";

        // When
        String code1 = urlGenerator.generateShortCode(url, userId);
        Thread.sleep(1); // Ensure different timestamp
        String code2 = urlGenerator.generateShortCode(url, userId);

        // Then
        assertNotEquals(code1, code2, 
                       "Codes generated at different times should be different");
    }
}
