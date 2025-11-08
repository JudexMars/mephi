package ru.example.finances.integration;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.finances.model.Budget;
import ru.example.finances.model.PendingTransfer;
import ru.example.finances.model.Transaction;
import ru.example.finances.model.Wallet;
import ru.example.finances.service.AuthenticationService;
import ru.example.finances.service.BudgetService;
import ru.example.finances.service.FinanceService;
import ru.example.finances.service.NotificationService;
import ru.example.finances.service.TransferService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceApplicationIntegrationTest {
    
    private AuthenticationService authService;
    private FinanceService financeService;
    private BudgetService budgetService;
    private TransferService transferService;
    private NotificationService notificationService;
    private static final String TEST_DATA_DIR = "test_data";
    
    @BeforeEach
    void setUp() {
        // Set test data directory
        System.setProperty("test.data.dir", TEST_DATA_DIR);
        
        // Clean up any existing test data before each test
        cleanupTestData();
        
        authService = new AuthenticationService();
        financeService = new FinanceService();
        budgetService = new BudgetService(financeService);
        notificationService = new NotificationService();
        transferService = new TransferService(financeService, authService, notificationService);
    }
    
    @AfterEach
    void tearDown() {
        authService.logout();
        cleanupTestData();
        // Clear test data directory property
        System.clearProperty("test.data.dir");
    }
    
    private void cleanupTestData() {
        // Clean up test data directory
        File dataDir = new File(TEST_DATA_DIR);
        if (dataDir.exists()) {
            File[] files = dataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dataDir.delete();
        }
        
        // Also clean up default data directory
        File defaultDataDir = new File("data");
        if (defaultDataDir.exists()) {
            File[] files = defaultDataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            defaultDataDir.delete();
        }
    }
    
    @Test
    void testCompleteUserWorkflow() {
        // 1. User Registration and Login
        var registerResult = authService.register("john", "password123", "john@example.com");
        assertTrue(registerResult.isSuccess());
        
        var loginResult = authService.login("john", "password123");
        assertTrue(loginResult.isSuccess());
        
        financeService.createNewWallet("john");
        
        // 2. Add Income
        financeService.addIncome("Зарплата", 50000.0, "Месячная зарплата");
        financeService.addIncome("Бонус", 10000.0, "Квартальный бонус");
        
        assertEquals(60000.0, financeService.getTotalIncome());
        assertEquals(60000.0, financeService.getBalance());
        
        // 3. Set Budgets
        budgetService.setBudget("Еда", 15000.0);
        budgetService.setBudget("Транспорт", 5000.0);
        budgetService.setBudget("Развлечения", 3000.0);
        
        // 4. Add Expenses
        financeService.addExpense("Еда", 8000.0, "Продукты за месяц");
        financeService.addExpense("Транспорт", 3000.0, "Проездной");
        financeService.addExpense("Развлечения", 2000.0, "Кино и рестораны");
        
        assertEquals(13000.0, financeService.getTotalExpenses());
        assertEquals(47000.0, financeService.getBalance());
        
        // 5. Check Budget Status
        List<Budget> exceededBudgets = budgetService.getExceededBudgets();
        assertTrue(exceededBudgets.isEmpty()); // No budgets exceeded
        
        Budget foodBudget = budgetService.getBudget("Еда");
        assertEquals(8000.0, foodBudget.getSpent());
        assertEquals(7000.0, foodBudget.getRemainingBudget());
        assertFalse(foodBudget.isExceeded());
        
        // 6. Exceed a Budget
        financeService.addExpense("Еда", 10000.0, "Дорогие покупки");
        
        exceededBudgets = budgetService.getExceededBudgets();
        assertEquals(1, exceededBudgets.size());
        assertEquals("Еда", exceededBudgets.get(0).getCategory());
        
        // 7. Save and Reload Wallet
        financeService.saveWallet();
        
        FinanceService newFinanceService = new FinanceService();
        newFinanceService.loadWallet("john");
        
        assertEquals(37000.0, newFinanceService.getBalance()); // 60000 - 23000
        assertEquals(60000.0, newFinanceService.getTotalIncome());
        assertEquals(23000.0, newFinanceService.getTotalExpenses());
    }
    
    @Test
    void testMultiUserTransferWorkflow() {
        // 1. Register two users
        authService.register("alice", "password123", "alice@example.com");
        authService.register("bob", "password123", "bob@example.com");
        
        // 2. Alice adds income and initiates transfer
        authService.login("alice", "password123");
        financeService.createNewWallet("alice");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        
        var transferResult = transferService.initiateTransfer("bob", 5000.0, "Возврат долга");
        assertTrue(transferResult.isSuccess());
        assertEquals(45000.0, financeService.getBalance());
        
        // 3. Check Alice's outgoing transfers
        List<PendingTransfer> aliceOutgoing = transferService.getOutgoingTransfers();
        assertEquals(1, aliceOutgoing.size());
        assertEquals("bob", aliceOutgoing.get(0).getToUsername());
        assertEquals(5000.0, aliceOutgoing.get(0).getAmount());
        
        // 4. Bob logs in and processes pending transfers
        authService.login("bob", "password123");
        financeService.createNewWallet("bob");
        
        int pendingCount = transferService.getPendingTransferCount();
        assertEquals(1, pendingCount);
        
        var processResult = transferService.processPendingTransfers();
        assertTrue(processResult.isSuccess());
        assertEquals(5000.0, financeService.getBalance());
        
        // 5. Check that notification was created
        List<String> notifications = notificationService.getNotifications();
        assertTrue(notifications.stream()
                .anyMatch(n -> n.contains("Получен перевод от alice")));
        
        // 6. Check transfer history
        String bobHistory = transferService.getTransferHistory();
        assertTrue(bobHistory.contains("Входящие переводы"));
        assertTrue(bobHistory.contains("alice"));
        assertTrue(bobHistory.contains("Завершен"));
        
        // 7. Check Alice's transfer history
        authService.login("alice", "password123");
        String aliceHistory = transferService.getTransferHistory();
        assertTrue(aliceHistory.contains("Исходящие переводы"));
        assertTrue(aliceHistory.contains("bob"));
        assertTrue(aliceHistory.contains("Завершен"));
    }
    
    @Test
    void testBudgetExceedanceNotificationWorkflow() {
        // 1. Setup user
        authService.register("user", "password123", "user@example.com");
        authService.login("user", "password123");
        financeService.createNewWallet("user");
        
        // 2. Add income and set budget
        financeService.addIncome("Зарплата", 30000.0, "Зарплата");
        budgetService.setBudget("Еда", 5000.0);
        
        // 3. Add expenses within budget
        financeService.addExpense("Еда", 3000.0, "Покупки 1");
        
        Budget budget = budgetService.getBudget("Еда");
        assertFalse(budget.isExceeded());
        
        // 4. Exceed budget
        financeService.addExpense("Еда", 3000.0, "Покупки 2");
        
        budget = budgetService.getBudget("Еда");
        assertTrue(budget.isExceeded());
        assertEquals(6000.0, budget.getSpent());
        assertEquals(-1000.0, budget.getRemainingBudget());
        
        // 5. Check exceeded budgets
        List<Budget> exceededBudgets = budgetService.getExceededBudgets();
        assertEquals(1, exceededBudgets.size());
        assertEquals("Еда", exceededBudgets.get(0).getCategory());
    }
    
    @Test
    void testCategoryAutoCreationWorkflow() {
        // 1. Setup user
        authService.register("user", "password123", "user@example.com");
        authService.login("user", "password123");
        financeService.createNewWallet("user");
        
        // 2. Add transaction with new category
        financeService.addIncome("Фриланс", 15000.0, "Проект по веб-разработке");
        
        // 3. Check that category was auto-created
        Wallet wallet = financeService.getCurrentWallet();
        assertTrue(wallet.getCategories().stream()
                .anyMatch(cat -> cat.getName().equals("Фриланс") && 
                                cat.getType() == Transaction.Type.INCOME));
        
        // 4. Add expense with another new category
        financeService.addExpense("Образование", 8000.0, "Курсы программирования");
        
        // 5. Check that expense category was auto-created
        assertTrue(wallet.getCategories().stream()
                .anyMatch(cat -> cat.getName().equals("Образование") && 
                                cat.getType() == Transaction.Type.EXPENSE));
        
        assertEquals(2, wallet.getCategories().size());
    }
    
    @Test
    void testTransferCancellationWorkflow() {
        // 1. Setup users
        authService.register("sender", "password123", "sender@example.com");
        authService.register("recipient", "password123", "recipient@example.com");
        
        // 2. Sender creates transfer
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 30000.0, "Зарплата");
        
        transferService.initiateTransfer("recipient", 5000.0, "Test transfer");
        assertEquals(25000.0, financeService.getBalance());
        
        // 3. Get transfer ID and cancel it
        List<PendingTransfer> outgoingTransfers = transferService.getOutgoingTransfers();
        String transferId = outgoingTransfers.get(0).getId();
        
        var cancelResult = transferService.cancelTransfer(transferId);
        assertTrue(cancelResult.isSuccess());
        assertEquals(30000.0, financeService.getBalance()); // Money refunded
        
        // 4. Check that recipient has no pending transfers
        authService.login("recipient", "password123");
        financeService.createNewWallet("recipient");
        
        int pendingCount = transferService.getPendingTransferCount();
        assertEquals(0, pendingCount);
        
        // 5. Check transfer history shows cancelled transfer
        authService.login("sender", "password123");
        String history = transferService.getTransferHistory();
        assertTrue(history.contains("Отменен"));
    }
    
    @Test
    void testCompleteFinancialReportingWorkflow() {
        // 1. Setup user with comprehensive financial data
        authService.register("user", "password123", "user@example.com");
        authService.login("user", "password123");
        financeService.createNewWallet("user");
        
        // 2. Add diverse income sources
        financeService.addIncome("Зарплата", 50000.0, "Основная работа");
        financeService.addIncome("Зарплата", 45000.0, "Основная работа");
        financeService.addIncome("Фриланс", 15000.0, "Дополнительный проект");
        financeService.addIncome("Инвестиции", 5000.0, "Дивиденды");
        
        // 3. Add expenses across multiple categories
        financeService.addExpense("Еда", 12000.0, "Продукты и рестораны");
        financeService.addExpense("Транспорт", 8000.0, "Проездной и такси");
        financeService.addExpense("Жилье", 25000.0, "Аренда квартиры");
        financeService.addExpense("Развлечения", 6000.0, "Кино, театры, игры");
        
        // 4. Set budgets
        budgetService.setBudget("Еда", 15000.0);
        budgetService.setBudget("Транспорт", 10000.0);
        budgetService.setBudget("Развлечения", 5000.0);
        
        // 5. Verify totals
        assertEquals(115000.0, financeService.getTotalIncome());
        assertEquals(51000.0, financeService.getTotalExpenses());
        assertEquals(64000.0, financeService.getBalance());
        
        // 6. Verify income by category
        var incomeByCategory = financeService.getIncomeByCategory();
        assertEquals(95000.0, incomeByCategory.get("Зарплата"));
        assertEquals(15000.0, incomeByCategory.get("Фриланс"));
        assertEquals(5000.0, incomeByCategory.get("Инвестиции"));
        
        // 7. Verify expenses by category
        var expensesByCategory = financeService.getExpensesByCategory();
        assertEquals(12000.0, expensesByCategory.get("Еда"));
        assertEquals(8000.0, expensesByCategory.get("Транспорт"));
        assertEquals(25000.0, expensesByCategory.get("Жилье"));
        assertEquals(6000.0, expensesByCategory.get("Развлечения"));
        
        // 8. Check budget status
        List<Budget> exceededBudgets = budgetService.getExceededBudgets();
        assertEquals(1, exceededBudgets.size());
        assertEquals("Развлечения", exceededBudgets.get(0).getCategory());
        
        // 9. Verify final state
        assertEquals(115000.0, financeService.getTotalIncome());
        assertEquals(51000.0, financeService.getTotalExpenses());
        assertEquals(64000.0, financeService.getBalance());
    }
}
