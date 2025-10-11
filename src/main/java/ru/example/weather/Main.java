package ru.example.weather;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Демонстрация работы сервиса получения метеорологических данных
 * 
 * Программа интерактивно запрашивает у пользователя:
 * - Широту (по умолчанию 55.75 - Москва)
 * - Долготу (по умолчанию 37.62 - Москва) 
 * - Количество дней прогноза (по умолчанию 7)
 * 
 * Для использования значений по умолчанию просто нажмите Enter
 */
public final class Main {
    
    private static final String API_KEY_FILE = "api_key.txt";
    private static final double DEFAULT_LON = 37.62;
    private static final double DEFAULT_LAT = 55.75;
    private static final int LAT_MAX = 90;
    private static final int LAT_MIN = -LAT_MAX;
    private static final int LON_MAX = 180;
    private static final int LON_MIN = -LON_MAX;
    private static final int DEFAULT_LIMIT = 7;

    private Main() {
    }

    private static String readApiKey() throws IOException {
        var sourceDir = Paths.get("src/main/java/ru/example/weather/");
        var apiKeyPath = sourceDir.resolve(API_KEY_FILE);
        
        if (!Files.exists(apiKeyPath)) {
            throw new IOException("Файл с API ключом не найден: " + apiKeyPath.toAbsolutePath() + 
                    "\nСоздайте файл api_key.txt в директории src/main/java/ru/example/weather/ " +
                    "и поместите в него ваш API ключ от Yandex Weather.");
        }
        
        var apiKey = Files.readString(apiKeyPath).trim();
        
        if (apiKey.isEmpty()) {
            throw new IOException("API ключ не настроен. Замените содержимое файла " + 
                    apiKeyPath.toAbsolutePath() + " на ваш реальный API ключ от Yandex Weather.");
        }
        
        return apiKey;
    }

    private record UserInput(
            double lat,
            double lon,
            int limit
    ) {
    }
    
    private static UserInput getUserInput() {
        try (var scanner = new Scanner(System.in)) {
            System.out.println("=== ВВОД ПАРАМЕТРОВ ===");
            System.out.println("(Нажмите Enter для использования значения по умолчанию)");
            
            var lat = DEFAULT_LAT;
            while (true) {
                try {
                    System.out.printf("Введите широту (по умолчанию %.2f): ", DEFAULT_LAT);
                    var latInput = scanner.nextLine().trim();
                    if (latInput.isEmpty()) {
                        break;
                    }
                    lat = Double.parseDouble(latInput);
                    if (lat < LAT_MIN || lat > LAT_MAX) {
                        System.out.println("Ошибка: широта должна быть от -90 до 90 градусов");
                        continue;
                    }
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка: введите корректное число");
                }
            }
            
            var lon = DEFAULT_LON;
            while (true) {
                try {
                    System.out.printf("Введите долготу (по умолчанию %.2f): ", DEFAULT_LON);
                    var lonInput = scanner.nextLine().trim();
                    if (lonInput.isEmpty()) {
                        break;
                    }
                    lon = Double.parseDouble(lonInput);
                    if (lon < LON_MIN || lon > LON_MAX) {
                        System.out.println("Ошибка: долгота должна быть от -180 до 180 градусов");
                        continue;
                    }
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка: введите корректное число");
                }
            }
            
            var limit = DEFAULT_LIMIT;
            while (true) {
                try {
                    System.out.printf("Введите количество дней прогноза (по умолчанию %d): ", DEFAULT_LIMIT);
                    var limitInput = scanner.nextLine().trim();
                    if (limitInput.isEmpty()) {
                        break;
                    }
                    limit = Integer.parseInt(limitInput);
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка: введите корректное целое число");
                }
            }
            return new UserInput(lat, lon, limit);
        }
    }
    
    public static void main(String[] args) {
        try {
            var apiKey = readApiKey();
            
            var input = getUserInput();
            
            System.out.println();
            System.out.println("=== ПОЛУЧЕНИЕ ДАННЫХ О ПОГОДЕ ===");
            System.out.printf("Координаты: %.2f, %.2f%n", input.lat, input.lon);
            System.out.println("Количество дней прогноза: " + input.limit);
            System.out.println();
            
            var weatherService = new WeatherService(apiKey);
            
            var weatherData = weatherService.getWeatherData(input.lat, input.lon, input.limit);
            System.out.println(weatherData);
            
        } catch (IOException e) {
            System.err.println("Ошибка при выполнении HTTP запроса: " + e.getMessage());
            System.err.println();
            System.err.println("ВАЖНО: Убедитесь, что вы:");
            System.err.println("1. Заполнили файл api_key.txt своим ключом от Yandex Weather API");
            System.err.println("2. Имеете доступ к интернету");
            System.err.println("3. Ваш API ключ действителен и активен");
            
        } catch (InterruptedException e) {
            System.err.println("Запрос был прерван: " + e.getMessage());
            Thread.currentThread().interrupt();
            
        } catch (URISyntaxException e) {
            System.err.println("Ошибка в формате URL: " + e.getMessage());
            
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
