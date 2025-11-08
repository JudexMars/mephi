package ru.example.finances.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import ru.example.finances.model.PendingTransfer;
import ru.example.finances.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Storage class for managing pending transfer data persistence.
 * Handles offline transfers between users.
 */
public class TransferStorage {
    private static final String TRANSFERS_FILE = getDataDir() + "/pending_transfers.json";
    private List<PendingTransfer> pendingTransfers;
    
    private static String getDataDir() {
        String testDataDir = System.getProperty("test.data.dir");
        return testDataDir != null ? testDataDir : "data";
    }

    public TransferStorage() {
        this.pendingTransfers = new ArrayList<>();
        loadTransfers();
    }

    /**
     * Loads pending transfers from the JSON file.
     */
    private void loadTransfers() {
        try {
            List<PendingTransfer> loadedTransfers = JsonUtils.loadFromFile(TRANSFERS_FILE, new TypeReference<>() {
            });
            if (loadedTransfers != null) {
                this.pendingTransfers = loadedTransfers;
            }
        } catch (IOException e) {
            System.err.println("Error loading pending transfers: " + e.getMessage());
            this.pendingTransfers = new ArrayList<>();
        }
    }

    /**
     * Saves pending transfers to the JSON file.
     */
    private void saveTransfers() {
        try {
            JsonUtils.saveToFile(pendingTransfers, TRANSFERS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving pending transfers: " + e.getMessage());
        }
    }

    /**
     * Adds a pending transfer.
     * @param transfer the transfer to add
     */
    public void addPendingTransfer(PendingTransfer transfer) {
        pendingTransfers.add(transfer);
        saveTransfers();
    }

    /**
     * Gets all pending transfers for a specific recipient.
     * @param username the recipient username
     * @return list of pending transfers for the user
     */
    public List<PendingTransfer> getPendingTransfersFor(String username) {
        return pendingTransfers.stream()
                .filter(transfer -> transfer.getToUsername().equals(username))
                .filter(PendingTransfer::isPending)
                .collect(Collectors.toList());
    }

    /**
     * Gets all pending transfers from a specific sender.
     * @param username the sender username
     * @return list of pending transfers from the user
     */
    public List<PendingTransfer> getPendingTransfersFrom(String username) {
        return pendingTransfers.stream()
                .filter(transfer -> transfer.getFromUsername().equals(username))
                .filter(PendingTransfer::isPending)
                .collect(Collectors.toList());
    }

    /**
     * Gets a pending transfer by ID.
     * @param transferId the transfer ID
     * @return the transfer if found, null otherwise
     */
    public PendingTransfer getPendingTransfer(String transferId) {
        return pendingTransfers.stream()
                .filter(transfer -> transfer.getId().equals(transferId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Marks a transfer as completed.
     * @param transferId the transfer ID
     * @return true if the transfer was found and marked as completed
     */
    public boolean markTransferCompleted(String transferId) {
        PendingTransfer transfer = getPendingTransfer(transferId);
        if (transfer != null && transfer.isPending()) {
            transfer.markCompleted();
            saveTransfers();
            return true;
        }
        return false;
    }

    /**
     * Marks a transfer as cancelled.
     * @param transferId the transfer ID
     * @return true if the transfer was found and marked as cancelled
     */
    public boolean markTransferCancelled(String transferId) {
        PendingTransfer transfer = getPendingTransfer(transferId);
        if (transfer != null && transfer.isPending()) {
            transfer.markCancelled();
            saveTransfers();
            return true;
        }
        return false;
    }

    /**
     * Removes completed and cancelled transfers older than the specified time.
     * @param olderThanMillis time in milliseconds
     * @return number of transfers removed
     */
    public int cleanupOldTransfers(long olderThanMillis) {
        long cutoffTime = System.currentTimeMillis() - olderThanMillis;
        int originalSize = pendingTransfers.size();
        
        pendingTransfers = pendingTransfers.stream()
                .filter(transfer -> {
                    // Keep pending transfers
                    if (transfer.isPending()) {
                        return true;
                    }
                    // Keep recent completed/cancelled transfers
                    return transfer.getProcessedAt() > cutoffTime;
                })
                .collect(Collectors.toList());
        
        int removedCount = originalSize - pendingTransfers.size();
        if (removedCount > 0) {
            saveTransfers();
        }
        
        return removedCount;
    }

    /**
     * Gets all transfers (pending, completed, and cancelled).
     * @return list of all transfers
     */
    public List<PendingTransfer> getAllTransfers() {
        return new ArrayList<>(pendingTransfers);
    }

    /**
     * Gets all transfers (including completed and cancelled) for a specific user.
     * @param username the recipient username
     * @return list of all transfers for the user
     */
    public List<PendingTransfer> getAllTransfersFor(String username) {
        return pendingTransfers.stream()
                .filter(transfer -> transfer.getToUsername().equals(username))
                .collect(Collectors.toList());
    }

    /**
     * Gets all transfers (including completed and cancelled) from a specific user.
     * @param username the sender username
     * @return list of all transfers from the user
     */
    public List<PendingTransfer> getAllTransfersFrom(String username) {
        return pendingTransfers.stream()
                .filter(transfer -> transfer.getFromUsername().equals(username))
                .collect(Collectors.toList());
    }

    /**
     * Gets the count of pending transfers for a user.
     * @param username the username
     * @return number of pending transfers for the user
     */
    public int getPendingTransferCount(String username) {
        return (int) pendingTransfers.stream()
                .filter(transfer -> transfer.getToUsername().equals(username))
                .filter(PendingTransfer::isPending)
                .count();
    }

    /**
     * Creates a backup of the transfers file.
     */
    public void createBackup() {
        try {
            JsonUtils.createBackup(TRANSFERS_FILE);
        } catch (IOException e) {
            System.err.println("Error creating backup: " + e.getMessage());
        }
    }
}
