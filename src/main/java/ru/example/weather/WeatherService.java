package ru.example.weather;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Сервис для получения метеорологических данных из Yandex Weather API
 * 
 * Пример использования:
 * <pre>
 * String apiKey = "ваш_api_ключ";
 * WeatherService service = new WeatherService(apiKey);
 * WeatherData data = service.getWeatherData(55.75, 37.62, 7);
 * System.out.println("Температура: " + data.currentTemperature + "°C");
 * </pre>
 */
public class WeatherService {
    
    private static final String API_BASE_URL = "https://api.weather.yandex.ru/v2/forecast";
    private static final String API_KEY_HEADER = "X-Yandex-Weather-Key";
    private static final int OK_STATUS_CODE = 200;
    private static final int TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient;
    private final String apiKey;
    
    /**
     * Создает новый экземпляр сервиса с указанным API ключом
     * 
     * @param apiKey API ключ для доступа к Yandex Weather API
     */
    public WeatherService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    
    /**
     * Получает данные о погоде для указанных координат
     * 
     * @param lat широта
     * @param lon долгота
     * @param limit количество дней прогноза (по умолчанию 7)
     * @return объект с данными о погоде
     * @throws IOException если произошла ошибка при выполнении запроса
     * @throws InterruptedException если запрос был прерван
     */
    public WeatherData getWeatherData(double lat, double lon, int limit) 
            throws IOException, InterruptedException, URISyntaxException {
        
        var url = String.format("%s?lat=%.2f&lon=%.2f&limit=%d",
                API_BASE_URL, lat, lon, limit);
        
        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header(API_KEY_HEADER, apiKey)
                .header("Content-Type", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        
        var response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != OK_STATUS_CODE) {
            throw new IOException("API вернул код ошибки: " + response.statusCode() + 
                    ". Ответ: " + response.body());
        }
        
        var jsonResponse = response.body();
        return parseWeatherData(jsonResponse);
    }

    private static WeatherData parseWeatherData(String jsonResponse) {
        var weatherData = new WeatherData();
        weatherData.fullResponse = jsonResponse;
        
        // Извлекаем текущую температуру из fact.temp
        weatherData.currentTemperature = extractCurrentTemperature(jsonResponse);
        
        // Извлекаем средние температуры из прогноза
        weatherData.forecastTemperatures = extractForecastTemperatures(jsonResponse);
        
        // Вычисляем среднюю температуру по прогнозу
        weatherData.averageForecastTemperature = calculateAverageTemperature(
                weatherData.forecastTemperatures);
        
        return weatherData;
    }

    private static Integer extractCurrentTemperature(String json) {
        var pattern = Pattern.compile("\"fact\"\\s*:\\s*\\{[^}]*\"temp\"\\s*:\\s*(-?\\d+)");
        var matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        return null;
    }

    private static List<Integer> extractForecastTemperatures(String json) {
        var temperatures = new ArrayList<Integer>();

        var pattern = Pattern.compile(
                "\"day\"\\s*:\\s*\\{[^}]*\"temp_avg\"\\s*:\\s*(-?\\d+)");
        var matcher = pattern.matcher(json);
        
        while (matcher.find()) {
            temperatures.add(Integer.parseInt(matcher.group(1)));
        }
        
        return temperatures;
    }
    
    /**
     * Вычисляет среднее арифметическое температур
     */
    private static Double calculateAverageTemperature(List<Integer> temperatures) {
        if (temperatures.isEmpty()) {
            return null;
        }
        var sum = temperatures.stream().mapToDouble(temp -> temp).sum();
        return sum / temperatures.size();
    }
    
    /**
     * Класс для хранения данных о погоде
     */
    public static class WeatherData {
        public String fullResponse;
        public Integer currentTemperature;
        public List<Integer> forecastTemperatures;
        public Double averageForecastTemperature;
        
        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append("=== ДАННЫЕ О ПОГОДЕ ===\n\n");
            
            sb.append("Полный ответ от сервиса:\n");
            sb.append(fullResponse).append("\n\n");
            
            sb.append("=== АНАЛИЗ ДАННЫХ ===\n");
            sb.append("Текущая температура: ");
            if (currentTemperature != null) {
                sb.append(currentTemperature).append("°C\n");
            } else {
                sb.append("не найдена\n");
            }
            
            sb.append("Температуры по дням прогноза: ");
            if (forecastTemperatures != null && !forecastTemperatures.isEmpty()) {
                sb.append(forecastTemperatures).append("°C\n");
            } else {
                sb.append("не найдены\n");
            }
            
            sb.append("Средняя температура по прогнозу: ");
            if (averageForecastTemperature != null) {
                sb.append(String.format("%.1f°C\n", averageForecastTemperature));
            } else {
                sb.append("не вычислена\n");
            }
            
            return sb.toString();
        }
    }
}
