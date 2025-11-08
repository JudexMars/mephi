package ru.example.finances;

import ru.example.finances.ui.FinanceConsoleInterface;

/**
 * Main application class for the Personal Finance Management System.
 * Handles application lifecycle and initialization.
 */
public final class FinanceApplication {

    private FinanceApplication() {
    }

    /**
     * Application entry point.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            // Print application header
            printHeader();
            
            // Initialize and start the console interface
            FinanceConsoleInterface consoleInterface = new FinanceConsoleInterface();
            consoleInterface.start();
            
        } catch (Exception e) {
            System.err.println("Критическая ошибка приложения: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Prints application header with version and copyright information.
     */
    private static void printHeader() {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                СИСТЕМА УПРАВЛЕНИЯ ЛИЧНЫМИ ФИНАНСАМИ            ║");
        System.out.println("║                           Версия 1.0                          ║");
        System.out.println("║                                                                ║");
        System.out.println("║  Функциональность:                                            ║");
        System.out.println("║  • Управление доходами и расходами                            ║");
        System.out.println("║  • Установка и мониторинг бюджетов                            ║");
        System.out.println("║  • Переводы между пользователями                              ║");
        System.out.println("║  • Финансовые отчеты и статистика                            ║");
        System.out.println("║  • Уведомления о превышении бюджетов                         ║");
        System.out.println("║  • Сохранение данных в JSON файлах                           ║");
        System.out.println("║                                                                ║");
        System.out.println("║  Разработано для курса объектно-ориентированного              ║");
        System.out.println("║  программирования                                             ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
