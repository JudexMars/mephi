package ru.example.finances.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;

class AuthenticationServiceTest {
    
    private AuthenticationService authService;
    private static final String TEST_DATA_DIR = "test_data";
    
    @BeforeEach
    void setUp() {
        // Set test data directory
        System.setProperty("test.data.dir", TEST_DATA_DIR);
        
        // Clean up any existing test data before each test
        cleanupTestData();
        authService = new AuthenticationService();
    }
    
    @AfterEach
    void tearDown() {
        authService.logout();
        cleanupTestData();
        // Clear test data directory property
        System.clearProperty("test.data.dir");
    }
    
    private static void cleanupTestData() {
        // Clean up data directory
        File dataDir = new File(TEST_DATA_DIR);
        if (dataDir.exists()) {
            File[] files = dataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dataDir.delete();
        }
        
        // Also clean up default data directory
        File defaultDataDir = new File("data");
        if (defaultDataDir.exists()) {
            File[] files = defaultDataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            defaultDataDir.delete();
        }
    }
    
    @Test
    void testUserRegistration() {
        var result = authService.register("testuser", "password123", "test@example.com");
        
        assertTrue(result.isSuccess());
        assertTrue(authService.userExists("testuser"));
    }
    
    @Test
    void testUserRegistrationWithExistingUsername() {
        authService.register("testuser", "password123", "test@example.com");
        
        var result = authService.register("testuser", "differentpassword", "different@example.com");
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    void testUserRegistrationWithInvalidData() {
        // Test null username
        var result1 = authService.register(null, "password", "email@test.com");
        assertFalse(result1.isSuccess());
        
        // Test empty username
        var result2 = authService.register("", "password", "email@test.com");
        assertFalse(result2.isSuccess());
        
        // Test null password
        var result3 = authService.register("username", null, "email@test.com");
        assertFalse(result3.isSuccess());
        
        // Test empty password
        var result4 = authService.register("username", "", "email@test.com");
        assertFalse(result4.isSuccess());
        
        // Test invalid email
        var result5 = authService.register("username", "password", "invalid-email");
        assertFalse(result5.isSuccess());
    }
    
    @Test
    void testUserLogin() {
        authService.register("testuser", "password123", "test@example.com");
        
        var result = authService.login("testuser", "password123");
        
        assertTrue(result.isSuccess());
        assertTrue(authService.isLoggedIn());
        assertEquals("testuser", authService.getCurrentUsername());
    }
    
    @Test
    void testUserLoginWithWrongPassword() {
        authService.register("testuser", "password123", "test@example.com");
        
        var result = authService.login("testuser", "wrongpassword");
        
        assertFalse(result.isSuccess());
        assertFalse(authService.isLoggedIn());
        assertNull(authService.getCurrentUsername());
    }
    
    @Test
    void testUserLoginWithNonExistentUser() {
        var result = authService.login("nonexistent", "password");
        
        assertFalse(result.isSuccess());
        assertFalse(authService.isLoggedIn());
        assertNull(authService.getCurrentUsername());
    }
    
    @Test
    void testUserLogout() {
        authService.register("testuser", "password123", "test@example.com");
        authService.login("testuser", "password123");
        
        assertTrue(authService.isLoggedIn());
        assertEquals("testuser", authService.getCurrentUsername());
        
        authService.logout();
        
        assertFalse(authService.isLoggedIn());
        assertNull(authService.getCurrentUsername());
    }
    
    @Test
    void testUserExists() {
        assertFalse(authService.userExists("testuser"));
        
        authService.register("testuser", "password123", "test@example.com");
        
        assertTrue(authService.userExists("testuser"));
        assertFalse(authService.userExists("nonexistent"));
    }
    
    @Test
    void testMultipleUsersRegistration() {
        authService.register("user1", "password1", "user1@example.com");
        authService.register("user2", "password2", "user2@example.com");
        authService.register("user3", "password3", "user3@example.com");
        
        assertTrue(authService.userExists("user1"));
        assertTrue(authService.userExists("user2"));
        assertTrue(authService.userExists("user3"));
        
        // Test login with different users
        var result1 = authService.login("user1", "password1");
        assertTrue(result1.isSuccess());
        assertEquals("user1", authService.getCurrentUsername());
        
        authService.logout();
        
        var result2 = authService.login("user2", "password2");
        assertTrue(result2.isSuccess());
        assertEquals("user2", authService.getCurrentUsername());
    }
    
    @Test
    void testLoginWithoutLogout() {
        authService.register("user1", "password1", "user1@example.com");
        authService.register("user2", "password2", "user2@example.com");
        
        authService.login("user1", "password1");
        assertEquals("user1", authService.getCurrentUsername());
        
        // Login with different user should work (implicit logout)
        var result = authService.login("user2", "password2");
        assertTrue(result.isSuccess());
        assertEquals("user2", authService.getCurrentUsername());
    }
    
    @Test
    void testPasswordHashing() {
        authService.register("testuser", "password123", "test@example.com");
        
        // Password should be hashed, not stored in plain text
        // We can't directly test the hash, but we can test that login works
        assertTrue(authService.login("testuser", "password123").isSuccess());
        assertFalse(authService.login("testuser", "wrongpassword").isSuccess());
    }
    
    @Test
    void testCaseSensitiveUsernames() {
        authService.register("TestUser", "password123", "test@example.com");
        
        assertTrue(authService.userExists("TestUser"));
        assertFalse(authService.userExists("testuser"));
        assertFalse(authService.userExists("TESTUSER"));
        
        assertTrue(authService.login("TestUser", "password123").isSuccess());
        assertFalse(authService.login("testuser", "password123").isSuccess());
    }
}
