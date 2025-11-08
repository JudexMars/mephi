package ru.example.url.shortener.storage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ru.example.url.shortener.model.ShortenedUrl;
import ru.example.url.shortener.model.UrlStatus;
import ru.example.url.shortener.model.User;

public final class UrlStorage {
    private static UrlStorage instance;
    private final Map<UUID, User> users;
    private final Map<String, ShortenedUrl> shortCodeToUrl;
    private final Map<UUID, List<ShortenedUrl>> userUrls;

    private UrlStorage() {
        this.users = new ConcurrentHashMap<>();
        this.shortCodeToUrl = new ConcurrentHashMap<>();
        this.userUrls = new ConcurrentHashMap<>();
    }

    public static synchronized UrlStorage getInstance() {
        if (instance == null) {
            instance = new UrlStorage();
        }
        return instance;
    }

    public User createUser() {
        User user = new User();
        users.put(user.getUuid(), user);
        userUrls.put(user.getUuid(), new ArrayList<>());
        return user;
    }

    public User getUser(UUID userId) {
        return users.get(userId);
    }

    public void saveUrl(ShortenedUrl url) {
        shortCodeToUrl.put(url.getShortCode(), url);
        
        // Add to user's URL list
        List<ShortenedUrl> urls = userUrls.get(url.getUserId());
        if (urls != null) {
            urls.add(url);
        }
        
        // Add to user object as well
        User user = users.get(url.getUserId());
        if (user != null) {
            user.addShortenedUrl(url);
        }
    }

    public ShortenedUrl getUrlByShortCode(String shortCode) {
        return shortCodeToUrl.get(shortCode);
    }

    public List<ShortenedUrl> getUserUrls(UUID userId) {
        return userUrls.getOrDefault(userId, new ArrayList<>());
    }

    public boolean isShortCodeExists(String shortCode) {
        return shortCodeToUrl.containsKey(shortCode);
    }

    public List<ShortenedUrl> getExpiredUrls() {
        LocalDateTime now = LocalDateTime.now();
        return shortCodeToUrl.values().stream()
                .filter(url -> url.getExpiresAt().isBefore(now) && url.getStatus() == UrlStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public void updateUrl(ShortenedUrl url) {
        shortCodeToUrl.put(url.getShortCode(), url);
    }

    public int getTotalUrlCount() {
        return shortCodeToUrl.size();
    }

    public int getTotalUserCount() {
        return users.size();
    }

    public List<ShortenedUrl> getAllUrls() {
        return new ArrayList<>(shortCodeToUrl.values());
    }

    public void clear() {
        users.clear();
        shortCodeToUrl.clear();
        userUrls.clear();
    }
}
