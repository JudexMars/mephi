package ru.example.finances.service;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import ru.example.finances.model.Category;
import ru.example.finances.model.Transaction;
import ru.example.finances.model.Wallet;
import ru.example.finances.storage.WalletStorage;
import ru.example.finances.util.ValidationUtils;

/**
 * Service class for managing financial operations.
 * Handles transactions, calculations, and wallet management.
 */
public class FinanceService {
    private final WalletStorage walletStorage;
    private Wallet currentWallet;

    public FinanceService() {
        this.walletStorage = new WalletStorage();
        this.currentWallet = null;
    }

    /**
     * Loads a wallet for the specified user.
     * @param username the username
     */
    public void loadWallet(String username) {
        this.currentWallet = walletStorage.loadWallet(username);
    }
    
    /**
     * Creates a new empty wallet for a user, replacing any existing wallet.
     * @param username the username
     */
    public void createNewWallet(String username) {
        this.currentWallet = new Wallet(username);
    }

    /**
     * Saves the current wallet.
     */
    public void saveWallet() {
        if (currentWallet != null) {
            walletStorage.saveWallet(currentWallet);
        }
    }

    /**
     * Gets the current wallet.
     * @return the current wallet, or null if none is loaded
     */
    public Wallet getCurrentWallet() {
        return currentWallet;
    }

    /**
     * Adds an income transaction.
     * @param category the income category
     * @param amount the amount
     * @param description the description
     * @return operation result
     */
    public OperationResult addIncome(String category, double amount, String description) {
        OperationResult x = validateOperation(category, amount);
        if (x != null) {
            return x;
        }

        Transaction transaction = new Transaction(Transaction.Type.INCOME, category, amount, description);
        currentWallet.addTransaction(transaction);
        saveWallet();

        return new OperationResult(true, String.format("Доход добавлен: %.2f руб. в категории '%s'", amount, category));
    }

    @Nullable
    private OperationResult validateOperation(String category, double amount) {
        if (currentWallet == null) {
            return new OperationResult(false, "Кошелек не загружен.");
        }

        if (!ValidationUtils.isValidCategory(category)) {
            return new OperationResult(false, "Неверное название категории.");
        }

        if (!ValidationUtils.isValidAmount(amount)) {
            return new OperationResult(false, "Неверная сумма. Сумма должна быть положительной.");
        }
        return null;
    }

    /**
     * Adds an expense transaction.
     * @param category the expense category
     * @param amount the amount
     * @param description the description
     * @return operation result
     */
    public OperationResult addExpense(String category, double amount, String description) {
        OperationResult x = validateOperation(category, amount);
        if (x != null) {
            return x;
        }

        Transaction transaction = new Transaction(Transaction.Type.EXPENSE, category, amount, description);
        currentWallet.addTransaction(transaction);
        saveWallet();

        return new OperationResult(true, String.format("Расход добавлен: %.2f руб. в категории '%s'", amount, category));
    }

    /**
     * Gets total income.
     * @return total income amount
     */
    public double getTotalIncome() {
        return currentWallet != null ? currentWallet.getTotalIncome() : 0.0;
    }

    /**
     * Gets total expenses.
     * @return total expenses amount
     */
    public double getTotalExpenses() {
        return currentWallet != null ? currentWallet.getTotalExpenses() : 0.0;
    }

    /**
     * Gets current balance.
     * @return current balance (income - expenses)
     */
    public double getBalance() {
        return currentWallet != null ? currentWallet.getBalance() : 0.0;
    }

    /**
     * Gets income by category.
     * @return map of category to income amount
     */
    public Map<String, Double> getIncomeByCategory() {
        return currentWallet != null ? currentWallet.getIncomeByCategory() : Map.of();
    }

    /**
     * Gets expenses by category.
     * @return map of category to expense amount
     */
    public Map<String, Double> getExpensesByCategory() {
        return currentWallet != null ? currentWallet.getExpensesByCategory() : Map.of();
    }

