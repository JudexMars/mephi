package ru.example.url.shortener.service;

import ru.example.url.shortener.storage.UrlStorage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UrlGenerator {
    private static final String BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;
    private final UrlStorage storage;

    public UrlGenerator(UrlStorage storage) {
        this.storage = storage;
    }

    /**
     * Generates a unique short code for the given URL and user
     * Ensures uniqueness by combining user UUID, original URL, and timestamp
     */
    public String generateShortCode(String originalUrl, UUID userId) {
        String input = userId.toString() + originalUrl + System.currentTimeMillis();
        String hash = generateMD5Hash(input);
        String shortCode = encodeToBase62(hash).substring(0, SHORT_CODE_LENGTH);
        
        // Ensure uniqueness by checking against existing codes
        int attempts = 0;
        while (storage.isShortCodeExists(shortCode) && attempts < 10) {
            input += attempts; // Add attempt number to make it different
            hash = generateMD5Hash(input);
            shortCode = encodeToBase62(hash).substring(0, SHORT_CODE_LENGTH);
            attempts++;
        }
        
        if (attempts >= 10) {
            // Fallback to random generation if collision persists
            shortCode = generateRandomCode();
        }
        
        return shortCode;
    }

    /**
     * Generates MD5 hash of the input string
     */
    private static String generateMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Encodes a hexadecimal string to Base62
     */
    private static String encodeToBase62(String hex) {
        // Convert hex to long (using first 15 characters to avoid overflow)
        String truncatedHex = hex.length() > 15 ? hex.substring(0, 15) : hex;
        long number = Long.parseUnsignedLong(truncatedHex, 16);
        
        if (number == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }
        
        StringBuilder result = new StringBuilder();
        while (number > 0) {
            result.append(BASE62_CHARS.charAt((int) (number % 62)));
            number /= 62;
        }
        
        return result.reverse().toString();
    }

    /**
     * Generates a random Base62 code as fallback
     */
    private static String generateRandomCode() {
        return IntStream.range(0, SHORT_CODE_LENGTH)
                .map(i -> (int) (Math.random() * BASE62_CHARS.length()))
                .mapToObj(randomIndex -> String.valueOf(BASE62_CHARS.charAt(randomIndex)))
                .collect(Collectors.joining());
    }

    /**
     * Validates if a short code contains only valid Base62 characters
     */
    public boolean isValidShortCode(String shortCode) {
        if (shortCode == null || shortCode.isEmpty()) {
            return false;
        }
        
        for (char c : shortCode.toCharArray()) {
            if (BASE62_CHARS.indexOf(c) == -1) {
                return false;
            }
        }
        
        return true;
    }
}
