package ru.example.finances.service;

import ru.example.finances.model.Budget;
import ru.example.finances.service.BudgetService.BudgetAlert;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing notifications and alerts.
 * Handles budget violations, overspending alerts, and transfer notifications.
 */
public class NotificationService {
    private final List<String> notifications;

    public NotificationService() {
        this.notifications = new ArrayList<>();
    }

    /**
     * Adds a notification message.
     * @param message the notification message
     */
    public void addNotification(String message) {
        notifications.add(message);
    }

    /**
     * Gets all notifications.
     * @return list of notification messages
     */
    public List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }

    /**
     * Clears all notifications.
     */
    public void clearNotifications() {
        notifications.clear();
    }

    /**
     * Gets the count of unread notifications.
     * @return number of notifications
     */
    public int getNotificationCount() {
        return notifications.size();
    }

    /**
     * Checks for budget alerts and adds notifications.
     * @param budgetService the budget service to check
     */
    public void checkBudgetAlerts(BudgetService budgetService) {
        List<BudgetAlert> alerts = budgetService.checkBudgetAlerts();
        
        for (BudgetAlert alert : alerts) {
            String message = formatBudgetAlert(alert);
            addNotification(message);
        }
    }

    /**
     * Formats a budget alert into a notification message.
     * @param alert the budget alert
     * @return formatted notification message
     */
    private String formatBudgetAlert(BudgetAlert alert) {
        String icon = switch (alert.getType()) {
            case EXCEEDED -> "🚨";
            case CRITICAL -> "⚠️";
            case WARNING -> "⚡";
            case OK -> "✅";
        };
        
        return String.format("%s %s: %s", icon, alert.getCategory(), alert.getMessage());
    }

    /**
     * Checks for overspending and adds notification.
     * @param financeService the finance service to check
     */
    public void checkOverspending(FinanceService financeService) {
        if (financeService.isOverspent()) {
            double balance = financeService.getBalance();
            addNotification(String.format("💸 ВНИМАНИЕ: Расходы превышают доходы на %.2f руб.!", Math.abs(balance)));
        }
    }

    /**
     * Adds a notification for a new income.
     * @param category the income category
     * @param amount the income amount
     */
    public void notifyIncomeAdded(String category, double amount) {
        addNotification(String.format("💰 Доход добавлен: %.2f руб. в категории '%s'", amount, category));
    }

    /**
     * Adds a notification for a new expense.
     * @param category the expense category
     * @param amount the expense amount
     */
    public void notifyExpenseAdded(String category, double amount) {
        addNotification(String.format("💳 Расход добавлен: %.2f руб. в категории '%s'", amount, category));
    }

    /**
     * Adds a notification for a budget being set.
     * @param category the budget category
     * @param limit the budget limit
     */
    public void notifyBudgetSet(String category, double limit) {
        addNotification(String.format("📊 Бюджет установлен для '%s': %.2f руб.", category, limit));
    }

    /**
     * Adds a notification for a budget being exceeded.
     * @param budget the exceeded budget
     */
    public void notifyBudgetExceeded(Budget budget) {
        addNotification(String.format("🚨 Бюджет превышен! Категория '%s': потрачено %.2f руб. из %.2f руб.", 
                budget.getCategory(), budget.getSpent(), budget.getLimit()));
    }

    /**
     * Adds a notification for a pending transfer.
     * @param fromUser the sender username
     * @param amount the transfer amount
     */
    public void notifyPendingTransfer(String fromUser, double amount) {
        addNotification(String.format("📨 Получен перевод от %s на сумму %.2f руб.", fromUser, amount));
    }

    /**
     * Adds a notification for a completed transfer.
     * @param toUser the recipient username
     * @param amount the transfer amount
     */
    public void notifyTransferCompleted(String toUser, double amount) {
        addNotification(String.format("✅ Перевод выполнен: %.2f руб. отправлено пользователю %s", amount, toUser));
    }

    /**
     * Adds a notification for low balance warning.
     * @param balance the current balance
     */
    public void notifyLowBalance(double balance) {
        if (balance < 1000 && balance > 0) {
            addNotification(String.format("⚠️ Низкий баланс: %.2f руб.", balance));
        } else if (balance <= 0) {
            addNotification(String.format("🚨 Отрицательный баланс: %.2f руб.!", balance));
        }
    }

    /**
     * Gets a formatted summary of all notifications.
     * @return formatted notifications summary
     */
    public String getNotificationsSummary() {
        if (notifications.isEmpty()) {
            return "📭 Нет новых уведомлений.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("📬 Уведомления (%d):\n", notifications.size()));
        
        for (int i = 0; i < notifications.size(); i++) {
            summary.append(String.format("%d. %s\n", i + 1, notifications.get(i)));
        }
        
        return summary.toString();
    }

    /**
     * Gets the most recent notifications (up to specified count).
     * @param count maximum number of notifications to return
     * @return list of recent notifications
     */
    public List<String> getRecentNotifications(int count) {
        if (notifications.isEmpty()) {
            return List.of();
        }
        
        int startIndex = Math.max(0, notifications.size() - count);
        return notifications.subList(startIndex, notifications.size());
    }

    /**
     * Checks if there are any critical notifications.
     * @return true if there are critical notifications (containing warning icons)
     */
    public boolean hasCriticalNotifications() {
        return notifications.stream()
                .anyMatch(notification -> notification.contains("🚨") || notification.contains("⚠️"));
    }

    /**
     * Gets only critical notifications.
     * @return list of critical notifications
     */
    public List<String> getCriticalNotifications() {
        return notifications.stream()
                .filter(notification -> notification.contains("🚨") || notification.contains("⚠️"))
                .toList();
    }
}
