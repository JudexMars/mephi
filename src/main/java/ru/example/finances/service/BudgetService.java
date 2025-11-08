package ru.example.finances.service;

import ru.example.finances.model.Budget;
import ru.example.finances.model.Wallet;
import ru.example.finances.util.ValidationUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for managing budget operations.
 * Handles budget creation, monitoring, and alerts.
 */
public class BudgetService {
    private final FinanceService financeService;

    public BudgetService(FinanceService financeService) {
        this.financeService = financeService;
    }

    /**
     * Sets a budget for a category.
     * @param category the category name
     * @param limit the budget limit
     * @return operation result
     */
    public BudgetResult setBudget(String category, double limit) {
        Wallet wallet = financeService.getCurrentWallet();
        if (wallet == null) {
            return new BudgetResult(false, "Кошелек не загружен.");
        }

        if (!ValidationUtils.isValidCategory(category)) {
            return new BudgetResult(false, "Неверное название категории.");
        }

        if (!ValidationUtils.isValidBudgetLimit(limit)) {
            return new BudgetResult(false, "Неверный лимит бюджета. Лимит должен быть неотрицательным.");
        }

        Budget budget = new Budget(category, limit);
        wallet.setBudget(budget);
        financeService.saveWallet();

        return new BudgetResult(true, String.format("Бюджет для категории '%s' установлен: %.2f руб.", category, limit));
    }

    /**
     * Gets a budget for a specific category.
     * @param category the category name
     * @return the budget, or null if not found
     */
    public Budget getBudget(String category) {
        Wallet wallet = financeService.getCurrentWallet();
        return wallet != null ? wallet.getBudget(category) : null;
    }

    /**
     * Gets all budgets.
     * @return map of category to budget
     */
    public Map<String, Budget> getAllBudgets() {
        Wallet wallet = financeService.getCurrentWallet();
        return wallet != null ? wallet.getBudgets() : Map.of();
    }

    /**
     * Removes a budget for a category.
     * @param category the category name
     * @return operation result
     */
    public BudgetResult removeBudget(String category) {
        Wallet wallet = financeService.getCurrentWallet();
        if (wallet == null) {
            return new BudgetResult(false, "Кошелек не загружен.");
        }

        Map<String, Budget> budgets = wallet.getBudgets();
        if (budgets.remove(category) != null) {
            wallet.setBudgets(budgets);
            financeService.saveWallet();
            return new BudgetResult(true, String.format("Бюджет для категории '%s' удален.", category));
        } else {
            return new BudgetResult(false, String.format("Бюджет для категории '%s' не найден.", category));
        }
    }

    /**
     * Updates a budget limit for a category.
     * @param category the category name
     * @param newLimit the new budget limit
     * @return operation result
     */
    public BudgetResult updateBudgetLimit(String category, double newLimit) {
        Wallet wallet = financeService.getCurrentWallet();
        if (wallet == null) {
            return new BudgetResult(false, "Кошелек не загружен.");
        }

        if (!ValidationUtils.isValidBudgetLimit(newLimit)) {
            return new BudgetResult(false, "Неверный лимит бюджета. Лимит должен быть неотрицательным.");
        }

        Budget budget = wallet.getBudget(category);
        if (budget == null) {
            return new BudgetResult(false, String.format("Бюджет для категории '%s' не найден.", category));
        }

        budget.setLimit(newLimit);
        financeService.saveWallet();

        return new BudgetResult(true, String.format("Лимит бюджета для категории '%s' обновлен: %.2f руб.", category, newLimit));
    }

    /**
     * Gets all exceeded budgets.
     * @return list of exceeded budgets
     */
    public List<Budget> getExceededBudgets() {
        Wallet wallet = financeService.getCurrentWallet();
        return wallet != null ? wallet.getExceededBudgets() : List.of();
    }

    /**
     * Gets budgets that are close to being exceeded (80% or more used).
     * @return list of budgets close to limit
     */
    public List<Budget> getBudgetsCloseToLimit() {
        Wallet wallet = financeService.getCurrentWallet();
        if (wallet == null) {
            return List.of();
        }

        return wallet.getBudgets().values().stream()
                .filter(budget -> budget.getUsagePercentage() >= 80.0 && !budget.isExceeded())
                .collect(Collectors.toList());
    }

    /**
     * Checks for budget violations and returns alerts.
     * @return list of budget alerts
     */
    public List<BudgetAlert> checkBudgetAlerts() {
        Wallet wallet = financeService.getCurrentWallet();
        if (wallet == null) {
            return List.of();
        }

        return wallet.getBudgets().values().stream()
                .map(this::createBudgetAlert)
                .filter(alert -> alert.getType() != BudgetAlert.AlertType.OK)
                .collect(Collectors.toList());
    }

