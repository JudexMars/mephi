# Сервис получения метеорологических данных

Этот сервис предназначен для получения метеорологических данных из Yandex Weather API.

## Функциональность

- Получение данных о погоде по координатам (широта и долгота)
- Вывод полного JSON ответа от сервиса
- Извлечение текущей температуры из поля `fact.temp`
- Вычисление средней температуры по прогнозу на основе `forecasts[*].parts.day.temp_avg`

## Настройка

1. Получите API ключ от Yandex Weather API по адресу: https://yandex.ru/dev/weather/doc/ru/concepts/how-to
2. Создайте файл `api_key.txt`:
3. Введите туда свой API ключ

## Использование

### Запуск демонстрации

```bash
cd /Users/bondarchukdo/IdeaProjects/MEPHI
./gradlew run --args="ru.example.weather.Main"
```