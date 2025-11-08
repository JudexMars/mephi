package ru.example.finances.service;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.finances.model.PendingTransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferServiceTest {
    
    private TransferService transferService;
    private FinanceService financeService;
    private AuthenticationService authService;
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
        notificationService = new NotificationService();
        transferService = new TransferService(financeService, authService, notificationService);
        
        // Register test users
        authService.register("sender", "password123", "sender@example.com");
        authService.register("recipient", "password123", "recipient@example.com");
        
        // Create fresh wallets for test users
        financeService.createNewWallet("sender");
        financeService.createNewWallet("recipient");
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
    void testInitiateTransfer() {
        // Login as sender and add some balance
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        
        var result = transferService.initiateTransfer("recipient", 5000.0, "Test transfer");
        
        assertTrue(result.isSuccess());
        assertEquals(45000.0, financeService.getBalance()); // 50000 - 5000
        
        // Check that transfer was created
        List<PendingTransfer> outgoingTransfers = transferService.getOutgoingTransfers();
        assertEquals(1, outgoingTransfers.size());
        assertEquals("recipient", outgoingTransfers.get(0).getToUsername());
        assertEquals(5000.0, outgoingTransfers.get(0).getAmount());
    }
    
    @Test
    void testInitiateTransferWithInsufficientBalance() {
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 1000.0, "Зарплата");
        
        var result = transferService.initiateTransfer("recipient", 5000.0, "Test transfer");
        
        assertFalse(result.isSuccess());
        assertEquals(1000.0, financeService.getBalance()); // Balance unchanged
    }
    
    @Test
    void testInitiateTransferToNonExistentUser() {
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        
        var result = transferService.initiateTransfer("nonexistent", 5000.0, "Test transfer");
        
        assertFalse(result.isSuccess());
        assertEquals(50000.0, financeService.getBalance()); // Balance unchanged
    }
    
    @Test
    void testInitiateTransferToSelf() {
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        
        var result = transferService.initiateTransfer("sender", 5000.0, "Test transfer");
        
        assertFalse(result.isSuccess());
        assertEquals(50000.0, financeService.getBalance()); // Balance unchanged
    }
    
    @Test
    void testInitiateTransferWithInvalidAmount() {
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        
        // Test negative amount
        var result1 = transferService.initiateTransfer("recipient", -1000.0, "Test");
        assertFalse(result1.isSuccess());
        
        // Test zero amount
        var result2 = transferService.initiateTransfer("recipient", 0.0, "Test");
        assertFalse(result2.isSuccess());
    }
    
    @Test
    void testInitiateTransferWithoutLogin() {
        var result = transferService.initiateTransfer("recipient", 5000.0, "Test transfer");
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    void testProcessPendingTransfers() {
        // Sender creates transfer
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        transferService.initiateTransfer("recipient", 5000.0, "Test transfer");
        
        // Recipient logs in and processes transfers
        authService.login("recipient", "password123");
        financeService.createNewWallet("recipient");
        
        var result = transferService.processPendingTransfers();
        
        assertTrue(result.isSuccess());
        assertEquals(5000.0, financeService.getBalance()); // Recipient received money
        
        // Check that transfer is marked as completed
        List<PendingTransfer> pendingTransfers = transferService.getPendingTransfers();
        assertTrue(pendingTransfers.isEmpty()); // No more pending transfers
    }
    
    @Test
    void testProcessPendingTransfersWithNoPendingTransfers() {
        authService.login("recipient", "password123");
        financeService.createNewWallet("recipient");
        
        var result = transferService.processPendingTransfers();
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testProcessMultiplePendingTransfers() {
        // Sender creates multiple transfers
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        transferService.initiateTransfer("recipient", 3000.0, "Transfer 1");
        transferService.initiateTransfer("recipient", 2000.0, "Transfer 2");
        
        // Recipient processes transfers
        authService.login("recipient", "password123");
        financeService.createNewWallet("recipient");
        
        var result = transferService.processPendingTransfers();
        
        assertTrue(result.isSuccess());
        assertEquals(5000.0, financeService.getBalance());
    }
    
    @Test
    void testCancelTransfer() {
        // Create transfer
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        transferService.initiateTransfer("recipient", 5000.0, "Test transfer");
        
        List<PendingTransfer> outgoingTransfers = transferService.getOutgoingTransfers();
        String transferId = outgoingTransfers.get(0).getId();
        
        var result = transferService.cancelTransfer(transferId);
        
        assertTrue(result.isSuccess());
        assertEquals(50000.0, financeService.getBalance()); // Money refunded
    }
    
    @Test
    void testCancelNonExistentTransfer() {
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        
        var result = transferService.cancelTransfer("nonexistent-id");
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    void testCancelTransferByWrongUser() {
        // Sender creates transfer
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        transferService.initiateTransfer("recipient", 5000.0, "Test transfer");
        
        List<PendingTransfer> outgoingTransfers = transferService.getOutgoingTransfers();
        String transferId = outgoingTransfers.get(0).getId();
        
        // Different user tries to cancel
        authService.login("recipient", "password123");
        financeService.createNewWallet("recipient");
        
        var result = transferService.cancelTransfer(transferId);
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    void testGetTransferHistory() {
        // Create and process transfer
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        transferService.initiateTransfer("recipient", 5000.0, "Test transfer");
        
        authService.login("recipient", "password123");
        financeService.createNewWallet("recipient");
        transferService.processPendingTransfers();
        
        // Check sender's history
        authService.login("sender", "password123");
        String senderHistory = transferService.getTransferHistory();
        assertTrue(senderHistory.contains("ИСТОРИЯ ПЕРЕВОДОВ"));
        assertTrue(senderHistory.contains("Исходящие переводы"));
        assertTrue(senderHistory.contains("recipient"));
        assertTrue(senderHistory.contains("5000.0"));
        assertTrue(senderHistory.contains("Завершен"));
        
        // Check recipient's history
        authService.login("recipient", "password123");
        String recipientHistory = transferService.getTransferHistory();
        assertTrue(recipientHistory.contains("Входящие переводы"));
        assertTrue(recipientHistory.contains("sender"));
        assertTrue(recipientHistory.contains("5000.0"));
        assertTrue(recipientHistory.contains("Завершен"));
    }
    
    @Test
    void testGetTransferHistoryWithNoTransfers() {
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        
        String history = transferService.getTransferHistory();
        
        assertTrue(history.contains("ИСТОРИЯ ПЕРЕВОДОВ"));
        assertTrue(history.contains("Нет переводов"));
    }
    
    @Test
    void testGetPendingTransferCount() {
        // Create transfers for recipient
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        transferService.initiateTransfer("recipient", 3000.0, "Transfer 1");
        transferService.initiateTransfer("recipient", 2000.0, "Transfer 2");
        
        // Check recipient's pending transfer count
        authService.login("recipient", "password123");
        financeService.createNewWallet("recipient");
        
        int pendingCount = transferService.getPendingTransferCount();
        assertEquals(2, pendingCount);
        
        // Process transfers and check count again
        transferService.processPendingTransfers();
        int pendingCountAfter = transferService.getPendingTransferCount();
        assertEquals(0, pendingCountAfter);
    }
    
    @Test
    void testTransferNotificationCreation() {
        // Create transfer
        authService.login("sender", "password123");
        financeService.createNewWallet("sender");
        financeService.addIncome("Зарплата", 50000.0, "Зарплата");
        transferService.initiateTransfer("recipient", 5000.0, "Test transfer");
        
        // Process transfer (should create notification)
        authService.login("recipient", "password123");
        financeService.createNewWallet("recipient");
        transferService.processPendingTransfers();
        
        // Check that notification was created
        List<String> notifications = notificationService.getNotifications();
        assertTrue(notifications.stream()
                .anyMatch(n -> n.contains("Получен перевод от sender")));
    }
}
