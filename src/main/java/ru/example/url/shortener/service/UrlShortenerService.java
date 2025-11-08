package ru.example.url.shortener.service;

import ru.example.url.shortener.model.ShortenedUrl;
import ru.example.url.shortener.model.User;
import ru.example.url.shortener.model.UrlStatus;
import ru.example.url.shortener.storage.UrlStorage;
import ru.example.url.shortener.util.NotificationService;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class UrlShortenerService {
    private static final int DEFAULT_MAX_CLICKS = 100;
    private static final int DEFAULT_EXPIRATION_HOURS = 24;
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?" + // Protocol (optional)
        "(?!-)" + // Domain cannot start with hyphen
        "(" +
            "([\\w]([\\w\\-]*[\\w])?\\.)+[\\w]([\\w\\-]*[\\w])?" + // Multi-part domain (e.g., example.com)
            "|" +
            "localhost" + // Only allow localhost as single-part domain
        ")" +
        "(?<!-)" + // Domain cannot end with hyphen
        "(:[1-9][0-9]*)?" + // Port (optional, must be positive number)
        "(/[\\w\\-._~:/?#\\[\\]@!&'()*+,;=]*)?" + // Path (optional)
        "$" // End of string
    );

    private final UrlStorage storage;
    private final UrlGenerator urlGenerator;
    private final NotificationService notificationService;
    private final ScheduledExecutorService cleanupExecutor;
    private boolean testMode = false; // Flag to prevent browser opening during tests

    public UrlShortenerService() {
        this.storage = UrlStorage.getInstance();
        this.urlGenerator = new UrlGenerator(storage);
        this.notificationService = NotificationService.getInstance();
        this.cleanupExecutor = Executors.newScheduledThreadPool(1);
        
        // Start cleanup task every 5 minutes
        startCleanupTask();
    }

    /**
     * Creates a new user and returns their UUID
     */
    public User createUser() {
        return storage.createUser();
    }

    /**
     * Gets user by UUID
     */
    public User getUser(UUID userId) {
        return storage.getUser(userId);
    }

    /**
     * Creates a shortened URL for the given original URL and user
     */
    public ShortenedUrl shortenUrl(String originalUrl, UUID userId) {
        return shortenUrl(originalUrl, userId, DEFAULT_MAX_CLICKS, DEFAULT_EXPIRATION_HOURS);
    }

    /**
     * Creates a shortened URL with custom parameters
     */
    public ShortenedUrl shortenUrl(String originalUrl, UUID userId, int maxClicks, int expirationHours) {
        // Validate input
        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException("Invalid URL format: " + originalUrl);
        }
        
        if (maxClicks <= 0) {
            throw new IllegalArgumentException("Max clicks must be positive");
        }
        
        if (expirationHours <= 0) {
            throw new IllegalArgumentException("Expiration hours must be positive");
        }

        // Normalize URL
        String normalizedUrl = normalizeUrl(originalUrl);
        
        // Generate unique short code
        String shortCode = urlGenerator.generateShortCode(normalizedUrl, userId);
        
        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expirationHours);
        
        // Create shortened URL
        ShortenedUrl shortenedUrl = new ShortenedUrl(normalizedUrl, shortCode, userId, maxClicks, expiresAt);
        
        // Save to storage
        storage.saveUrl(shortenedUrl);
        
        // Notify user
        notificationService.notifyUrlCreated(shortenedUrl);
        
        return shortenedUrl;
    }

    /**
     * Accesses a shortened URL by its short code
     *
     * @return
     */
    public boolean accessUrl(String shortCode) {
        // Validate short code format first
        if (!urlGenerator.isValidShortCode(shortCode)) {
            System.out.println("❌ Invalid short code format: " + shortCode);
            System.out.println("💡 Short codes should contain only letters and numbers (a-z, A-Z, 0-9)");
            return false;
        }
        
        ShortenedUrl url = storage.getUrlByShortCode(shortCode);
        
        if (url == null) {
            System.out.println("❌ Short URL not found: clck.ru/" + shortCode);
            return false;
        }

        // Check if URL is accessible
        if (!url.isAccessible()) {
            String reason = getInaccessibilityReason(url);
            notificationService.notifyUrlNotAccessible(url, reason);
            System.out.println("❌ URL is not accessible: " + reason);
            return false;
        }

        // Increment click count
        url.incrementClickCount();
        storage.updateUrl(url);

        // Check if limit reached after increment
        if (url.isClickLimitReached()) {
            notificationService.notifyClickLimitReached(url);
        }

        // Open URL in browser (unless in test mode)
        try {
            if (!testMode && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url.getOriginalUrl()));
                System.out.println("✅ Opening: " + url.getOriginalUrl());
                System.out.println("📊 Clicks: " + url.getClickCount() + "/" + url.getMaxClicks());
                return false;
            }
            System.out.println("✅ URL: " + url.getOriginalUrl());
            System.out.println("📊 Clicks: " + url.getClickCount() + "/" + url.getMaxClicks());
            if (testMode) {
                System.out.println("🧪 Test mode - browser opening disabled");
            } else {
                System.out.println("⚠️  Desktop not supported - cannot open browser automatically");
            }
        } catch (Exception e) {
            System.out.println("❌ Error opening URL: " + e.getMessage());
            System.out.println("📋 URL: " + url.getOriginalUrl());
        }
        return false;
    }

    /**
     * Gets all URLs for a specific user
     */
    public List<ShortenedUrl> getUserUrls(UUID userId) {
        return storage.getUserUrls(userId);
    }

    /**
     * Gets URL information by short code
     */
    public ShortenedUrl getUrlInfo(String shortCode) {
        // Validate short code format first
        if (!urlGenerator.isValidShortCode(shortCode)) {
            return null; // Invalid format
        }
        
        return storage.getUrlByShortCode(shortCode);
    }

    /**
     * Validates URL format
     */
    private static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }

    /**
     * Normalizes URL by adding protocol if missing
     */
    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    /**
     * Gets the reason why a URL is not accessible
     */
    private static String getInaccessibilityReason(ShortenedUrl url) {
        if (url.getStatus() == UrlStatus.EXPIRED || url.isExpired()) {
            return "URL has expired";
        }
        if (url.getStatus() == UrlStatus.LIMIT_EXCEEDED || url.isClickLimitReached()) {
            return "Click limit exceeded";
        }
        if (url.getStatus() == UrlStatus.INACTIVE) {
            return "URL is inactive";
        }
        return "URL is not accessible";
    }

    /**
     * Starts the background cleanup task for expired URLs
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredUrls, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Cleans up expired URLs
     */
    public void cleanupExpiredUrls() {
        List<ShortenedUrl> expiredUrls = storage.getExpiredUrls();
        
        for (ShortenedUrl url : expiredUrls) {
            url.markAsExpired();
            storage.updateUrl(url);
            notificationService.notifyUrlExpired(url);
        }
        
        if (!expiredUrls.isEmpty()) {
            System.out.println("🧹 Cleaned up " + expiredUrls.size() + " expired URLs");
        }
    }

    /**
     * Gets storage statistics
     */
    public String getStatistics() {
        return String.format(
                """
                        📊 Storage Statistics:
                           Total URLs: %d
                           Total Users: %d
                           Active URLs: %d""",
            storage.getTotalUrlCount(),
            storage.getTotalUserCount(),
            storage.getAllUrls().stream()
                .mapToInt(url -> url.getStatus() == UrlStatus.ACTIVE ? 1 : 0)
                .sum()
        );
    }

    /**
     * Enable test mode to prevent browser opening during tests
     */
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /**
     * Shuts down the service and cleanup tasks
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
