package ru.example.finances.service;

import ru.example.finances.model.PendingTransfer;
import ru.example.finances.storage.TransferStorage;
import ru.example.finances.util.ValidationUtils;

import java.util.List;

/**
 * Service class for managing offline transfers between users.
 * Handles wallet-to-wallet transfers when recipients are not online.
 */
public class TransferService {
    private final TransferStorage transferStorage;
    private final FinanceService financeService;
    private final AuthenticationService authService;
    private final NotificationService notificationService;

    public TransferService(FinanceService financeService, AuthenticationService authService, NotificationService notificationService) {
        this.transferStorage = new TransferStorage();
        this.financeService = financeService;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    /**
     * Initiates a transfer to another user.
     * @param toUsername the recipient username
     * @param amount the transfer amount
     * @param description the transfer description
     * @return transfer result
     */
    public TransferResult initiateTransfer(String toUsername, double amount, String description) {
        String fromUsername = authService.getCurrentUsername();
        if (fromUsername == null) {
            return new TransferResult(false, "Необходимо войти в систему для выполнения перевода.");
        }

        // Validate recipient
        if (!authService.userExists(toUsername)) {
            return new TransferResult(false, "Пользователь-получатель не найден.");
        }

        if (fromUsername.equals(toUsername)) {
            return new TransferResult(false, "Нельзя переводить средства самому себе.");
        }

        // Validate amount
        if (!ValidationUtils.isValidAmount(amount)) {
            return new TransferResult(false, "Неверная сумма перевода. Сумма должна быть положительной.");
        }

        // Check if sender has sufficient balance
        double currentBalance = financeService.getBalance();
        if (currentBalance < amount) {
            return new TransferResult(false, String.format("Недостаточно средств. Баланс: %.2f руб., требуется: %.2f руб.", 
                    currentBalance, amount));
        }

        // Create expense transaction for sender
        String transferDescription = String.format("Перевод пользователю %s: %s", toUsername, description);
        var expenseResult = financeService.addExpense("Переводы", amount, transferDescription);
        if (!expenseResult.isSuccess()) {
            return new TransferResult(false, "Ошибка при создании расходной операции: " + expenseResult.getMessage());
        }

        // Create pending transfer
        PendingTransfer transfer = new PendingTransfer(fromUsername, toUsername, amount, description);
        transferStorage.addPendingTransfer(transfer);

        return new TransferResult(true, String.format("Перевод на сумму %.2f руб. пользователю %s создан. " +
                "Перевод будет обработан при следующем входе получателя в систему.", amount, toUsername));
    }

    /**
     * Processes pending transfers for the current user.
     * @return processing result
     */
    public TransferResult processPendingTransfers() {
        String username = authService.getCurrentUsername();
        if (username == null) {
            return new TransferResult(false, "Необходимо войти в систему.");
        }

        List<PendingTransfer> pendingTransfers = transferStorage.getPendingTransfersFor(username);
        if (pendingTransfers.isEmpty()) {
            return new TransferResult(true, "Нет ожидающих переводов.");
        }

        int processedCount = 0;
        double totalAmount = 0.0;

        for (PendingTransfer transfer : pendingTransfers) {
            // Add income transaction for recipient
            String incomeDescription = String.format("Перевод от %s: %s", transfer.getFromUsername(), transfer.getDescription());
            var incomeResult = financeService.addIncome("Переводы", transfer.getAmount(), incomeDescription);
            
            if (incomeResult.isSuccess()) {
                // Mark transfer as completed
                transferStorage.markTransferCompleted(transfer.getId());
                
                // Add notification about received transfer
                String notificationMessage = String.format("💰 Получен перевод от %s на сумму %.2f руб.: %s", 
                    transfer.getFromUsername(), transfer.getAmount(), transfer.getDescription());
                notificationService.addNotification(notificationMessage);
                
                processedCount++;
                totalAmount += transfer.getAmount();
            }
        }

        if (processedCount > 0) {
            return new TransferResult(true, String.format("Обработано переводов: %d на общую сумму %.2f руб.", 
                    processedCount, totalAmount));
        } else {
            return new TransferResult(false, "Не удалось обработать переводы.");
        }
    }

    /**
     * Gets pending transfers for the current user.
     * @return list of pending transfers
     */
    public List<PendingTransfer> getPendingTransfers() {
        String username = authService.getCurrentUsername();
        if (username == null) {
            return List.of();
        }
        return transferStorage.getPendingTransfersFor(username);
    }

    /**
     * Gets outgoing transfers from the current user.
     * @return list of outgoing transfers
     */
    public List<PendingTransfer> getOutgoingTransfers() {
        String username = authService.getCurrentUsername();
        if (username == null) {
            return List.of();
        }
        return transferStorage.getPendingTransfersFrom(username);
    }

    /**
     * Cancels a pending transfer (only if it's from the current user).
     * @param transferId the transfer ID
     * @return cancellation result
     */
    public TransferResult cancelTransfer(String transferId) {
        String username = authService.getCurrentUsername();
        if (username == null) {
            return new TransferResult(false, "Необходимо войти в систему.");
        }

        PendingTransfer transfer = transferStorage.getPendingTransfer(transferId);
        if (transfer == null) {
            return new TransferResult(false, "Перевод не найден.");
        }

        if (!transfer.getFromUsername().equals(username)) {
            return new TransferResult(false, "Можно отменить только свои переводы.");
        }

        if (!transfer.isPending()) {
            return new TransferResult(false, "Перевод уже обработан и не может быть отменен.");
        }

        // Mark transfer as cancelled
        transferStorage.markTransferCancelled(transferId);

        // Refund the amount to sender
        String refundDescription = String.format("Возврат отмененного перевода пользователю %s", transfer.getToUsername());
        var refundResult = financeService.addIncome("Переводы", transfer.getAmount(), refundDescription);
        
        if (refundResult.isSuccess()) {
            return new TransferResult(true, String.format("Перевод отменен. Сумма %.2f руб. возвращена на ваш счет.", 
                    transfer.getAmount()));
        } else {
            return new TransferResult(false, "Перевод отменен, но возврат средств не удался: " + refundResult.getMessage());
        }
    }

    /**
     * Gets transfer history summary (including completed transfers).
     * @return formatted transfer history
     */
    public String getTransferHistory() {
        String username = authService.getCurrentUsername();
        if (username == null) {
            return "Необходимо войти в систему.";
        }

        List<PendingTransfer> incoming = transferStorage.getAllTransfersFor(username);
        List<PendingTransfer> outgoing = transferStorage.getAllTransfersFrom(username);

        StringBuilder history = new StringBuilder();
        history.append("=== ИСТОРИЯ ПЕРЕВОДОВ ===\n");

        if (!incoming.isEmpty()) {
            history.append("\n--- Входящие переводы ---\n");
            for (PendingTransfer transfer : incoming) {
                String statusText = getStatusText(transfer.getStatus());
                String dateText = transfer.getStatus() != PendingTransfer.Status.PENDING ? 
                    " (" + new java.util.Date(transfer.getProcessedAt()) + ")" : "";
                history.append(String.format("От %s: %.2f руб. - %s [%s]%s\n", 
                        transfer.getFromUsername(), 
                        transfer.getAmount(), 
                        transfer.getDescription(),
                        statusText,
                        dateText));
            }
        }

        if (!outgoing.isEmpty()) {
            history.append("\n--- Исходящие переводы ---\n");
            for (PendingTransfer transfer : outgoing) {
                String statusText = getStatusText(transfer.getStatus());
                String dateText = transfer.getStatus() != PendingTransfer.Status.PENDING ? 
                    " (" + new java.util.Date(transfer.getProcessedAt()) + ")" : "";
                history.append(String.format("Для %s: %.2f руб. - %s [%s]%s\n", 
                        transfer.getToUsername(), 
                        transfer.getAmount(), 
                        transfer.getDescription(),
                        statusText,
                        dateText));
            }
        }

        if (incoming.isEmpty() && outgoing.isEmpty()) {
            history.append("Нет переводов.\n");
        }

        return history.toString();
    }

    /**
     * Converts transfer status to readable text.
     * @param status the transfer status
     * @return readable status text
     */
    private String getStatusText(PendingTransfer.Status status) {
        return switch (status) {
            case PENDING -> "Ожидает";
            case COMPLETED -> "Завершен";
            case CANCELLED -> "Отменен";
        };
    }

    /**
     * Gets count of pending transfers for the current user.
     * @return number of pending transfers
     */
    public int getPendingTransferCount() {
        String username = authService.getCurrentUsername();
        if (username == null) {
            return 0;
        }
        return transferStorage.getPendingTransferCount(username);
    }

    /**
     * Cleans up old completed and cancelled transfers.
     * @param daysOld number of days old to clean up
     * @return cleanup result
     */
    public TransferResult cleanupOldTransfers(int daysOld) {
        long millisecondsOld = (long) daysOld * 24 * 60 * 60 * 1000;
        int removedCount = transferStorage.cleanupOldTransfers(millisecondsOld);
        
        return new TransferResult(true, String.format("Очищено старых переводов: %d", removedCount));
    }

    /**
     * Result class for transfer operations.
     */
    public static class TransferResult {
        private final boolean success;
        private final String message;

        public TransferResult(boolean success, String message) {
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
