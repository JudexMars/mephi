package ru.example.url.shortener.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigLoader {
    private static final String CONFIG_FILE = "url-shortener.properties";
    private static Properties properties;
    
    static {
        loadProperties();
    }

    private ConfigLoader() {
    }

    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.out.println("Could not find " + CONFIG_FILE + " in resources, using default values");
            } else {
                properties.load(input);
                System.out.println("Configuration loaded from " + CONFIG_FILE);
            }
        } catch (IOException e) {
            System.out.println("Could not load " + CONFIG_FILE + ", using default values: " + e.getMessage());
        }
    }
    
    public static int getDefaultMaxClicks() {
        return getIntProperty("url.shortener.default.max.clicks", 100);
    }
    
    public static int getDefaultExpirationHours() {
        return getIntProperty("url.shortener.default.expiration.hours", 24);
    }
    
    public static String getBaseUrl() {
        return getStringProperty("url.shortener.base.url", "clck.ru");
    }
    
    public static int getCleanupIntervalMinutes() {
        return getIntProperty("url.shortener.cleanup.interval.minutes", 5);
    }
    
    private static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Invalid integer value for " + key + ": " + value + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    private static String getStringProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue).trim();
    }
}
