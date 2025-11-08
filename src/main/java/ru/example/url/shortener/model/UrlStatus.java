package ru.example.url.shortener.model;

public enum UrlStatus {
    ACTIVE,      // URL is active and can be accessed
    EXPIRED,     // URL has expired due to time limit
    LIMIT_EXCEEDED, // URL has reached its click limit
    INACTIVE     // URL has been manually deactivated
}
