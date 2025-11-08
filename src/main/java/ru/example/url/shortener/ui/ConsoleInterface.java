package ru.example.url.shortener.ui;

import ru.example.url.shortener.config.ConfigLoader;
import ru.example.url.shortener.model.ShortenedUrl;
import ru.example.url.shortener.model.User;
import ru.example.url.shortener.service.UrlShortenerService;
import ru.example.url.shortener.util.NotificationService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleInterface {
    private final UrlShortenerService urlService;
    private final NotificationService notificationService;
    private final Scanner scanner;
    private User currentUser;
    private boolean running;
    private boolean userSessionActive;

    public ConsoleInterface() {
        this.urlService = new UrlShortenerService();
        this.notificationService = NotificationService.getInstance();
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    /**
     * Starts the console interface
     */
    public void start() {
        displayWelcome();
        
        while (running) {
            initializeUser();
            userSessionActive = true;
            
            // User session loop
            while (running && userSessionActive) {
            displayNotifications();
            displayMenu();
            handleUserChoice();
            }
            
            if (running) {
                System.out.println("👋 User session ended.");
                System.out.println("Press Enter to continue with a new user session...");
                scanner.nextLine();
                displayWelcome();
            }
        }
        
        displayGoodbye();
        
        // Clean up notifications on exit
        notificationService.clearAllNotifications();
        urlService.shutdown();
    }

    private static void displayWelcome() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔗 WELCOME TO URL SHORTENER SERVICE");
        System.out.println("=".repeat(60));
        System.out.println("Transform your long URLs into short, manageable links!");
        System.out.println("Each user gets unique short URLs with click tracking.");
        System.out.println("=".repeat(60));
    }

    private void initializeUser() {
        System.out.println("\n🆔 USER IDENTIFICATION");
        System.out.println("1. Create new user session");
        System.out.println("2. Continue with existing user ID");
        System.out.print("Choose option (1-2): ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                currentUser = urlService.createUser();
                System.out.println("✅ New user created!");
                System.out.println("📋 Your User ID: " + currentUser.getUuid());
                System.out.println("💡 Save this ID to continue your session later.");
                break;
            case "2":
                System.out.print("Enter your User ID: ");
                String userIdStr = scanner.nextLine().trim();
                try {
                    UUID userId = UUID.fromString(userIdStr);
                    currentUser = urlService.getUser(userId);
                    if (currentUser == null) {
                        System.out.println("❌ User not found. Creating new user...");
                        currentUser = urlService.createUser();
                        System.out.println("📋 Your new User ID: " + currentUser.getUuid());
                    } else {
                        System.out.println("✅ Welcome back!");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("❌ Invalid User ID format. Creating new user...");
                    currentUser = urlService.createUser();
                    System.out.println("📋 Your new User ID: " + currentUser.getUuid());
                }
                break;
            default:
                System.out.println("Invalid choice. Creating new user...");
                currentUser = urlService.createUser();
                System.out.println("📋 Your User ID: " + currentUser.getUuid());
        }
    }

    private void displayNotifications() {
        if (notificationService.hasNotifications(currentUser.getUuid())) {
            notificationService.displayNotifications(currentUser.getUuid());
        }
    }

    private static void displayMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔗 URL SHORTENER - MAIN MENU");
        System.out.println("=".repeat(60));
        System.out.println("1. 📝 Create short URL");
        System.out.println("2. 📋 View my URLs");
        System.out.println("3. 🌐 Access short URL");
        System.out.println("4. 📊 View URL statistics");
        System.out.println("5. 👤 User information");
        System.out.println("6. 🔧 System statistics");
        System.out.println("7. ⚙️ Manage my URLs");
        System.out.println("8. 🧹 Clear my notifications");
        System.out.println("9. 🚪 Switch user (logout)");
        System.out.println("10. ❌ Exit application");
        System.out.println("=".repeat(60));
        System.out.print("Choose option (1-10): ");
    }

    private void handleUserChoice() {
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                createShortUrl();
                break;
            case "2":
                viewMyUrls();
                break;
            case "3":
                accessShortUrl();
                break;
            case "4":
                viewUrlStatistics();
                break;
            case "5":
                viewUserInformation();
                break;
            case "6":
                viewSystemStatistics();
                break;
            case "7":
                manageUrls();
                break;
            case "8":
                clearNotifications();
                break;
            case "9":
                switchUser();
                break;
            case "10":
                running = false;
                break;
            default:
                System.out.println("❌ Invalid choice. Please try again.");
        }
    }

    private void createShortUrl() {
        System.out.println("\n📝 CREATE SHORT URL");
        System.out.println("-".repeat(40));
        
        System.out.print("Enter the URL to shorten: ");
        String originalUrl = scanner.nextLine().trim();
        
        if (originalUrl.isEmpty()) {
            System.out.println("❌ URL cannot be empty.");
            return;
        }
        
        try {
            ShortenedUrl shortenedUrl = urlService.shortenUrl(originalUrl, currentUser.getUuid());
            
            System.out.println("\n✅ SUCCESS!");
            System.out.println("📋 Original URL: " + shortenedUrl.getOriginalUrl());
            System.out.println("🔗 Short URL: " + shortenedUrl.getFullShortUrl());
            System.out.println("🎯 Click limit: " + shortenedUrl.getMaxClicks());
            System.out.println("⏰ Expires: " + shortenedUrl.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    private void viewMyUrls() {
        System.out.println("\n📋 MY SHORTENED URLS");
        System.out.println("-".repeat(40));
        
        List<ShortenedUrl> urls = urlService.getUserUrls(currentUser.getUuid());
        
        if (urls.isEmpty()) {
            System.out.println("📭 You haven't created any short URLs yet.");
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        for (int i = 0; i < urls.size(); i++) {
            ShortenedUrl url = urls.get(i);
            System.out.println("\n" + (i + 1) + ". " + url.getFullShortUrl());
            System.out.println("   📋 Original: " + url.getOriginalUrl());
            System.out.println("   📊 Clicks: " + url.getClickCount() + "/" + url.getMaxClicks());
            System.out.println("   📅 Created: " + url.getCreatedAt().format(formatter));
            System.out.println("   ⏰ Expires: " + url.getExpiresAt().format(formatter));
            System.out.println("   🔘 Status: " + getStatusDisplay(url));
        }
    }

    private void accessShortUrl() {
        System.out.println("\n🌐 ACCESS SHORT URL");
        System.out.println("-".repeat(40));
        
        System.out.print("Enter short URL or just the code (e.g., clck.ru/abc123 or abc123): ");
        String input = scanner.nextLine().trim();
        
        if (input.isEmpty()) {
            System.out.println("❌ Input cannot be empty.");
            return;
        }
        
        String shortCode = extractShortCode(input);
        
        if (shortCode == null) {
            System.out.println("❌ Invalid short URL format.");
            return;
        }
        
        System.out.println("🔄 Accessing: clck.ru/" + shortCode);
        urlService.accessUrl(shortCode);
    }

    private void viewUrlStatistics() {
        System.out.println("\n📊 URL STATISTICS");
        System.out.println("-".repeat(40));
        
        System.out.print("Enter short code: ");
        String shortCode = scanner.nextLine().trim();
        
        if (shortCode.isEmpty()) {
            System.out.println("❌ Short code cannot be empty.");
            return;
        }
        
        ShortenedUrl url = urlService.getUrlInfo(shortCode);
        
        if (url == null) {
            System.out.println("❌ Short URL not found or invalid format.");
            System.out.println("💡 Make sure the short code contains only letters and numbers.");
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        System.out.println("\n📊 URL DETAILS");
        System.out.println("🔗 Short URL: " + url.getFullShortUrl());
        System.out.println("📋 Original URL: " + url.getOriginalUrl());
        System.out.println("👤 Owner: " + (url.getUserId().equals(currentUser.getUuid()) ? "You" : "Another user"));
        System.out.println("📊 Clicks: " + url.getClickCount() + "/" + url.getMaxClicks());
        System.out.println("📅 Created: " + url.getCreatedAt().format(formatter));
        System.out.println("⏰ Expires: " + url.getExpiresAt().format(formatter));
        System.out.println("🔘 Status: " + getStatusDisplay(url));
    }

    private void viewUserInformation() {
        System.out.println("\n👤 USER INFORMATION");
        System.out.println("-".repeat(40));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        System.out.println("🆔 User ID: " + currentUser.getUuid());
        System.out.println("📅 Created: " + currentUser.getCreatedAt().format(formatter));
        System.out.println("🔗 Total URLs: " + currentUser.getShortenedUrls().size());
        System.out.println("📬 Notifications: " + notificationService.getNotificationCount(currentUser.getUuid()));
    }

    private void viewSystemStatistics() {
        System.out.println("\n🔧 SYSTEM STATISTICS");
        System.out.println("-".repeat(40));
        System.out.println(urlService.getStatistics());
    }

    private void clearNotifications() {
        System.out.println("\n🧹 CLEAR NOTIFICATIONS");
        System.out.println("-".repeat(40));
        
        int notificationCount = notificationService.getNotificationCount(currentUser.getUuid());
        
        if (notificationCount == 0) {
            System.out.println("📭 You have no pending notifications to clear.");
            return;
        }
        
        System.out.println("📬 You have " + notificationCount + " pending notification(s).");
        System.out.print("Are you sure you want to clear all notifications? (y/N): ");
        
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if (confirmation.equals("y") || confirmation.equals("yes")) {
            notificationService.clearNotifications(currentUser.getUuid());
            System.out.println("✅ All notifications cleared successfully!");
        } else {
            System.out.println("❌ Operation cancelled. Notifications preserved.");
        }
    }

    private void switchUser() {
        System.out.println("\n🚪 SWITCH USER");
        System.out.println("-".repeat(40));
        System.out.println("👤 Current user: " + currentUser.getUuid());
        System.out.print("Are you sure you want to logout and switch to another user? (y/N): ");
        
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (confirmation.equals("y") || confirmation.equals("yes")) {
            System.out.println("✅ Logging out current user...");
            System.out.println("🧹 Clearing console...");
            
            // End current user session
            userSessionActive = false;
        } else {
            System.out.println("❌ Operation cancelled. Staying with current user.");
        }
    }

    private void manageUrls() {
        System.out.println("\n⚙️ MANAGE MY URLs");
        System.out.println("-".repeat(40));
        
        List<ShortenedUrl> userUrls = urlService.getUserUrls(currentUser.getUuid());
        if (userUrls.isEmpty()) {
            System.out.println("📭 You have no URLs to manage.");
            return;
        }
        
        // Display user's URLs
        System.out.println("📋 Your URLs:");
        for (int i = 0; i < userUrls.size(); i++) {
            ShortenedUrl url = userUrls.get(i);
            System.out.printf("%d. %s/%s - %s (%d/%d clicks)%n", 
                i + 1, 
                ConfigLoader.getBaseUrl(), 
                url.getShortCode(),
                getStatusDisplay(url),
                url.getClickCount(),
                url.getMaxClicks()
            );
        }
        
        System.out.println("\n🔧 Management Options:");
        System.out.println("1. 📊 Update click limit");
        System.out.println("2. ⏰ Extend expiration time");
        System.out.println("3. 🚫 Deactivate URL");
        System.out.println("4. ↩️ Back to main menu");
        System.out.print("Choose option (1-4): ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                updateClickLimit();
                break;
            case "2":
                extendExpirationTime();
                break;
            case "3":
                deactivateUrl();
                break;
            case "4":
                return;
            default:
                System.out.println("❌ Invalid choice.");
        }
    }

    private void updateClickLimit() {
        System.out.println("\n📊 UPDATE CLICK LIMIT");
        System.out.println("-".repeat(40));
        
        System.out.print("Enter short code (without " + ConfigLoader.getBaseUrl() + "/): ");
        String shortCode = scanner.nextLine().trim();
        
        if (shortCode.isEmpty()) {
            System.out.println("❌ Short code cannot be empty.");
            return;
        }
        
        System.out.print("Enter new click limit (must be positive): ");
        String limitStr = scanner.nextLine().trim();
        
        try {
            int newLimit = Integer.parseInt(limitStr);
            
            boolean success = urlService.updateClickLimit(shortCode, currentUser.getUuid(), newLimit);
            if (success) {
                System.out.println("✅ Click limit updated successfully!");
                System.out.println("🔗 URL: " + ConfigLoader.getBaseUrl() + "/" + shortCode);
                System.out.println("📊 New limit: " + newLimit + " clicks");
            } else {
                System.out.println("❌ URL not found or you don't have permission to modify it.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format. Please enter a valid integer.");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void extendExpirationTime() {
        System.out.println("\n⏰ EXTEND EXPIRATION TIME");
        System.out.println("-".repeat(40));
        
        System.out.print("Enter short code (without " + ConfigLoader.getBaseUrl() + "/): ");
        String shortCode = scanner.nextLine().trim();
        
        if (shortCode.isEmpty()) {
            System.out.println("❌ Short code cannot be empty.");
            return;
        }
        
        System.out.print("Enter additional hours (must be positive): ");
        String hoursStr = scanner.nextLine().trim();
        
        try {
            int additionalHours = Integer.parseInt(hoursStr);
            
            boolean success = urlService.updateExpirationTime(shortCode, currentUser.getUuid(), additionalHours);
            if (success) {
                System.out.println("✅ Expiration time extended successfully!");
                System.out.println("🔗 URL: " + ConfigLoader.getBaseUrl() + "/" + shortCode);
                System.out.println("⏰ Extended by: " + additionalHours + " hours");
            } else {
                System.out.println("❌ URL not found or you don't have permission to modify it.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format. Please enter a valid integer.");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void deactivateUrl() {
        System.out.println("\n🚫 DEACTIVATE URL");
        System.out.println("-".repeat(40));
        
        System.out.print("Enter short code (without " + ConfigLoader.getBaseUrl() + "/): ");
        String shortCode = scanner.nextLine().trim();
        
        if (shortCode.isEmpty()) {
            System.out.println("❌ Short code cannot be empty.");
            return;
        }
        
        System.out.print("Are you sure you want to deactivate this URL? This action cannot be undone. (y/N): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if (!confirmation.equals("y") && !confirmation.equals("yes")) {
            System.out.println("❌ Operation cancelled.");
            return;
        }
        
        try {
            boolean success = urlService.deactivateUrl(shortCode, currentUser.getUuid());
            if (success) {
                System.out.println("✅ URL deactivated successfully!");
                System.out.println("🔗 URL: " + ConfigLoader.getBaseUrl() + "/" + shortCode);
                System.out.println("🚫 Status: Deactivated");
            } else {
                System.out.println("❌ URL not found, already inactive, or you don't have permission to modify it.");
            }
            
        } catch (SecurityException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private static String extractShortCode(String input) {
        if (input.startsWith("clck.ru/")) {
            return input.substring(8);
        }
        if (input.startsWith("http://clck.ru/") || input.startsWith("https://clck.ru/")) {
            int index = input.lastIndexOf('/');
            return index == -1 ? null : input.substring(index + 1);
        }
        return input;
    }

    private static String getStatusDisplay(ShortenedUrl url) {
        switch (url.getStatus()) {
            case ACTIVE:
                if (url.isExpired()) {
                    return "🔴 EXPIRED";
                } else if (url.isClickLimitReached()) {
                    return "🟡 LIMIT REACHED";
                } else {
                    return "🟢 ACTIVE";
                }
            case EXPIRED:
                return "🔴 EXPIRED";
            case LIMIT_EXCEEDED:
                return "🟡 LIMIT EXCEEDED";
            case INACTIVE:
                return "⚫ INACTIVE";
            default:
                return "❓ UNKNOWN";
        }
    }

    private void displayGoodbye() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("👋 THANK YOU FOR USING URL SHORTENER SERVICE!");
        System.out.println("=".repeat(60));
        if (currentUser != null) {
            System.out.println("💡 Last User ID: " + currentUser.getUuid());
        System.out.println("🔄 Use it to continue your session next time.");
        }
        System.out.println("✨ Application closed successfully.");
        System.out.println("=".repeat(60));
    }
}
