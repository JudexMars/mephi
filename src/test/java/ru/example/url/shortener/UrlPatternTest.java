package ru.example.url.shortener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class UrlPatternTest {
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?" +                     // Protocol (optional)
        "(?!-)" +                             // Domain cannot start with hyphen
        "(" +
            "([\\w]([\\w\\-]*[\\w])?\\.)+[\\w]([\\w\\-]*[\\w])?" + // Multi-part domain (e.g., example.com)
            "|" +
            "localhost" +                     // Only allow localhost as single-part domain
        ")" +
        "(?<!-)" +                            // Domain cannot end with hyphen
        "(:[1-9][0-9]*)?" +                   // Port (optional, must be positive number)
        "(/[\\w\\-._~:/?#\\[\\]@!&'()*+,;=]*)?" + // Path (optional)
        "$"                                   // End of string
    );

    @Test
    @DisplayName("Should validate basic URLs correctly")
    void testBasicUrls() {
        // Valid basic URLs
        assertTrue(URL_PATTERN.matcher("https://www.google.com").matches());
        assertTrue(URL_PATTERN.matcher("http://example.com").matches());
        assertTrue(URL_PATTERN.matcher("www.github.com").matches());
        assertTrue(URL_PATTERN.matcher("stackoverflow.com").matches());
        assertTrue(URL_PATTERN.matcher("test-site.co.uk").matches());
    }

    @Test
    @DisplayName("Should validate URLs with ports")
    void testUrlsWithPorts() {
        assertTrue(URL_PATTERN.matcher("api.example.com:8080").matches());
        assertTrue(URL_PATTERN.matcher("https://localhost:3000").matches());
        assertTrue(URL_PATTERN.matcher("http://server.com:443").matches());
        assertTrue(URL_PATTERN.matcher("test.local:9999").matches());
    }

    @Test
    @DisplayName("Should validate URLs with paths and parameters")
    void testUrlsWithPathsAndParams() {
        assertTrue(URL_PATTERN.matcher("https://example.com/path/to/resource").matches());
        assertTrue(URL_PATTERN.matcher("http://site.com/path?param=value&other=123").matches());
        assertTrue(URL_PATTERN.matcher("https://sub.domain.com:443/complex/path?query=test").matches());
        assertTrue(URL_PATTERN.matcher("example.com/simple-path").matches());
        assertTrue(URL_PATTERN.matcher("site.com/path_with_underscores").matches());
        assertTrue(URL_PATTERN.matcher("example.org/path~with~tildes").matches());
        assertTrue(URL_PATTERN.matcher("test.net/path(with)parentheses").matches());
        assertTrue(URL_PATTERN.matcher("site.com/path[with]brackets").matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://www.google.com",
        "http://example.com", 
        "www.github.com",
        "stackoverflow.com/questions/123",
        "https://api.example.com:8080/path?param=value",
        "test-site.co.uk",
        "my-site.com/path_with_underscores",
        "site.com/path~with~tildes",
        "example.org/path(with)parentheses",
        "test.net/path[with]brackets",
        "site.com/path?query=test&other=value",
        "https://sub.domain.example.com/deep/path/structure"
    })
    @DisplayName("Should accept valid URLs")
    void testValidUrls(String url) {
        assertTrue(URL_PATTERN.matcher(url).matches(), 
                  "Should accept valid URL: " + url);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "not-a-url",
        "ftp://invalid-protocol.com",
        "javascript:alert('xss')",
        "just-text-without-domain",
        "http://",
        "https://",
        ".com",
        "http://.com",
        "spaces in url.com",
        "invalid..domain.com",
        "http://invalid-.com",
        "https://-invalid.com"
    })
    @DisplayName("Should reject invalid URLs")
    void testInvalidUrls(String url) {
        assertFalse(URL_PATTERN.matcher(url).matches(), 
                   "Should reject invalid URL: " + url);
    }

    @Test
    @DisplayName("Should handle special characters in paths")
    void testSpecialCharactersInPaths() {
        // Test various special characters that should be allowed in paths
        assertTrue(URL_PATTERN.matcher("example.com/path-with-hyphens").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path_with_underscores").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path.with.dots").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path~with~tildes").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path:with:colons").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path?query=value").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path#anchor").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path[with]brackets").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path@symbol").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path!exclamation").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path&ampersand").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path'apostrophe").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path(with)parentheses").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path*asterisk").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path+plus").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path,comma").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path;semicolon").matches());
        assertTrue(URL_PATTERN.matcher("example.com/path=equals").matches());
    }

    @Test
    @DisplayName("Should validate domain formats")
    void testDomainFormats() {
        // Valid domain formats
        assertTrue(URL_PATTERN.matcher("a.co").matches());
        assertTrue(URL_PATTERN.matcher("test.example.com").matches());
        assertTrue(URL_PATTERN.matcher("sub.domain.example.co.uk").matches());
        assertTrue(URL_PATTERN.matcher("very-long-subdomain.example.com").matches());
        assertTrue(URL_PATTERN.matcher("test123.example456.com").matches());
        
        // Invalid domain formats
        assertFalse(URL_PATTERN.matcher("invalid-.com").matches());
        assertFalse(URL_PATTERN.matcher("-invalid.com").matches());
        assertFalse(URL_PATTERN.matcher("invalid..com").matches());
        assertFalse(URL_PATTERN.matcher(".invalid.com").matches());
        assertFalse(URL_PATTERN.matcher("invalid.").matches());
    }

    @Test
    @DisplayName("Should validate protocol formats")
    void testProtocolFormats() {
        // Valid protocols
        assertTrue(URL_PATTERN.matcher("http://example.com").matches());
        assertTrue(URL_PATTERN.matcher("https://example.com").matches());
        
        // No protocol should also work
        assertTrue(URL_PATTERN.matcher("example.com").matches());
        
        // Invalid protocols should fail
        assertFalse(URL_PATTERN.matcher("ftp://example.com").matches());
        assertFalse(URL_PATTERN.matcher("file://example.com").matches());
        assertFalse(URL_PATTERN.matcher("javascript:alert()").matches());
    }

    @Test
    @DisplayName("Should validate port numbers")
    void testPortNumbers() {
        // Valid ports
        assertTrue(URL_PATTERN.matcher("example.com:80").matches());
        assertTrue(URL_PATTERN.matcher("example.com:443").matches());
        assertTrue(URL_PATTERN.matcher("example.com:8080").matches());
        assertTrue(URL_PATTERN.matcher("example.com:3000").matches());
        assertTrue(URL_PATTERN.matcher("example.com:65535").matches());
        
        // Invalid port formats
        assertFalse(URL_PATTERN.matcher("example.com:").matches());
        assertFalse(URL_PATTERN.matcher("example.com:abc").matches());
        assertFalse(URL_PATTERN.matcher("example.com:-80").matches());
        assertFalse(URL_PATTERN.matcher("example.com:0").matches()); // Port 0 is invalid
    }

    @Test
    @DisplayName("Should handle edge cases")
    void testEdgeCases() {
        // Minimal valid URL
        assertTrue(URL_PATTERN.matcher("a.co").matches());
        
        // Long but valid URL
        String longDomain = "very-long-subdomain-name-that-is-still-valid.example.com";
        assertTrue(URL_PATTERN.matcher(longDomain).matches());
        
        // URL with all components
        String complexUrl = "https://sub.example.com:8080/path/to/resource?param1=value1&param2=value2#anchor";
        assertTrue(URL_PATTERN.matcher(complexUrl).matches());
        
        // Empty path should work
        assertTrue(URL_PATTERN.matcher("https://example.com/").matches());
    }
}
