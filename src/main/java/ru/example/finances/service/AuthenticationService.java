package ru.example.finances.service;

import ru.example.finances.model.User;
import ru.example.finances.storage.UserStorage;
import ru.example.finances.util.ValidationUtils;

/**
 * Service class for handling user authentication operations.
 * Manages user registration, login, and session management.
 */
public class AuthenticationService {
    private final UserStorage userStorage;
    private User currentUser;

    public AuthenticationService() {
        this.userStorage = new UserStorage();
        this.currentUser = null;
    }

    /**
     * Registers a new user.
     * @param username the username
     * @param password the password
     * @param email the email address
     * @return registration result
     */
    public AuthResult register(String username, String password, String email) {
        // Validate input
        if (!ValidationUtils.isValidUsername(username)) {
            return new AuthResult(false, "Неверное имя пользователя. Используйте 3-20 символов (буквы, цифры, подчеркивание).");
        }
        
        if (!ValidationUtils.isValidPassword(password)) {
            return new AuthResult(false, "Пароль должен содержать минимум 6 символов.");
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            return new AuthResult(false, "Неверный формат email адреса.");
        }

        // Check if user already exists
        if (userStorage.userExists(username)) {
            return new AuthResult(false, "Пользователь с таким именем уже существует.");
        }

        // Create and save new user
        String hashedPassword = ValidationUtils.hashPassword(password);
        User newUser = new User(username, hashedPassword, email);
        userStorage.saveUser(newUser);

        return new AuthResult(true, "Пользователь успешно зарегистрирован.");
    }

    /**
     * Authenticates a user.
     * @param username the username
     * @param password the password
     * @return authentication result
     */
    public AuthResult login(String username, String password) {
        // Validate input
        if (!ValidationUtils.isNotEmpty(username) || !ValidationUtils.isNotEmpty(password)) {
            return new AuthResult(false, "Имя пользователя и пароль не могут быть пустыми.");
        }

        // Load user
        User user = userStorage.loadUser(username);
        if (user == null) {
            return new AuthResult(false, "Пользователь не найден.");
        }

        // Verify password
        if (!ValidationUtils.verifyPassword(password, user.getPasswordHash())) {
            return new AuthResult(false, "Неверный пароль.");
        }

        // Set current user
        this.currentUser = user;
        return new AuthResult(true, "Успешный вход в систему.");
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Checks if a user is currently logged in.
     * @return true if a user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Gets the current logged-in user.
     * @return the current user, or null if no user is logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Gets the current user's username.
     * @return the username, or null if no user is logged in
     */
    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    /**
     * Changes the password for the current user.
     * @param oldPassword the current password
     * @param newPassword the new password
     * @return result of the password change
     */
    public AuthResult changePassword(String oldPassword, String newPassword) {
        if (!isLoggedIn()) {
            return new AuthResult(false, "Необходимо войти в систему для смены пароля.");
        }

        if (!ValidationUtils.verifyPassword(oldPassword, currentUser.getPasswordHash())) {
            return new AuthResult(false, "Неверный текущий пароль.");
        }

        if (!ValidationUtils.isValidPassword(newPassword)) {
            return new AuthResult(false, "Новый пароль должен содержать минимум 6 символов.");
        }

        // Update password
        String hashedNewPassword = ValidationUtils.hashPassword(newPassword);
        currentUser.setPasswordHash(hashedNewPassword);
        userStorage.updateUser(currentUser);

        return new AuthResult(true, "Пароль успешно изменен.");
    }

    /**
     * Updates the current user's email.
     * @param newEmail the new email address
     * @return result of the email update
     */
    public AuthResult updateEmail(String newEmail) {
        if (!isLoggedIn()) {
            return new AuthResult(false, "Необходимо войти в систему для изменения email.");
        }

        if (!ValidationUtils.isValidEmail(newEmail)) {
            return new AuthResult(false, "Неверный формат email адреса.");
        }

        currentUser.setEmail(newEmail);
        userStorage.updateUser(currentUser);

        return new AuthResult(true, "Email успешно обновлен.");
    }

    /**
     * Gets all registered usernames (for transfer functionality).
     * @return array of usernames
     */
    public String[] getAllUsernames() {
        return userStorage.getAllUsernames();
    }

    /**
     * Checks if a username exists.
     * @param username the username to check
     * @return true if the user exists
     */
    public boolean userExists(String username) {
        return userStorage.userExists(username);
    }

    /**
     * Gets the total number of registered users.
     * @return number of users
     */
    public int getUserCount() {
        return userStorage.getUserCount();
    }

    /**
     * Result class for authentication operations.
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;

        public AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
