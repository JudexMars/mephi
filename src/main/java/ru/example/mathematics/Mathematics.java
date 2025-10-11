package ru.example.mathematics;

public final class Mathematics {
    private Mathematics() {
    }

    public static double abs(double num) {
        if (num < 0) {
            return num * -1;
        }
        return num;
    }

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static int min(int a, int b) {
        return a < b ? a : b;
    }

    public static void main(String[] args) {
        int a = -6, b = -3;
        System.out.println(abs(a));
        System.out.println(max(a, b));
        System.out.println(min(a, b));
    }
}