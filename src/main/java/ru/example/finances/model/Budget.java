package ru.example.finances.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * Represents a budget allocation for a specific category.
 * Tracks spending limits and current usage.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Budget {
    private String category;
    private double limit;
    private double spent;
    private long createdAt;
    private long updatedAt;

    public Budget() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = createdAt;
        this.spent = 0.0;
    }

    public Budget(String category, double limit) {
        this.category = category;
        this.limit = limit;
        this.spent = 0.0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    /**
     * Gets the remaining budget amount.
     * @return remaining budget (can be negative if overspent)
     */
    public double getRemainingBudget() {
        return limit - spent;
    }

    /**
     * Checks if the budget is exceeded.
     * @return true if spent amount exceeds the limit
     */
    public boolean isExceeded() {
        return spent > limit;
    }

    /**
     * Gets the percentage of budget used.
     * @return percentage (0-100+)
     */
    public double getUsagePercentage() {
        if (limit == 0) return 0;
        return (spent / limit) * 100;
    }

    /**
     * Adds an expense to the budget tracking.
     * @param amount amount to add to spent
     */
    public void addExpense(double amount) {
        this.spent += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Removes an expense from the budget tracking.
     * @param amount amount to subtract from spent
     */
    public void removeExpense(double amount) {
        this.spent = Math.max(0, this.spent - amount);
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = spent;
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Budget budget = (Budget) o;
        return Objects.equals(category, budget.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category);
    }

    @Override
    public String toString() {
        return "Budget{" +
                "category='" + category + '\'' +
                ", limit=" + limit +
                ", spent=" + spent +
                ", remaining=" + getRemainingBudget() +
                ", usagePercentage=" + String.format("%.1f", getUsagePercentage()) + "%" +
                '}';
    }
}
