package ru.example.url.shortener.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {
    private final UUID uuid;
    private final LocalDateTime createdAt;
    private final List<ShortenedUrl> shortenedUrls;

    public User() {
        this.uuid = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.shortenedUrls = new ArrayList<>();
    }

    public User(UUID uuid) {
        this.uuid = uuid;
        this.createdAt = LocalDateTime.now();
        this.shortenedUrls = new ArrayList<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ShortenedUrl> getShortenedUrls() {
        return shortenedUrls;
    }

    public void addShortenedUrl(ShortenedUrl url) {
        this.shortenedUrls.add(url);
    }

    public void removeShortenedUrl(ShortenedUrl url) {
        this.shortenedUrls.remove(url);
    }

    @Override
    public String toString() {
        return "User{" +
                "uuid=" + uuid +
                ", createdAt=" + createdAt +
                ", urlCount=" + shortenedUrls.size() +
                '}';
    }
}
