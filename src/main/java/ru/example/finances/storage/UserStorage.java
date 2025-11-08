package ru.example.finances.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import ru.example.finances.model.User;
import ru.example.finances.util.JsonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Storage class for managing user data persistence.
 * Handles saving and loading user accounts to/from JSON files.
 */
public class UserStorage {
    private static final String USERS_FILE = getDataDir() + "/users.json";
    private Map<String, User> users;
    
    private static String getDataDir() {
        String testDataDir = System.getProperty("test.data.dir");
        return testDataDir != null ? testDataDir : "data";
    }

    public UserStorage() {
        this.users = new HashMap<>();
        loadUsers();
    }

    /**
     * Loads users from the JSON file.
     */
    private void loadUsers() {
        try {
            Map<String, User> loadedUsers = JsonUtils.loadFromFile(USERS_FILE, new TypeReference<Map<String, User>>() {});
            if (loadedUsers != null) {
                this.users = loadedUsers;
            }
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
            this.users = new HashMap<>();
        }
    }

    /**
     * Saves users to the JSON file.
     */
    private void saveUsers() {
        try {
            JsonUtils.saveToFile(users, USERS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }

    /**
     * Saves a user to storage.
     * @param user the user to save
     */
    public void saveUser(User user) {
        users.put(user.getUsername(), user);
        saveUsers();
    }

    /**
     * Loads a user by username.
     * @param username the username to search for
     * @return the user if found, null otherwise
     */
    public User loadUser(String username) {
        return users.get(username);
    }

    /**
     * Checks if a user exists.
     * @param username the username to check
     * @return true if the user exists
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * Deletes a user from storage.
     * @param username the username to delete
     * @return true if the user was deleted, false if not found
     */
    public boolean deleteUser(String username) {
        User removed = users.remove(username);
        if (removed != null) {
            saveUsers();
            return true;
        }
        return false;
    }

    /**
     * Gets all usernames.
     * @return array of all usernames
     */
    public String[] getAllUsernames() {
        return users.keySet().toArray(new String[0]);
    }

    /**
     * Gets the total number of users.
     * @return number of users
     */
    public int getUserCount() {
        return users.size();
    }

    /**
     * Updates an existing user.
     * @param user the user to update
     * @return true if the user was updated, false if not found
     */
    public boolean updateUser(User user) {
        if (users.containsKey(user.getUsername())) {
            users.put(user.getUsername(), user);
            saveUsers();
            return true;
        }
        return false;
    }

    /**
     * Creates a backup of the users file.
     */
    public void createBackup() {
        try {
            JsonUtils.createBackup(USERS_FILE);
        } catch (IOException e) {
            System.err.println("Error creating backup: " + e.getMessage());
        }
    }
}
