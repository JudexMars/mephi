package ru.example.finances.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * Represents a category for financial transactions.
 * Can be used for both income and expense categorization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {
    private String name;
    private String description;
    private Transaction.Type type; // INCOME or EXPENSE
    private long createdAt;

    public Category() {
        this.createdAt = System.currentTimeMillis();
    }

    public Category(String name, String description, Transaction.Type type) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Transaction.Type getType() {
        return type;
    }

    public void setType(Transaction.Type type) {
        this.type = type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(name, category.name) && type == category.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "Category{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", createdAt=" + createdAt +
                '}';
    }
}