    /**
     * Creates a budget alert for a budget.
     * @param budget the budget to check
     * @return budget alert
     */
    private BudgetAlert createBudgetAlert(Budget budget) {
        if (budget.isExceeded()) {
            return new BudgetAlert(
                    BudgetAlert.AlertType.EXCEEDED,
                    budget.getCategory(),
                    String.format("Бюджет превышен на %.2f руб. (%.1f%%)", 
                            Math.abs(budget.getRemainingBudget()), 
                            budget.getUsagePercentage())
            );
        } else if (budget.getUsagePercentage() >= 90.0) {
            return new BudgetAlert(
                    BudgetAlert.AlertType.CRITICAL,
                    budget.getCategory(),
                    String.format("Критический уровень: использовано %.1f%% бюджета", 
                            budget.getUsagePercentage())
            );
        } else if (budget.getUsagePercentage() >= 80.0) {
            return new BudgetAlert(
                    BudgetAlert.AlertType.WARNING,
                    budget.getCategory(),
                    String.format("Предупреждение: использовано %.1f%% бюджета", 
                            budget.getUsagePercentage())
            );
        } else {
            return new BudgetAlert(BudgetAlert.AlertType.OK, budget.getCategory(), "Бюджет в норме");
        }
    }

    /**
     * Gets a formatted budget summary.
     * @return formatted budget summary string
     */
    public String getBudgetSummary() {
        Wallet wallet = financeService.getCurrentWallet();
        if (wallet == null) {
            return "Кошелек не загружен.";
        }

        Map<String, Budget> budgets = wallet.getBudgets();
        if (budgets.isEmpty()) {
            return "Бюджеты не установлены.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("=== СВОДКА ПО БЮДЖЕТАМ ===\n");

        for (Budget budget : budgets.values()) {
            summary.append(String.format("\n%s: %.2f руб.\n", budget.getCategory(), budget.getLimit()));
            summary.append(String.format("  Потрачено: %.2f руб. (%.1f%%)\n", 
                    budget.getSpent(), budget.getUsagePercentage()));
            summary.append(String.format("  Остаток: %.2f руб.\n", budget.getRemainingBudget()));
            
            if (budget.isExceeded()) {
                summary.append("  ⚠️ ПРЕВЫШЕН!\n");
            } else if (budget.getUsagePercentage() >= 80.0) {
                summary.append("  ⚡ Близко к лимиту\n");
            } else {
                summary.append("  ✅ В норме\n");
            }
        }

        // Add alerts summary
        List<BudgetAlert> alerts = checkBudgetAlerts();
        if (!alerts.isEmpty()) {
            summary.append("\n=== УВЕДОМЛЕНИЯ ===\n");
            for (BudgetAlert alert : alerts) {
                summary.append(String.format("%s: %s\n", alert.getCategory(), alert.getMessage()));
            }
        }

        return summary.toString();
    }

    /**
     * Gets budget statistics.
     * @return formatted statistics string
     */
    public String getBudgetStatistics() {
        Wallet wallet = financeService.getCurrentWallet();
        if (wallet == null) {
            return "Кошелек не загружен.";
        }

        Map<String, Budget> budgets = wallet.getBudgets();
        if (budgets.isEmpty()) {
            return "Бюджеты не установлены.";
        }

        int totalBudgets = budgets.size();
        int exceededBudgets = (int) budgets.values().stream().filter(Budget::isExceeded).count();
        int warningBudgets = (int) budgets.values().stream()
                .filter(b -> b.getUsagePercentage() >= 80.0 && !b.isExceeded()).count();
        
        double totalBudgetLimit = budgets.values().stream().mapToDouble(Budget::getLimit).sum();
        double totalSpent = budgets.values().stream().mapToDouble(Budget::getSpent).sum();
        double overallUsage = totalBudgetLimit > 0 ? (totalSpent / totalBudgetLimit) * 100 : 0;

        StringBuilder stats = new StringBuilder();
        stats.append("=== СТАТИСТИКА БЮДЖЕТОВ ===\n");
        stats.append(String.format("Всего бюджетов: %d\n", totalBudgets));
        stats.append(String.format("Превышено: %d\n", exceededBudgets));
        stats.append(String.format("Предупреждения: %d\n", warningBudgets));
        stats.append(String.format("Общий лимит: %.2f руб.\n", totalBudgetLimit));
        stats.append(String.format("Общие траты: %.2f руб.\n", totalSpent));
        stats.append(String.format("Общее использование: %.1f%%\n", overallUsage));

        return stats.toString();
    }

    /**
     * Result class for budget operations.
     */
    public static class BudgetResult {
        private final boolean success;
        private final String message;

        public BudgetResult(boolean success, String message) {
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

    /**
     * Budget alert class.
     */
    public static class BudgetAlert {
        public enum AlertType {
            OK, WARNING, CRITICAL, EXCEEDED
        }

        private final AlertType type;
        private final String category;
        private final String message;

        public BudgetAlert(AlertType type, String category, String message) {
            this.type = type;
            this.category = category;
            this.message = message;
        }

        public AlertType getType() {
            return type;
        }

        public String getCategory() {
            return category;
        }

        public String getMessage() {
            return message;
        }
    }
}
