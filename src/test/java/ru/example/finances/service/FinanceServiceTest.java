package ru.example.finances.service;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.finances.model.Transaction;
import ru.example.finances.model.Wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceServiceTest {
    
    private FinanceService financeService;
    private AuthenticationService authService;
    private static final String TEST_DATA_DIR = "test_data";
    
    @BeforeEach
    void setUp() {
        // Set test data directory
        System.setProperty("test.data.dir", TEST_DATA_DIR);
        
        // Clean up any existing test data before each test
        cleanupTestData();
        
        authService = new AuthenticationService();
        financeService = new FinanceService();
        
        // Register and login test user
        authService.register("testuser", "password123", "test@example.com");
        authService.login("testuser", "password123");
        
        // Create a fresh wallet for each test
        financeService.createNewWallet("testuser");
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
    void testAddIncome() {
        var result = financeService.addIncome("Зарплата", 50000.0, "Месячная зарплата");
        
        assertTrue(result.isSuccess());
        assertEquals(50000.0, financeService.getBalance());
        assertEquals(50000.0, financeService.getTotalIncome());
        assertEquals(0.0, financeService.getTotalExpenses());
    }
    
    @Test
    void testAddExpense() {
        // Add some income first
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        
        var result = financeService.addExpense("Еда", 5000.0, "Покупки в магазине");
        
        assertTrue(result.isSuccess());
        assertEquals(45000.0, financeService.getBalance());
        assertEquals(50000.0, financeService.getTotalIncome());
        assertEquals(5000.0, financeService.getTotalExpenses());
    }
    
    @Test
    void testAddIncomeWithInvalidData() {
        // Test negative amount
        var result1 = financeService.addIncome("Test", -1000.0, "Test");
        assertFalse(result1.isSuccess());
        
        // Test zero amount
        var result2 = financeService.addIncome("Test", 0.0, "Test");
        assertFalse(result2.isSuccess());
        
        // Test null category
        var result3 = financeService.addIncome(null, 1000.0, "Test");
        assertFalse(result3.isSuccess());
        
        // Test empty category
        var result4 = financeService.addIncome("", 1000.0, "Test");
        assertFalse(result4.isSuccess());
    }
    
    @Test
    void testAddExpenseWithInvalidData() {
        // Test negative amount
        var result1 = financeService.addExpense("Test", -1000.0, "Test");
        assertFalse(result1.isSuccess());
        
        // Test zero amount
        var result2 = financeService.addExpense("Test", 0.0, "Test");
        assertFalse(result2.isSuccess());
        
        // Test null category
        var result3 = financeService.addExpense(null, 1000.0, "Test");
        assertFalse(result3.isSuccess());
        
        // Test empty category
        var result4 = financeService.addExpense("", 1000.0, "Test");
        assertFalse(result4.isSuccess());
    }
    
    @Test
    void testGetIncomeByCategory() {
        financeService.addIncome("Зарплата", 50000.0, "Зарплата 1");
        financeService.addIncome("Зарплата", 45000.0, "Зарплата 2");
        financeService.addIncome("Бонус", 10000.0, "Бонус");
        
        Map<String, Double> incomeByCategory = financeService.getIncomeByCategory();
        assertEquals(95000.0, incomeByCategory.get("Зарплата"));
        assertEquals(10000.0, incomeByCategory.get("Бонус"));
    }
    
    @Test
    void testGetExpensesByCategory() {
        financeService.addExpense("Еда", 3000.0, "Покупки 1");
        financeService.addExpense("Еда", 2000.0, "Покупки 2");
        financeService.addExpense("Транспорт", 1500.0, "Проезд");
        
        Map<String, Double> expensesByCategory = financeService.getExpensesByCategory();
        assertEquals(5000.0, expensesByCategory.get("Еда"));
        assertEquals(1500.0, expensesByCategory.get("Транспорт"));
    }
    
    @Test
    void testGetTransactionsByCategory() {
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        financeService.addExpense("Еда", 2000.0, "Покупки 1");
        financeService.addExpense("Еда", 1500.0, "Покупки 2");
        financeService.addExpense("Транспорт", 1000.0, "Проезд");
        
        // Test through category totals
        var incomeByCategory = financeService.getIncomeByCategory();
        assertEquals(50000.0, incomeByCategory.get("Зарплата"));
        
        var expensesByCategory = financeService.getExpensesByCategory();
        assertEquals(3500.0, expensesByCategory.get("Еда"));
        assertEquals(1000.0, expensesByCategory.get("Транспорт"));
        
        // Test total transactions count
        List<Transaction> allTransactions = financeService.getAllTransactions();
        assertEquals(4, allTransactions.size());
    }
    
    @Test
    void testGetAllTransactions() {
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        financeService.addExpense("Еда", 2000.0, "Покупки");
        financeService.addExpense("Транспорт", 1000.0, "Проезд");
        
        List<Transaction> allTransactions = financeService.getAllTransactions();
        assertEquals(3, allTransactions.size());
    }
    
    @Test
    void testAddCategory() {
        var result = financeService.addCategory("Развлечения", "Досуг и отдых", Transaction.Type.EXPENSE);
        
        assertTrue(result.isSuccess());
        
        Wallet wallet = financeService.getCurrentWallet();
        assertTrue(wallet.getCategories().stream()
                .anyMatch(cat -> cat.getName().equals("Развлечения")));
    }
    
    @Test
    void testAddDuplicateCategory() {
        financeService.addCategory("Еда", "Продукты питания", Transaction.Type.EXPENSE);
        
        var result = financeService.addCategory("Еда", "Другое описание", Transaction.Type.EXPENSE);
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    void testAddCategoryWithInvalidData() {
        // Test null name
        var result1 = financeService.addCategory(null, "Description", Transaction.Type.INCOME);
        assertFalse(result1.isSuccess());
        
        // Test empty name
        var result2 = financeService.addCategory("", "Description", Transaction.Type.INCOME);
        assertFalse(result2.isSuccess());
        
        // Test null type
        var result3 = financeService.addCategory("Name", "Description", null);
        assertFalse(result3.isSuccess());
    }
    
    @Test
    void testCalculateTotalsByCategories() {
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        financeService.addIncome("Бонус", 10000.0, "Бонус");
        financeService.addExpense("Еда", 5000.0, "Еда");
        financeService.addExpense("Транспорт", 2000.0, "Транспорт");
        
        // Test totals through existing methods
        assertEquals(60000.0, financeService.getTotalIncome());
        assertEquals(7000.0, financeService.getTotalExpenses());
        
        var incomeByCategory = financeService.getIncomeByCategory();
        assertEquals(50000.0, incomeByCategory.get("Зарплата"));
        assertEquals(10000.0, incomeByCategory.get("Бонус"));
        
        var expensesByCategory = financeService.getExpensesByCategory();
        assertEquals(5000.0, expensesByCategory.get("Еда"));
        assertEquals(2000.0, expensesByCategory.get("Транспорт"));
    }
    
    @Test
    void testDataPersistence() {
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        financeService.addExpense("Еда", 5000.0, "Еда");
        
        // Test that data is stored correctly
        assertEquals(45000.0, financeService.getBalance());
        assertEquals(50000.0, financeService.getTotalIncome());
        assertEquals(5000.0, financeService.getTotalExpenses());
        
        List<Transaction> transactions = financeService.getAllTransactions();
        assertEquals(2, transactions.size());
    }
    
    @Test
    void testWalletPersistence() {
        // Add some data
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        financeService.addExpense("Еда", 5000.0, "Еда");
        
        // Save wallet
        financeService.saveWallet();
        
        // Create new service and load wallet
        FinanceService newFinanceService = new FinanceService();
        newFinanceService.loadWallet("testuser");
        
        // Check that data was persisted
        assertEquals(45000.0, newFinanceService.getBalance());
        assertEquals(50000.0, newFinanceService.getTotalIncome());
        assertEquals(5000.0, newFinanceService.getTotalExpenses());
    }
    
    @Test
    void testLoadNonExistentWallet() {
        FinanceService newFinanceService = new FinanceService();
        newFinanceService.loadWallet("nonexistentuser");
        
        // Should create new empty wallet
        assertEquals(0.0, newFinanceService.getBalance());
        assertEquals(0.0, newFinanceService.getTotalIncome());
        assertEquals(0.0, newFinanceService.getTotalExpenses());
        assertTrue(newFinanceService.getAllTransactions().isEmpty());
    }
}
