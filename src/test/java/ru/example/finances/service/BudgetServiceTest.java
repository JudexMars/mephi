package ru.example.finances.service;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.finances.model.Budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetServiceTest {
    
    private BudgetService budgetService;
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
        budgetService = new BudgetService(financeService);
        
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
    void testSetBudget() {
        var result = budgetService.setBudget("Еда", 10000.0);
        
        assertTrue(result.isSuccess());
        
        Budget budget = budgetService.getBudget("Еда");
        assertNotNull(budget);
        assertEquals("Еда", budget.getCategory());
        assertEquals(10000.0, budget.getLimit());
        assertEquals(0.0, budget.getSpent());
    }
    
    @Test
    void testSetBudgetWithInvalidData() {
        // Test null category
        var result1 = budgetService.setBudget(null, 1000.0);
        assertFalse(result1.isSuccess());
        
        // Test empty category
        var result2 = budgetService.setBudget("", 1000.0);
        assertFalse(result2.isSuccess());
        
        // Test negative limit
        var result3 = budgetService.setBudget("Еда", -1000.0);
        assertFalse(result3.isSuccess());
        
        // Note: Zero limit might be valid depending on implementation
        // Let's check what the actual behavior is
    }
    
    @Test
    void testUpdateExistingBudget() {
        budgetService.setBudget("Еда", 10000.0);
        
        var result = budgetService.setBudget("Еда", 15000.0);
        
        assertTrue(result.isSuccess());
        
        Budget budget = budgetService.getBudget("Еда");
        assertEquals(15000.0, budget.getLimit());
    }
    
    @Test
    void testGetBudget() {
        budgetService.setBudget("Еда", 10000.0);
        
        Budget budget = budgetService.getBudget("Еда");
        assertNotNull(budget);
        assertEquals("Еда", budget.getCategory());
        assertEquals(10000.0, budget.getLimit());
        
        Budget nonExistentBudget = budgetService.getBudget("Несуществующая");
        assertNull(nonExistentBudget);
    }
    
    @Test
    void testGetAllBudgets() {
        budgetService.setBudget("Еда", 10000.0);
        budgetService.setBudget("Транспорт", 5000.0);
        budgetService.setBudget("Развлечения", 3000.0);
        
        var budgets = budgetService.getAllBudgets();
        assertEquals(3, budgets.size());
        
        // Check that all budgets are present
        assertTrue(budgets.containsKey("Еда"));
        assertTrue(budgets.containsKey("Транспорт"));
        assertTrue(budgets.containsKey("Развлечения"));
    }
    
    @Test
    void testRemoveBudget() {
        budgetService.setBudget("Еда", 10000.0);
        assertNotNull(budgetService.getBudget("Еда"));
        
        var result = budgetService.removeBudget("Еда");
        
        assertTrue(result.isSuccess());
        assertNull(budgetService.getBudget("Еда"));
    }
    
    @Test
    void testRemoveNonExistentBudget() {
        var result = budgetService.removeBudget("Несуществующая");
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    void testBudgetTrackingWithExpenses() {
        budgetService.setBudget("Еда", 10000.0);
        
        // Add expenses
        financeService.addExpense("Еда", 3000.0, "Покупки 1");
        financeService.addExpense("Еда", 2000.0, "Покупки 2");
        
        Budget budget = budgetService.getBudget("Еда");
        assertEquals(5000.0, budget.getSpent());
        assertEquals(5000.0, budget.getRemainingBudget());
        assertEquals(50.0, budget.getUsagePercentage());
        assertFalse(budget.isExceeded());
    }
    
    @Test
    void testBudgetExceeded() {
        budgetService.setBudget("Еда", 5000.0);
        
        // Add expenses that exceed budget
        financeService.addExpense("Еда", 3000.0, "Покупки 1");
        financeService.addExpense("Еда", 3000.0, "Покупки 2");
        
        Budget budget = budgetService.getBudget("Еда");
        assertEquals(6000.0, budget.getSpent());
        assertEquals(-1000.0, budget.getRemainingBudget());
        assertEquals(120.0, budget.getUsagePercentage());
        assertTrue(budget.isExceeded());
    }
    
    @Test
    void testGetExceededBudgets() {
        budgetService.setBudget("Еда", 5000.0);
        budgetService.setBudget("Транспорт", 3000.0);
        budgetService.setBudget("Развлечения", 2000.0);
        
        // Exceed only food and transport budgets
        financeService.addExpense("Еда", 6000.0, "Много еды");
        financeService.addExpense("Транспорт", 4000.0, "Дорогой проезд");
        financeService.addExpense("Развлечения", 1000.0, "Кино");
        
        List<Budget> exceededBudgets = budgetService.getExceededBudgets();
        assertEquals(2, exceededBudgets.size());
        
        assertTrue(exceededBudgets.stream().anyMatch(b -> b.getCategory().equals("Еда")));
        assertTrue(exceededBudgets.stream().anyMatch(b -> b.getCategory().equals("Транспорт")));
        assertFalse(exceededBudgets.stream().anyMatch(b -> b.getCategory().equals("Развлечения")));
    }
    
    @Test
    void testGetBudgetSummary() {
        budgetService.setBudget("Еда", 10000.0);
        budgetService.setBudget("Транспорт", 5000.0);
        
        financeService.addExpense("Еда", 7000.0, "Покупки");
        financeService.addExpense("Транспорт", 2000.0, "Проезд");
        
        String summary = budgetService.getBudgetSummary();
        
        assertNotNull(summary);
        // Summary should not be empty when budgets exist
        assertFalse(summary.trim().isEmpty());
        
        // Verify budgets are properly tracked
        Budget foodBudget = budgetService.getBudget("Еда");
        Budget transportBudget = budgetService.getBudget("Транспорт");
        assertNotNull(foodBudget);
        assertNotNull(transportBudget);
        assertEquals(7000.0, foodBudget.getSpent());
        assertEquals(2000.0, transportBudget.getSpent());
    }
    
    @Test
    void testBudgetSummaryWithExceededBudgets() {
        budgetService.setBudget("Еда", 5000.0);
        financeService.addExpense("Еда", 7000.0, "Превышение");
        
        String summary = budgetService.getBudgetSummary();
        
        assertNotNull(summary);
        assertFalse(summary.trim().isEmpty());
        
        // Verify budget is exceeded
        Budget budget = budgetService.getBudget("Еда");
        assertNotNull(budget);
        assertEquals(7000.0, budget.getSpent());
        assertTrue(budget.getSpent() > budget.getLimit()); // Budget exceeded
    }
    
    @Test
    void testBudgetSummaryWithNoBudgets() {
        String summary = budgetService.getBudgetSummary();
        
        assertNotNull(summary);
        // When no budgets are set, summary should not be empty
        assertFalse(summary.trim().isEmpty());
    }
    
    @Test
    void testBudgetWithIncomeTransactions() {
        budgetService.setBudget("Зарплата", 50000.0);
        
        // Add income (should not affect budget spending)
        financeService.addIncome("Зарплата", 60000.0, "Месячная зарплата");
        
        Budget budget = budgetService.getBudget("Зарплата");
        assertEquals(0.0, budget.getSpent()); // Income should not count as spending
    }
    
    @Test
    void testMultipleBudgetsWithSameExpenseCategory() {
        budgetService.setBudget("Еда", 10000.0);
        
        financeService.addExpense("Еда", 2000.0, "Покупки 1");
        financeService.addExpense("Еда", 3000.0, "Покупки 2");
        financeService.addExpense("Еда", 1000.0, "Покупки 3");
        
        Budget budget = budgetService.getBudget("Еда");
        assertEquals(6000.0, budget.getSpent());
        assertEquals(4000.0, budget.getRemainingBudget());
    }
}
