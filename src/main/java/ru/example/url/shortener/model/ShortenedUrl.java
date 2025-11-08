package ru.example.url.shortener.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ShortenedUrl {
    private final String originalUrl;
    private final String shortCode;
    private final UUID userId;
    private int clickCount;
    private final int maxClicks;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private UrlStatus status;

    public ShortenedUrl(String originalUrl, String shortCode, UUID userId, int maxClicks, LocalDateTime expiresAt) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.userId = userId;
        this.clickCount = 0;
        this.maxClicks = maxClicks;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.status = UrlStatus.ACTIVE;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getFullShortUrl() {
        return "clck.ru/" + shortCode;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getClickCount() {
        return clickCount;
    }

    public int getMaxClicks() {
        return maxClicks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public UrlStatus getStatus() {
        return status;
    }

    public void setStatus(UrlStatus status) {
        this.status = status;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isClickLimitReached() {
        return clickCount >= maxClicks;
    }

    public boolean isAccessible() {
        return status == UrlStatus.ACTIVE && !isExpired() && !isClickLimitReached();
    }

    public void incrementClickCount() {
        this.clickCount++;
        if (isClickLimitReached()) {
            this.status = UrlStatus.LIMIT_EXCEEDED;
        }
    }

    public void markAsExpired() {
        this.status = UrlStatus.EXPIRED;
    }

    @Override
    public String toString() {
        return "ShortenedUrl{" +
                "originalUrl='" + originalUrl + '\'' +
                ", shortCode='" + shortCode + '\'' +
                ", clickCount=" + clickCount +
                ", maxClicks=" + maxClicks +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
