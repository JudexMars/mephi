package ru.example.finances.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a user's financial wallet containing all transactions, budgets, and categories.
 * This is the main container for a user's financial data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Wallet {
    private String username;
    private List<Transaction> transactions;
    private Map<String, Budget> budgets;
    private Set<Category> categories;
    private long createdAt;
    private long updatedAt;

    public Wallet() {
        this.transactions = new ArrayList<>();
        this.budgets = new HashMap<>();
        this.categories = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public Wallet(String username) {
        this.username = username;
        this.transactions = new ArrayList<>();
        this.budgets = new HashMap<>();
        this.categories = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    /**
     * Adds a transaction to the wallet and updates relevant budgets.
     * @param transaction the transaction to add
     */
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        
        // Auto-create category if it doesn't exist
        String categoryName = transaction.getCategory();
        boolean categoryExists = categories.stream()
                .anyMatch(cat -> cat.getName().equals(categoryName));
        
        if (!categoryExists) {
            Category newCategory = new Category(categoryName, 
                "Автоматически созданная категория", 
                transaction.getType());
            categories.add(newCategory);
        }
        
        // Update budget if it's an expense
        if (transaction.getType() == Transaction.Type.EXPENSE) {
            Budget budget = budgets.get(transaction.getCategory());
            if (budget != null) {
                budget.addExpense(transaction.getAmount());
            }
        }
        
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Removes a transaction from the wallet and updates relevant budgets.
     * @param transactionId the ID of the transaction to remove
     * @return true if the transaction was found and removed
     */
    public boolean removeTransaction(String transactionId) {
        Transaction toRemove = transactions.stream()
                .filter(t -> t.getId().equals(transactionId))
                .findFirst()
                .orElse(null);
        
        if (toRemove != null) {
            transactions.remove(toRemove);
            
            // Update budget if it was an expense
            if (toRemove.getType() == Transaction.Type.EXPENSE) {
                Budget budget = budgets.get(toRemove.getCategory());
                if (budget != null) {
                    budget.removeExpense(toRemove.getAmount());
                }
            }
            
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * Calculates total income.
     * @return sum of all income transactions
     */
    public double getTotalIncome() {
        return transactions.stream()
                .filter(t -> t.getType() == Transaction.Type.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Calculates total expenses.
     * @return sum of all expense transactions
     */
    public double getTotalExpenses() {
        return transactions.stream()
                .filter(t -> t.getType() == Transaction.Type.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Calculates current balance (income - expenses).
     * @return current balance
     */
    public double getBalance() {
        return getTotalIncome() - getTotalExpenses();
    }

    /**
     * Gets income by category.
     * @return map of category to total income amount
     */
    public Map<String, Double> getIncomeByCategory() {
        return transactions.stream()
                .filter(t -> t.getType() == Transaction.Type.INCOME)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    /**
     * Gets expenses by category.
     * @return map of category to total expense amount
     */
    public Map<String, Double> getExpensesByCategory() {
        return transactions.stream()
                .filter(t -> t.getType() == Transaction.Type.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    /**
     * Gets transactions for specific categories.
     * @param categoryNames list of category names to filter by
     * @return list of transactions in the specified categories
     */
    public List<Transaction> getTransactionsByCategories(List<String> categoryNames) {
        return transactions.stream()
                .filter(t -> categoryNames.contains(t.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * Adds or updates a budget for a category.
     * @param budget the budget to set
     */
    public void setBudget(Budget budget) {
        budgets.put(budget.getCategory(), budget);
        
        // Recalculate spent amount based on existing transactions
        double spent = transactions.stream()
                .filter(t -> t.getType() == Transaction.Type.EXPENSE)
                .filter(t -> t.getCategory().equals(budget.getCategory()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        budget.setSpent(spent);
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Gets budget for a specific category.
     * @param category the category name
     * @return the budget for the category, or null if not found
     */
    public Budget getBudget(String category) {
        return budgets.get(category);
    }

    /**
     * Gets all budgets that are exceeded.
     * @return list of exceeded budgets
     */
    public List<Budget> getExceededBudgets() {
        return budgets.values().stream()
                .filter(Budget::isExceeded)
                .collect(Collectors.toList());
    }

    /**
     * Adds a category to the wallet.
     * @param category the category to add
     */
    public void addCategory(Category category) {
        categories.add(category);
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Checks if expenses exceed income.
     * @return true if total expenses are greater than total income
     */
    public boolean isOverspent() {
        return getTotalExpenses() > getTotalIncome();
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = new ArrayList<>(transactions);
        this.updatedAt = System.currentTimeMillis();
    }

    public Map<String, Budget> getBudgets() {
        return new HashMap<>(budgets);
    }

    public void setBudgets(Map<String, Budget> budgets) {
        this.budgets = new HashMap<>(budgets);
        this.updatedAt = System.currentTimeMillis();
    }

    public Set<Category> getCategories() {
        return new HashSet<>(categories);
    }

    public void setCategories(Set<Category> categories) {
        this.categories = new HashSet<>(categories);
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return Objects.equals(username, wallet.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "username='" + username + '\'' +
                ", transactionCount=" + transactions.size() +
                ", budgetCount=" + budgets.size() +
                ", categoryCount=" + categories.size() +
                ", balance=" + getBalance() +
                ", totalIncome=" + getTotalIncome() +
                ", totalExpenses=" + getTotalExpenses() +
                '}';
    }
}
