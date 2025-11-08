package ru.example.finances.storage;

import ru.example.finances.model.Wallet;
import ru.example.finances.util.JsonUtils;

import java.io.IOException;

/**
 * Storage class for managing wallet data persistence.
 * Each user has their own wallet file for data isolation.
 */
public class WalletStorage {
    private static final String WALLET_DIR = getDataDir() + "/wallets/";
    private static final String WALLET_FILE_EXTENSION = ".json";
    
    private static String getDataDir() {
        String testDataDir = System.getProperty("test.data.dir");
        return testDataDir != null ? testDataDir : "data";
    }

    /**
     * Gets the file path for a user's wallet.
     * @param username the username
     * @return the file path for the wallet
     */
    private String getWalletFilePath(String username) {
        return WALLET_DIR + username + WALLET_FILE_EXTENSION;
    }

    /**
     * Saves a wallet to storage.
     * @param wallet the wallet to save
     */
    public void saveWallet(Wallet wallet) {
        try {
            String filePath = getWalletFilePath(wallet.getUsername());
            JsonUtils.saveToFile(wallet, filePath);
        } catch (IOException e) {
            System.err.println("Error saving wallet for " + wallet.getUsername() + ": " + e.getMessage());
        }
    }

    /**
     * Loads a wallet from storage.
     * @param username the username whose wallet to load
     * @return the wallet if found, or a new empty wallet if not found
     */
    public Wallet loadWallet(String username) {
        try {
            String filePath = getWalletFilePath(username);
            Wallet wallet = JsonUtils.loadFromFile(filePath, Wallet.class);
            
            if (wallet == null) {
                // Create a new wallet if none exists
                wallet = new Wallet(username);
                saveWallet(wallet);
            } else {
                // Ensure username is set (in case of data migration)
                wallet.setUsername(username);
            }
            
            return wallet;
        } catch (IOException e) {
            System.err.println("Error loading wallet for " + username + ": " + e.getMessage());
            // Return a new wallet if loading fails
            Wallet newWallet = new Wallet(username);
            saveWallet(newWallet);
            return newWallet;
        }
    }

    /**
     * Checks if a wallet exists for a user.
     * @param username the username to check
     * @return true if the wallet file exists
     */
    public boolean walletExists(String username) {
        String filePath = getWalletFilePath(username);
        return JsonUtils.fileExists(filePath);
    }

    /**
     * Deletes a wallet from storage.
     * @param username the username whose wallet to delete
     * @return true if the wallet was deleted or didn't exist
     */
    public boolean deleteWallet(String username) {
        String filePath = getWalletFilePath(username);
        return JsonUtils.deleteFile(filePath);
    }

    /**
     * Creates a backup of a user's wallet.
     * @param username the username whose wallet to backup
     */
    public void createBackup(String username) {
        try {
            String filePath = getWalletFilePath(username);
            JsonUtils.createBackup(filePath);
        } catch (IOException e) {
            System.err.println("Error creating backup for " + username + ": " + e.getMessage());
        }
    }

    /**
     * Exports a wallet to a specific file path.
     * @param wallet the wallet to export
     * @param exportPath the path to export to
     */
    public void exportWallet(Wallet wallet, String exportPath) {
        try {
            JsonUtils.saveToFile(wallet, exportPath);
        } catch (IOException e) {
            System.err.println("Error exporting wallet: " + e.getMessage());
        }
    }

    /**
     * Imports a wallet from a specific file path.
     * @param importPath the path to import from
     * @return the imported wallet, or null if import failed
     */
    public Wallet importWallet(String importPath) {
        try {
            return JsonUtils.loadFromFile(importPath, Wallet.class);
        } catch (IOException e) {
            System.err.println("Error importing wallet: " + e.getMessage());
            return null;
        }
    }
}
