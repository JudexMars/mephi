package ru.example.finances.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * Utility class for input validation and password hashing.
 * Provides methods to validate user input and secure password handling.
 */
public class ValidationUtils {
    
    // Regex patterns for validation
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("^[a-zA-Zа-яА-Я0-9\\s_-]{1,50}$");

    /**
     * Validates a username.
     * @param username the username to validate
     * @return true if valid (3-20 characters, alphanumeric and underscore only)
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validates a password.
     * @param password the password to validate
     * @return true if valid (at least 6 characters)
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Validates an email address.
     * @param email the email to validate
     * @return true if valid email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates a category name.
     * @param category the category name to validate
     * @return true if valid (1-50 characters, letters, numbers, spaces, underscore, hyphen)
     */
    public static boolean isValidCategory(String category) {
        return category != null && CATEGORY_PATTERN.matcher(category).matches();
    }

    /**
     * Validates an amount (must be positive).
     * @param amount the amount to validate
     * @return true if positive
     */
    public static boolean isValidAmount(double amount) {
        return amount > 0 && !Double.isNaN(amount) && !Double.isInfinite(amount);
    }

    /**
     * Validates a budget limit (must be non-negative).
     * @param limit the budget limit to validate
     * @return true if non-negative
     */
    public static boolean isValidBudgetLimit(double limit) {
        return limit >= 0 && !Double.isNaN(limit) && !Double.isInfinite(limit);
    }

    /**
     * Validates that a string is not null or empty.
     * @param str the string to validate
     * @return true if not null and not empty after trimming
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Validates that a string is within the specified length limits.
     * @param str the string to validate
     * @param minLength minimum length (inclusive)
     * @param maxLength maximum length (inclusive)
     * @return true if within length limits
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) return false;
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Hashes a password using SHA-256.
     * @param password the password to hash
     * @return the hashed password as a hex string
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a password against a hash.
     * @param password the plain text password
     * @param hash the stored hash
     * @return true if the password matches the hash
     */
    public static boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }

    /**
     * Sanitizes a string by trimming whitespace and limiting length.
     * @param input the input string
     * @param maxLength maximum allowed length
     * @return sanitized string
     */
    public static String sanitizeInput(String input, int maxLength) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    /**
     * Validates a numeric string and parses it to double.
     * @param str the string to parse
     * @return the parsed double, or null if invalid
     */
    public static Double parseAmount(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        
        try {
            double amount = Double.parseDouble(str.trim());
            if (isValidAmount(amount)) {
                return amount;
            }
        } catch (NumberFormatException e) {
            // Invalid number format
        }
        
        return null;
    }

    /**
     * Validates a budget limit string and parses it to double.
     * @param str the string to parse
     * @return the parsed double, or null if invalid
     */
    public static Double parseBudgetLimit(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        
        try {
            double limit = Double.parseDouble(str.trim());
            if (isValidBudgetLimit(limit)) {
                return limit;
            }
        } catch (NumberFormatException e) {
            // Invalid number format
        }
        
        return null;
    }
}
