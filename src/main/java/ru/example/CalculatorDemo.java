package ru.example;

/**
 * Демонстрационный класс для показа возможностей накопительного ООП калькулятора
 */
public final class CalculatorDemo {
    public static void main(String[] args) {
        System.out.println("=== Демонстрация Накопительного Калькулятора ===\n");
        
        // Создаем калькулятор с начальным значением
        Calculator calc = new Calculator(10.0);
        System.out.println("Создан калькулятор с начальным значением: " + calc.getCurrentResult());
        
        // Демонстрируем различные операции
        System.out.println("\n--- Демонстрация статических операций ---");
        
        double result1 = Calculator.add(5.0, 3.0);
        System.out.println("5.0 + 3.0 = " + result1);
        
        double result2 = Calculator.subtract(10.0, 4.0);
        System.out.println("10.0 - 4.0 = " + result2);
        
        double result3 = Calculator.multiply(6.0, 7.0);
        System.out.println("6.0 * 7.0 = " + result3);
        
        double result4 = Calculator.divide(15.0, 3.0);
        System.out.println("15.0 / 3.0 = " + result4);
        
        // Демонстрируем обработку ошибок
        System.out.println("\n--- Демонстрация обработки ошибок ---");
        double errorResult = Calculator.divide(10.0, 0.0);
        System.out.println("Результат деления на ноль: " + errorResult);
        
        // Демонстрируем работу с состоянием в накопительном режиме
        System.out.println("\n--- Демонстрация накопительного режима ---");
        Calculator accumulator = new Calculator(100.0);
        System.out.println("Начальное значение: " + accumulator.getCurrentResult());
        
        // Симулируем последовательность операций как в настоящем калькуляторе
        System.out.println("Операция: 100 + 50");
        double step1 = Calculator.add(accumulator.getCurrentResult(), 50.0);
        accumulator.setCurrentResult(step1);
        System.out.println("Результат: " + accumulator.getCurrentResult());
        
        System.out.println("Операция: " + accumulator.getCurrentResult() + " * 2");
        double step2 = Calculator.multiply(accumulator.getCurrentResult(), 2.0);
        accumulator.setCurrentResult(step2);
        System.out.println("Результат: " + accumulator.getCurrentResult());
        
        System.out.println("Операция: " + accumulator.getCurrentResult() + " / 4");
        double step3 = Calculator.divide(accumulator.getCurrentResult(), 4.0);
        accumulator.setCurrentResult(step3);
        System.out.println("Результат: " + accumulator.getCurrentResult());
        
        // Демонстрируем сброс
        System.out.println("\nСброс калькулятора...");
        accumulator.clearResult();
        System.out.println("После сброса: " + accumulator.getCurrentResult());
        
        // Создаем второй калькулятор для демонстрации инкапсуляции
        Calculator calc2 = new Calculator(25.0);
        System.out.println("\nВторой калькулятор: " + calc2.getCurrentResult());
        System.out.println("Первый калькулятор: " + accumulator.getCurrentResult());
        
        System.out.println("\n=== Демонстрация завершена ===");
        System.out.println("Для интерактивной работы запустите ru.example.PlayGround.main()");
        System.out.println("\nВ интерактивном режиме:");
        System.out.println("- Введите начальное число");
        System.out.println("- Затем операцию и число для накопительных вычислений");
        System.out.println("- Используйте 'C' для сброса, 'S' для выхода");
    }

    private CalculatorDemo() {
    }
}
