package ru.example.calculator;

import java.util.Scanner;

public class Calculator {
    private static final String COMMAND_EXIT = "s";
    private static final String COMMAND_CLEAR = "c";
    
    private static final String MSG_EXIT = "Завершение работы калькулятора.";
    private static final String MSG_CLEAR = "Результат сброшен!";
    private static final String MSG_INVALID_OPERATION = "Ошибка: Неподдерживаемая операция '%s'";
    private static final String MSG_USE_OPERATIONS = "Используйте: +, -, *, / или команды C/S";
    private static final String MSG_INVALID_NUMBER = "Ошибка: Введите корректное число!";
    
    private enum CommandResult {
        CONTINUE,
        EXIT,
        CLEAR_AND_RESTART
    }
    
    private double currentResult;
    private final Scanner scanner;

    public Calculator() {
        this.currentResult = 0.0;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Конструктор с начальным значением.
     *
     * @param initialValue начальное значение результата
     */
    public Calculator(double initialValue) {
        this.currentResult = initialValue;
        this.scanner = new Scanner(System.in);
    }

    public double getCurrentResult() {
        return currentResult;
    }

    public void setCurrentResult(double result) {
        this.currentResult = result;
    }

    /**
     * Сброс результата к нулю
     */
    public void clearResult() {
        this.currentResult = 0.0;
    }

    /**
     * Запуск калькулятора
     */
    public void start() {
        System.out.println("=== Калькулятор ===");
        System.out.println("Поддерживаемые операции: +, -, *, /");
        System.out.println("Команды управления:");
        System.out.println("  C/c - сброс результата");
        System.out.println("  S/s - выход из программы");
        System.out.println();
        
        if (!inputInitialNumber()) {
            return;
        }
        
        while (true) {
            System.out.println("Текущий результат: " + currentResult);
            
            System.out.print("Введите операцию (+, -, *, /) или команду (C/S): ");
            var operation = scanner.nextLine().trim();
            
            var commandResult = handleCommand(operation);
            if (commandResult == CommandResult.EXIT) {
                break;
            }
            if (commandResult == CommandResult.CLEAR_AND_RESTART) {
                continue;
            }
            
            // Проверяем, что это валидная операция
            if (!isValidOperation(operation)) {
                System.out.printf(MSG_INVALID_OPERATION + "%n", operation);
                System.out.println(MSG_USE_OPERATIONS);
                continue;
            }
            
            try {
                System.out.print("Введите число: ");
                var numberInput = scanner.nextLine().trim();
                
                var numberCommandResult = handleCommand(numberInput);
                if (numberCommandResult == CommandResult.EXIT) {
                    break;
                }
                if (numberCommandResult == CommandResult.CLEAR_AND_RESTART) {
                    continue;
                }
                
                var secondNumber = Double.parseDouble(numberInput);
                
                // Выполняем операцию с текущим результатом
                var result = performOperation(currentResult, operation, secondNumber);

                if (!Double.isNaN(result)) {
                    currentResult = result;
                    System.out.println("= " + currentResult);
                } else {
                    System.out.println("Операция не выполнена. Текущий результат: " + currentResult);
                }
                
            } catch (NumberFormatException e) {
                System.out.println(MSG_INVALID_NUMBER);
            } catch (Exception e) {
                System.out.println("Произошла ошибка: " + e.getMessage());
            }
            
            System.out.println();
        }
        
        scanner.close();
    }

    private boolean inputInitialNumber() {
        while (true) {
            try {
                System.out.print("Введите начальное число (или команду S для выхода): ");
                var input = scanner.nextLine().trim();
                
                // Проверяем команду выхода
                if (input.equalsIgnoreCase(COMMAND_EXIT)) {
                    System.out.println(MSG_EXIT);
                    return false;
                }

                currentResult = Double.parseDouble(input);
                System.out.println("Начальное значение: " + currentResult);
                System.out.println();
                return true;
                
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: Введите корректное число или команду S!");
            }
        }
    }

    private CommandResult handleCommand(String input) {
        if (input.equalsIgnoreCase(COMMAND_EXIT)) {
            System.out.println(MSG_EXIT);
            return CommandResult.EXIT;
        }
        
        if (input.equalsIgnoreCase(COMMAND_CLEAR)) {
            clearResult();
            System.out.println(MSG_CLEAR);
            System.out.println();
            
            // После сброса запрашиваем новое начальное число
            if (!inputInitialNumber()) {
                return CommandResult.EXIT;
            }
            return CommandResult.CLEAR_AND_RESTART;
        }
        
        return CommandResult.CONTINUE;
    }

    private static boolean isValidOperation(String operation) {
        return "+".equals(operation) || "-".equals(operation) ||
               "*".equals(operation) || "/".equals(operation);
    }

    private static double performOperation(
            double firstNumber,
            String operation,
            double secondNumber
    ) {
        switch (operation) {
            case "+":
                return add(firstNumber, secondNumber);
            case "-":
                return subtract(firstNumber, secondNumber);
            case "*":
                return multiply(firstNumber, secondNumber);
            case "/":
                return divide(firstNumber, secondNumber);
            default:
                System.out.println("Ошибка: Неподдерживаемая операция '" + operation + "'");
                System.out.println("Используйте: +, -, *, /");
                return Double.NaN;
        }
    }

    /**
     * Сложение двух чисел
     *
     * @param a первое число
     * @param b второе число
     * @return результат сложения
     */
    public static double add(double a, double b) {
        return a + b;
    }

    /**
     * Вычитание двух чисел
     *
     * @param a первое число
     * @param b второе число
     * @return результат вычитания
     */
    public static double subtract(double a, double b) {
        return a - b;
    }

    /**
     * Умножение двух чисел
     *
     * @param a первое число
     * @param b второе число
     * @return результат умножения
     */
    public static double multiply(double a, double b) {
        return a * b;
    }

    /**
     * Деление двух чисел с проверкой деления на ноль
     *
     * @param a делимое
     * @param b делитель
     * @return результат деления или NaN при делении на ноль
     */
    public static double divide(double a, double b) {
        if (b == 0) {
            System.out.println("Ошибка: Деление на ноль невозможно!");
            return Double.NaN;
        }
        return a / b;
    }
}