package ru.example.finances.ui;

import java.util.Scanner;

import ru.example.finances.model.Transaction;
import ru.example.finances.service.AuthenticationService;
import ru.example.finances.service.BudgetService;
import ru.example.finances.service.FinanceService;
import ru.example.finances.service.NotificationService;
import ru.example.finances.service.TransferService;
import ru.example.finances.util.ValidationUtils;

/**
 * Console interface for the finance management application.
 * Provides a simple menu-driven interface using Scanner.
 */
public class FinanceConsoleInterface {
    private final Scanner scanner;
    private final AuthenticationService authService;
    private final FinanceService financeService;
    private final BudgetService budgetService;
    private final TransferService transferService;
    private final NotificationService notificationService;
    private boolean running;

    public FinanceConsoleInterface() {
        this.scanner = new Scanner(System.in);
        this.authService = new AuthenticationService();
        this.financeService = new FinanceService();
        this.budgetService = new BudgetService(financeService);
        this.notificationService = new NotificationService();
        this.transferService = new TransferService(financeService, authService, notificationService);
        this.running = true;
    }

    /**
     * Starts the console interface.
     */
    public void start() {
        printWelcome();
        
        while (running) {
            try {
                if (authService.isLoggedIn()) {
                    showMainMenu();
                } else {
                    showAuthMenu();
                }
            } catch (Exception e) {
                System.err.println("Произошла ошибка: " + e.getMessage());
                System.out.println("Нажмите Enter для продолжения...");
                scanner.nextLine();
            }
        }
        
        cleanup();
    }