    /**
     * Gets all transactions.
     * @return list of all transactions
     */
    public List<Transaction> getAllTransactions() {
        return currentWallet != null ? currentWallet.getTransactions() : List.of();
    }

    /**
     * Adds a category to the wallet.
     * @param name the category name
     * @param description the category description
     * @param type the category type (INCOME or EXPENSE)
     * @return operation result
     */
    public OperationResult addCategory(String name, String description, Transaction.Type type) {
        if (currentWallet == null) {
            return new OperationResult(false, "Кошелек не загружен.");
        }

        if (!ValidationUtils.isValidCategory(name)) {
            return new OperationResult(false, "Неверное название категории.");
        }
        
        if (type == null) {
            return new OperationResult(false, "Тип категории не может быть null.");
        }

        // Check if category already exists
        boolean categoryExists = currentWallet.getCategories().stream()
                .anyMatch(cat -> cat.getName().equals(name) && cat.getType() == type);
        
        if (categoryExists) {
            return new OperationResult(false, String.format("Категория '%s' уже существует.", name));
        }

        Category category = new Category(name, description, type);
        currentWallet.addCategory(category);
        saveWallet();

        return new OperationResult(true, String.format("Категория '%s' добавлена.", name));
    }

    /**
     * Checks if expenses exceed income.
     * @return true if overspent
     */
    public boolean isOverspent() {
        return currentWallet != null && currentWallet.isOverspent();
    }

    /**
     * Exports wallet data to a file.
     * @param exportPath the path to export to
     * @return operation result
     */
    public OperationResult exportWallet(String exportPath) {
        if (currentWallet == null) {
            return new OperationResult(false, "Кошелек не загружен.");
        }

        try {
            walletStorage.exportWallet(currentWallet, exportPath);
            return new OperationResult(true, "Данные кошелька экспортированы в " + exportPath);
        } catch (Exception e) {
            return new OperationResult(false, "Ошибка экспорта: " + e.getMessage());
        }
    }

    /**
     * Creates a backup of the current wallet.
     * @return operation result
     */
    public OperationResult createBackup() {
        if (currentWallet == null) {
            return new OperationResult(false, "Кошелек не загружен.");
        }

        try {
            walletStorage.createBackup(currentWallet.getUsername());
            return new OperationResult(true, "Резервная копия кошелька создана.");
        } catch (Exception e) {
            return new OperationResult(false, "Ошибка создания резервной копии: " + e.getMessage());
        }
    }

    /**
     * Gets financial summary.
     * @return formatted summary string
     */
    public String getFinancialSummary() {
        if (currentWallet == null) {
            return "Кошелек не загружен.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("=== ФИНАНСОВАЯ СВОДКА ===\n");
        summary.append(String.format("Общий доход: %.2f руб.\n", getTotalIncome()));
        summary.append(String.format("Общие расходы: %.2f руб.\n", getTotalExpenses()));
        summary.append(String.format("Баланс: %.2f руб.\n", getBalance()));
        
        if (isOverspent()) {
            summary.append("⚠️ ВНИМАНИЕ: Расходы превышают доходы!\n");
        }

        summary.append("\n--- Доходы по категориям ---\n");
        Map<String, Double> incomeByCategory = getIncomeByCategory();
        if (incomeByCategory.isEmpty()) {
            summary.append("Нет доходов.\n");
        } else {
            incomeByCategory.forEach((category, amount) -> 
                summary.append(String.format("%s: %.2f руб.\n", category, amount)));
        }

        summary.append("\n--- Расходы по категориям ---\n");
        Map<String, Double> expensesByCategory = getExpensesByCategory();
        if (expensesByCategory.isEmpty()) {
            summary.append("Нет расходов.\n");
        } else {
            expensesByCategory.forEach((category, amount) -> 
                summary.append(String.format("%s: %.2f руб.\n", category, amount)));
        }

        return summary.toString();
    }

    /**
     * Result class for finance operations.
     */
    public static class OperationResult {
        private final boolean success;
        private final String message;

        public OperationResult(boolean success, String message) {
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
