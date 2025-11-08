package ru.example.finances.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for JSON serialization and deserialization operations.
 * Provides methods to save and load objects to/from JSON files.
 */
public final class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // Configure ObjectMapper for pretty printing
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private JsonUtils() {
    }

    /**
     * Saves an object to a JSON file.
     * @param object the object to save
     * @param filePath the path to the JSON file
     * @throws IOException if an I/O error occurs
     */
    public static void saveToFile(Object object, String filePath) throws IOException {
        // Create directories if they don't exist
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        
        objectMapper.writeValue(new File(filePath), object);
    }

    /**
     * Loads an object from a JSON file.
     * @param filePath the path to the JSON file
     * @param clazz the class of the object to load
     * @param <T> the type of the object
     * @return the loaded object, or null if file doesn't exist
     * @throws IOException if an I/O error occurs
     */
    public static <T> T loadFromFile(String filePath, Class<T> clazz) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        return objectMapper.readValue(file, clazz);
    }

    /**
     * Loads an object from a JSON file using TypeReference for complex types.
     * @param filePath the path to the JSON file
     * @param typeReference the type reference for complex types
     * @param <T> the type of the object
     * @return the loaded object, or null if file doesn't exist
     * @throws IOException if an I/O error occurs
     */
    public static <T> T loadFromFile(String filePath, TypeReference<T> typeReference) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        return objectMapper.readValue(file, typeReference);
    }

    /**
     * Converts an object to JSON string.
     * @param object the object to convert
     * @return JSON string representation
     * @throws IOException if serialization fails
     */
    public static String toJsonString(Object object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Converts a JSON string to an object.
     * @param jsonString the JSON string
     * @param clazz the class of the object
     * @param <T> the type of the object
     * @return the deserialized object
     * @throws IOException if deserialization fails
     */
    public static <T> T fromJsonString(String jsonString, Class<T> clazz) throws IOException {
        return objectMapper.readValue(jsonString, clazz);
    }

    /**
     * Checks if a file exists.
     * @param filePath the path to check
     * @return true if the file exists
     */
    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * Deletes a file if it exists.
     * @param filePath the path to the file to delete
     * @return true if the file was deleted or didn't exist
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return !file.exists() || file.delete();
    }

    /**
     * Creates a backup of a file by copying it with a .backup extension.
     * @param filePath the path to the file to backup
     * @throws IOException if backup creation fails
     */
    public static void createBackup(String filePath) throws IOException {
        File originalFile = new File(filePath);
        if (originalFile.exists()) {
            File backupFile = new File(filePath + ".backup");
            Files.copy(originalFile.toPath(), backupFile.toPath());
        }
    }
}