    /**
     * Prints welcome message.
     */
    private void printWelcome() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     СИСТЕМА УПРАВЛЕНИЯ ФИНАНСАМИ     ║");
        System.out.println("║              Добро пожаловать!       ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Shows authentication menu for non-logged users.
     */
    private void showAuthMenu() {
        System.out.println("\n=== МЕНЮ ВХОДА ===");
        System.out.println("1. Войти в систему");
        System.out.println("2. Зарегистрироваться");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> handleLogin();
            case "2" -> handleRegistration();
            case "0" -> {
                running = false;
                System.out.println("До свидания!");
            }
            default -> System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    /**
     * Shows main menu for logged-in users.
     */
    private void showMainMenu() {
        // Process pending transfers on login
        var transferResult = transferService.processPendingTransfers();
        if (transferResult.isSuccess() && !transferResult.getMessage().contains("Нет ожидающих")) {
            System.out.println("📨 " + transferResult.getMessage());
        }

        // Check for notifications
        notificationService.checkBudgetAlerts(budgetService);
        notificationService.checkOverspending(financeService);
        
        if (notificationService.getNotificationCount() > 0) {
            System.out.println("\n" + notificationService.getNotificationsSummary());
        }

        System.out.println("\n=== ГЛАВНОЕ МЕНЮ ===");
        System.out.println("Пользователь: " + authService.getCurrentUsername());
        System.out.println("Баланс: " + String.format("%.2f руб.", financeService.getBalance()));
        
        System.out.println("\n1. Управление финансами");
        System.out.println("2. Управление бюджетами");
        System.out.println("3. Переводы между пользователями");
        System.out.println("4. Отчеты и статистика");
        System.out.println("5. Настройки аккаунта");
        System.out.println("6. Уведомления");
        System.out.println("0. Выйти из аккаунта");
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> showFinanceMenu();
            case "2" -> showBudgetMenu();
            case "3" -> showTransferMenu();
            case "4" -> showReportsMenu();
            case "5" -> showAccountMenu();
            case "6" -> showNotificationsMenu();
            case "0" -> {
                financeService.saveWallet();
                authService.logout();
                System.out.println("Вы вышли из системы.");
            }
            default -> System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    /**
     * Handles user login.
     */
    private void handleLogin() {
        System.out.println("\n=== ВХОД В СИСТЕМУ ===");
        System.out.print("Имя пользователя: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Пароль: ");
        String password = scanner.nextLine().trim();
        
        var result = authService.login(username, password);
        System.out.println(result.getMessage());
        
        if (result.isSuccess()) {
            financeService.loadWallet(username);
            notificationService.clearNotifications();
        }
    }

    /**
     * Handles user registration.
     */
    private void handleRegistration() {
        System.out.println("\n=== РЕГИСТРАЦИЯ ===");
        System.out.print("Имя пользователя (3-20 символов, буквы, цифры, _): ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Пароль (минимум 6 символов): ");
        String password = scanner.nextLine().trim();
        
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        
        var result = authService.register(username, password, email);
        System.out.println(result.getMessage());
    }

    /**
     * Shows finance management menu.
     */
    private void showFinanceMenu() {
        System.out.println("\n=== УПРАВЛЕНИЕ ФИНАНСАМИ ===");
        System.out.println("1. Добавить доход");
        System.out.println("2. Добавить расход");
        System.out.println("3. Просмотреть транзакции");
        System.out.println("4. Управление категориями");
        System.out.println("5. Экспорт данных");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> handleAddIncome();
            case "2" -> handleAddExpense();
            case "3" -> handleViewTransactions();
            case "4" -> showCategoryMenu();
            case "5" -> handleExportData();
            case "0" -> { /* return to main menu */ }
            default -> System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    /**
     * Shows budget management menu.
     */
    private void showBudgetMenu() {
        System.out.println("\n=== УПРАВЛЕНИЕ БЮДЖЕТАМИ ===");
        System.out.println("1. Установить бюджет");
        System.out.println("2. Просмотреть бюджеты");
        System.out.println("3. Изменить бюджет");
        System.out.println("4. Удалить бюджет");
        System.out.println("5. Статистика бюджетов");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> handleSetBudget();
            case "2" -> handleViewBudgets();
            case "3" -> handleUpdateBudget();
            case "4" -> handleRemoveBudget();
            case "5" -> handleBudgetStatistics();
            case "0" -> { /* return to main menu */ }
            default -> System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    /**
     * Shows transfer menu.
     */
    private void showTransferMenu() {
        System.out.println("\n=== ПЕРЕВОДЫ ===");
        System.out.println("1. Отправить перевод");
        System.out.println("2. История переводов");
        System.out.println("3. Отменить перевод");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> handleSendTransfer();
            case "2" -> handleTransferHistory();
            case "3" -> handleCancelTransfer();
            case "0" -> { /* return to main menu */ }
            default -> System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    /**
     * Shows reports menu.
     */
    private void showReportsMenu() {
        System.out.println("\n=== ОТЧЕТЫ И СТАТИСТИКА ===");
        System.out.println("1. Финансовая сводка");
        System.out.println("2. Сводка по бюджетам");
        System.out.println("3. Статистика бюджетов");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> System.out.println("\n" + financeService.getFinancialSummary());
            case "2" -> System.out.println("\n" + budgetService.getBudgetSummary());
            case "3" -> System.out.println("\n" + budgetService.getBudgetStatistics());
            case "0" -> { /* return to main menu */ }
            default -> System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    /**
     * Shows account settings menu.
     */
    private void showAccountMenu() {
        System.out.println("\n=== НАСТРОЙКИ АККАУНТА ===");
        System.out.println("1. Изменить пароль");
        System.out.println("2. Изменить email");
        System.out.println("3. Создать резервную копию");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> handleChangePassword();
            case "2" -> handleChangeEmail();
            case "3" -> handleCreateBackup();
            case "0" -> { /* return to main menu */ }
            default -> System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    /**
     * Shows notifications menu.
     */
    private void showNotificationsMenu() {
        System.out.println("\n=== УВЕДОМЛЕНИЯ ===");
        System.out.println(notificationService.getNotificationsSummary());
        System.out.println("\n1. Очистить уведомления");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> {
                notificationService.clearNotifications();
                System.out.println("Уведомления очищены.");
            }
            case "0" -> { /* return to main menu */ }
            default -> System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    // Handler methods for specific actions
    
    private void handleAddIncome() {
        System.out.println("\n=== ДОБАВИТЬ ДОХОД ===");
        System.out.print("Категория: ");
        String category = scanner.nextLine().trim();
        
        System.out.print("Сумма: ");
        String amountStr = scanner.nextLine().trim();
        Double amount = ValidationUtils.parseAmount(amountStr);
        
        if (amount == null) {
            System.out.println("Неверная сумма.");
            return;
        }
        
        System.out.print("Описание (необязательно): ");
        String description = scanner.nextLine().trim();
        
        var result = financeService.addIncome(category, amount, description);
        System.out.println(result.getMessage());
        
        if (result.isSuccess()) {
            notificationService.notifyIncomeAdded(category, amount);
        }
    }

    private void handleAddExpense() {
        System.out.println("\n=== ДОБАВИТЬ РАСХОД ===");
        System.out.print("Категория: ");
        String category = scanner.nextLine().trim();
        
        System.out.print("Сумма: ");
        String amountStr = scanner.nextLine().trim();
        Double amount = ValidationUtils.parseAmount(amountStr);
        
        if (amount == null) {
            System.out.println("Неверная сумма.");
            return;
        }
        
        System.out.print("Описание (необязательно): ");
        String description = scanner.nextLine().trim();
        
        var result = financeService.addExpense(category, amount, description);
        System.out.println(result.getMessage());
        
        if (result.isSuccess()) {
            notificationService.notifyExpenseAdded(category, amount);
            
            // Check if budget is exceeded
            var budget = budgetService.getBudget(category);
            if (budget != null && budget.isExceeded()) {
                notificationService.notifyBudgetExceeded(budget);
            }
        }
    }

    private void handleViewTransactions() {
        System.out.println("\n=== ТРАНЗАКЦИИ ===");
        var transactions = financeService.getAllTransactions();
        
        if (transactions.isEmpty()) {
            System.out.println("Нет транзакций.");
            return;
        }
        
        System.out.println("Последние 10 транзакций:");
        int count = Math.min(10, transactions.size());
        for (int i = transactions.size() - count; i < transactions.size(); i++) {
            var transaction = transactions.get(i);
            String type = transaction.getType() == ru.example.finances.model.Transaction.Type.INCOME ? "Доход" : "Расход";
            System.out.printf("%s: %.2f руб. (%s) - %s\n", 
                    type, transaction.getAmount(), transaction.getCategory(), transaction.getDescription());
        }
    }

    private void handleExportData() {
        System.out.print("Путь для экспорта (например, export.json): ");
        String path = scanner.nextLine().trim();
        
        var result = financeService.exportWallet(path);
        System.out.println(result.getMessage());
    }

    private void handleSetBudget() {
        System.out.println("\n=== УСТАНОВИТЬ БЮДЖЕТ ===");
        System.out.print("Категория: ");
        String category = scanner.nextLine().trim();
        
        System.out.print("Лимит: ");
        String limitStr = scanner.nextLine().trim();
        Double limit = ValidationUtils.parseBudgetLimit(limitStr);
        
        if (limit == null) {
            System.out.println("Неверный лимит.");
            return;
        }
        
        var result = budgetService.setBudget(category, limit);
        System.out.println(result.getMessage());
        
        if (result.isSuccess()) {
            notificationService.notifyBudgetSet(category, limit);
        }
    }

    private void handleViewBudgets() {
        System.out.println("\n" + budgetService.getBudgetSummary());
    }

    private void handleUpdateBudget() {
        System.out.println("\n=== ИЗМЕНИТЬ БЮДЖЕТ ===");
        System.out.print("Категория: ");
        String category = scanner.nextLine().trim();
        
        System.out.print("Новый лимит: ");
        String limitStr = scanner.nextLine().trim();
        Double limit = ValidationUtils.parseBudgetLimit(limitStr);
        
        if (limit == null) {
            System.out.println("Неверный лимит.");
            return;
        }
        
        var result = budgetService.updateBudgetLimit(category, limit);
        System.out.println(result.getMessage());
    }

    private void handleRemoveBudget() {
        System.out.println("\n=== УДАЛИТЬ БЮДЖЕТ ===");
        System.out.print("Категория: ");
        String category = scanner.nextLine().trim();
        
        var result = budgetService.removeBudget(category);
        System.out.println(result.getMessage());
    }

    private void handleBudgetStatistics() {
        System.out.println("\n" + budgetService.getBudgetStatistics());
    }

    private void handleSendTransfer() {
        System.out.println("\n=== ОТПРАВИТЬ ПЕРЕВОД ===");
        System.out.print("Получатель (имя пользователя): ");
        String recipient = scanner.nextLine().trim();
        
        System.out.print("Сумма: ");
        String amountStr = scanner.nextLine().trim();
        Double amount = ValidationUtils.parseAmount(amountStr);
        
        if (amount == null) {
            System.out.println("Неверная сумма.");
            return;
        }
        
        System.out.print("Описание: ");
        String description = scanner.nextLine().trim();
        
        var result = transferService.initiateTransfer(recipient, amount, description);
        System.out.println(result.getMessage());
    }

    private void handleTransferHistory() {
        System.out.println("\n" + transferService.getTransferHistory());
    }

    private void handleCancelTransfer() {
        System.out.println("\n=== ОТМЕНИТЬ ПЕРЕВОД ===");
        var outgoing = transferService.getOutgoingTransfers();
        
        if (outgoing.isEmpty()) {
            System.out.println("Нет исходящих переводов для отмены.");
            return;
        }
        
        System.out.println("Ваши исходящие переводы:");
        for (int i = 0; i < outgoing.size(); i++) {
            var transfer = outgoing.get(i);
            System.out.printf("%d. %s: %.2f руб. - %s [%s]\n", 
                    i + 1, transfer.getToUsername(), transfer.getAmount(), 
                    transfer.getDescription(), transfer.getStatus());
        }
        
        System.out.print("Номер перевода для отмены (0 - отмена): ");
        String choiceStr = scanner.nextLine().trim();
        
        try {
            int choice = Integer.parseInt(choiceStr);
            if (choice > 0 && choice <= outgoing.size()) {
                var transfer = outgoing.get(choice - 1);
                var result = transferService.cancelTransfer(transfer.getId());
                System.out.println(result.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Неверный номер.");
        }
    }

    private void handleChangePassword() {
        System.out.println("\n=== ИЗМЕНИТЬ ПАРОЛЬ ===");
        System.out.print("Текущий пароль: ");
        String oldPassword = scanner.nextLine().trim();
        
        System.out.print("Новый пароль: ");
        String newPassword = scanner.nextLine().trim();
        
        var result = authService.changePassword(oldPassword, newPassword);
        System.out.println(result.getMessage());
    }

    private void handleChangeEmail() {
        System.out.println("\n=== ИЗМЕНИТЬ EMAIL ===");
        System.out.print("Новый email: ");
        String newEmail = scanner.nextLine().trim();
        
        var result = authService.updateEmail(newEmail);
        System.out.println(result.getMessage());
    }

    private void handleCreateBackup() {
        var result = financeService.createBackup();
        System.out.println(result.getMessage());
    }

    /**
     * Cleanup resources.
     */
    private void cleanup() {
        if (authService.isLoggedIn()) {
            financeService.saveWallet();
        }
        scanner.close();
    }

    /**
     * Shows category management menu.
     */
    private void showCategoryMenu() {
        System.out.println("\n=== УПРАВЛЕНИЕ КАТЕГОРИЯМИ ===");
        System.out.println("1. Просмотреть категории");
        System.out.println("2. Добавить категорию");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> handleViewCategories();
            case "2" -> handleAddCategory();
            case "0" -> { /* return to finance menu */ }
            default -> System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    /**
     * Handles viewing categories.
     */
    private void handleViewCategories() {
        System.out.println("\n=== СПИСОК КАТЕГОРИЙ ===");
        
        var wallet = financeService.getCurrentWallet();
        if (wallet == null) {
            System.out.println("Кошелек не загружен.");
            return;
        }
        
        var categories = wallet.getCategories();
        if (categories.isEmpty()) {
            System.out.println("Категории не найдены.");
            return;
        }
        
        System.out.println("Доходные категории:");
        categories.stream()
            .filter(cat -> cat.getType() == Transaction.Type.INCOME)
            .forEach(cat -> System.out.printf("  - %s: %s%n", cat.getName(), cat.getDescription()));
            
        System.out.println("\nРасходные категории:");
        categories.stream()
            .filter(cat -> cat.getType() == Transaction.Type.EXPENSE)
            .forEach(cat -> System.out.printf("  - %s: %s%n", cat.getName(), cat.getDescription()));
            
        System.out.println("\nВсего категорий: " + categories.size());
    }

    /**
     * Handles adding a new category.
     */
    private void handleAddCategory() {
        System.out.println("\n=== ДОБАВЛЕНИЕ КАТЕГОРИИ ===");
        
        System.out.print("Название категории: ");
        String name = scanner.nextLine().trim();
        
        if (name.isEmpty()) {
            System.out.println("Название категории не может быть пустым.");
            return;
        }
        
        System.out.print("Описание категории: ");
        String description = scanner.nextLine().trim();
        
        System.out.println("Тип категории:");
        System.out.println("1. Доход");
        System.out.println("2. Расход");
        System.out.print("Выберите тип: ");
        
        String typeChoice = scanner.nextLine().trim();
        Transaction.Type type;
        
        switch (typeChoice) {
            case "1" -> type = Transaction.Type.INCOME;
            case "2" -> type = Transaction.Type.EXPENSE;
            default -> {
                System.out.println("Неверный выбор типа категории.");
                return;
            }
        }
        
        var result = financeService.addCategory(name, description, type);
        System.out.println(result.getMessage());
    }
}
