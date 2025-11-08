# URL Shortener Service

A comprehensive URL shortening service implemented in Java with unique features for user management, click tracking, and automatic expiration.

## Features

### Core Functionality
- Convert long URLs into short `clck.ru/xxxxxxx` format
- Different users get unique short codes for the same URL
- Monitor and limit the number of clicks per URL
- URLs automatically expire after 24 hours
- UUID-based user identification without authentication
- Automatic browser opening when accessing short URLs

### Advanced Features
- Automatic removal of expired URLs every 5 minutes
- User alerts for expired/blocked URLs
- Concurrent access support with in-memory storage
- Efficient short code generation using alphanumeric characters
- Comprehensive usage statistics and reporting

## Quick Start

### Running the Application

1. **Compile the project:**
   ```bash
   cd PROJECT_DIR
   ./gradlew build
   ```

2. **Run the main application:**
   ```bash
   ./gradlew run -PmainClass=ru.example.url.shortener.UrlShortenerApplication
   ```
## Usage Guide

### Console Interface

The application provides an interactive console menu:

```
🔗 URL SHORTENER - MAIN MENU
============================================================
1. 📝 Create short URL
2. 📋 View my URLs
3. 🌐 Access short URL
4. 📊 View URL statistics
5. 👤 User information
6. 🔧 System statistics
7. ❌ Exit
```

### User Workflow

1. **First Time Users:**
   - Choose "Create new user session"
   - Save your UUID for future sessions

2. **Returning Users:**
   - Choose "Continue with existing user ID"
   - Enter your saved UUID

3. **Creating Short URLs:**
   - Select option 1 from the menu
   - Enter your long URL (e.g., `https://www.baeldung.com/java-9-http-client`)
   - Receive your unique short URL (e.g., `clck.ru/abc123`)

4. **Accessing Short URLs:**
   - Select option 3 from the menu
   - Enter the short code or full URL
   - The original URL opens automatically in your browser

### Example Session

```
Enter the URL to shorten: https://www.baeldung.com/java-9-http-client

✅ SUCCESS!
📋 Original URL: https://www.baeldung.com/java-9-http-client
🔗 Short URL: clck.ru/aB3xY7z
🎯 Click limit: 100
⏰ Expires: 2024-11-09 14:30:00
```

## Technical Specifications

### Default Limits
- **Click Limit**: 100 clicks per URL
- **Expiration Time**: 24 hours from creation
- **Short Code Length**: 7 characters (Base62 encoded)
- **Cleanup Interval**: Every 5 minutes

### URL Format Support
- `https://example.com`
- `http://example.com`
- `www.example.com` (automatically adds https://)
- `example.com/path?query=value`
- `subdomain.example.com:8080/path`

## Limitations

- **In-Memory Storage**: Data is lost when application restarts
- **Single Instance**: No distributed storage or load balancing
- **Desktop Dependency**: Browser opening requires Desktop support
- **No Persistence**: URLs and users don't survive application restart