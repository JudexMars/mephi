package ru.example.url.shortener;

import ru.example.url.shortener.ui.ConsoleInterface;

public final class UrlShortenerApplication {

    private UrlShortenerApplication() {
    }

    public static void main(String[] args) {
        try {
            // Initialize and start the console interface
            ConsoleInterface consoleInterface = new ConsoleInterface();
            consoleInterface.start();
            
        } catch (Exception e) {
            System.err.println("❌ Fatal error occurred:");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
