package ru.example.finances.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a pending transfer between users that will be processed offline.
 * Used for wallet-to-wallet transfers when the recipient is not online.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PendingTransfer {
    public enum Status {
        PENDING, COMPLETED, CANCELLED
    }

    private String id;
    private String fromUsername;
    private String toUsername;
    private double amount;
    private String description;
    private Status status;
    private long createdAt;
    private long processedAt;

    public PendingTransfer() {
        this.id = UUID.randomUUID().toString();
        this.status = Status.PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public PendingTransfer(String fromUsername, String toUsername, double amount, String description) {
        this.id = UUID.randomUUID().toString();
        this.fromUsername = fromUsername;
        this.toUsername = toUsername;
        this.amount = amount;
        this.description = description;
        this.status = Status.PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Marks the transfer as completed.
     */
    public void markCompleted() {
        this.status = Status.COMPLETED;
        this.processedAt = System.currentTimeMillis();
    }

    /**
     * Marks the transfer as cancelled.
     */
    public void markCancelled() {
        this.status = Status.CANCELLED;
        this.processedAt = System.currentTimeMillis();
    }

    /**
     * Checks if the transfer is still pending.
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return status == Status.PENDING;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public String getToUsername() {
        return toUsername;
    }

    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(long processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PendingTransfer that = (PendingTransfer) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PendingTransfer{" +
                "id='" + id + '\'' +
                ", fromUsername='" + fromUsername + '\'' +
                ", toUsername='" + toUsername + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", processedAt=" + processedAt +
                '}';
    }
}
