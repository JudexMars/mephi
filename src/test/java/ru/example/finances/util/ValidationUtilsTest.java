package ru.example.finances.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationUtils class.
 */
class ValidationUtilsTest {
    
    @Test
    void testIsValidAmount() {
        // Valid amounts
        assertTrue(ValidationUtils.isValidAmount(0.01));
        assertTrue(ValidationUtils.isValidAmount(1.0));
        assertTrue(ValidationUtils.isValidAmount(100.0));
        assertTrue(ValidationUtils.isValidAmount(1000000.0));
        assertTrue(ValidationUtils.isValidAmount(99.99));
        
        // Invalid amounts
        assertFalse(ValidationUtils.isValidAmount(0.0));
        assertFalse(ValidationUtils.isValidAmount(-0.01));
        assertFalse(ValidationUtils.isValidAmount(-1.0));
        assertFalse(ValidationUtils.isValidAmount(-100.0));
        assertFalse(ValidationUtils.isValidAmount(Double.NaN));
        assertFalse(ValidationUtils.isValidAmount(Double.POSITIVE_INFINITY));
        assertFalse(ValidationUtils.isValidAmount(Double.NEGATIVE_INFINITY));
    }
    
    @Test
    void testIsValidEmail() {
        // Valid emails
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name@domain.co.uk"));
        assertTrue(ValidationUtils.isValidEmail("user+tag@example.org"));
        assertTrue(ValidationUtils.isValidEmail("firstname.lastname@company.com"));
        assertTrue(ValidationUtils.isValidEmail("user123@test-domain.com"));
        assertTrue(ValidationUtils.isValidEmail("a@b.co"));
        
        // Invalid emails
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail("   "));
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
        assertFalse(ValidationUtils.isValidEmail("user@"));
        assertFalse(ValidationUtils.isValidEmail("user@.com"));
        // Note: Some of these might be valid according to the regex, so we test what actually fails
        assertFalse(ValidationUtils.isValidEmail("user name@example.com")); // Space not allowed
        assertFalse(ValidationUtils.isValidEmail("user@exam ple.com")); // Space not allowed
    }
    
    @Test
    void testIsValidUsername() {
        // Valid usernames (3-20 characters, alphanumeric and underscore only)
        assertTrue(ValidationUtils.isValidUsername("user"));
        assertTrue(ValidationUtils.isValidUsername("user123"));
        assertTrue(ValidationUtils.isValidUsername("test_user"));
        assertTrue(ValidationUtils.isValidUsername("User"));
        assertTrue(ValidationUtils.isValidUsername("USER"));
        assertTrue(ValidationUtils.isValidUsername("abc")); // Min length
        assertTrue(ValidationUtils.isValidUsername("a".repeat(20))); // Max length
        
        // Invalid usernames
        assertFalse(ValidationUtils.isValidUsername(null));
        assertFalse(ValidationUtils.isValidUsername(""));
        assertFalse(ValidationUtils.isValidUsername("   "));
        assertFalse(ValidationUtils.isValidUsername("ab")); // Too short (< 3)
        assertFalse(ValidationUtils.isValidUsername("user-name")); // Hyphen not allowed
        assertFalse(ValidationUtils.isValidUsername(" user"));
        assertFalse(ValidationUtils.isValidUsername("user "));
        assertFalse(ValidationUtils.isValidUsername("user name"));
        assertFalse(ValidationUtils.isValidUsername("user@name"));
        assertFalse(ValidationUtils.isValidUsername("user#name"));
        assertFalse(ValidationUtils.isValidUsername("user$name"));
        assertFalse(ValidationUtils.isValidUsername("a".repeat(21))); // Too long (> 20)
    }
    
    @Test
    void testIsValidPassword() {
        // Valid passwords (at least 6 characters)
        assertTrue(ValidationUtils.isValidPassword("password"));
        assertTrue(ValidationUtils.isValidPassword("password123"));
        assertTrue(ValidationUtils.isValidPassword("Password123"));
        assertTrue(ValidationUtils.isValidPassword("P@ssw0rd!"));
        assertTrue(ValidationUtils.isValidPassword("123456")); // Min length
        assertTrue(ValidationUtils.isValidPassword("a".repeat(100))); // Long password
        assertTrue(ValidationUtils.isValidPassword("пароль123")); // Non-ASCII
        assertTrue(ValidationUtils.isValidPassword("      ")); // 6 spaces is valid (>= 6 chars)
        
        // Invalid passwords
        assertFalse(ValidationUtils.isValidPassword(null));
        assertFalse(ValidationUtils.isValidPassword(""));
        assertFalse(ValidationUtils.isValidPassword("short")); // 5 chars, too short
        assertFalse(ValidationUtils.isValidPassword("12345")); // 5 chars, too short
        assertFalse(ValidationUtils.isValidPassword("     ")); // 5 spaces, too short
    }
    
    @Test
    void testValidationUtilsBasicFunctionality() {
        // Test that ValidationUtils class exists and basic methods work
        assertTrue(ValidationUtils.isValidAmount(100.0));
        assertFalse(ValidationUtils.isValidAmount(-100.0));
        
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        
        assertTrue(ValidationUtils.isValidUsername("validuser"));
        assertFalse(ValidationUtils.isValidUsername(""));
        
        assertTrue(ValidationUtils.isValidPassword("password123"));
        assertFalse(ValidationUtils.isValidPassword("short"));
    }
}
