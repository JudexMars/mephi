package ru.example.sandbox;

import java.io.ByteArrayInputStream;

public class Main {
    public static void main(String[] args) {
        var in = new ByteArrayInputStream(new byte[]{'A', 'B', 'C'});
        int x;
        while ((x = in.read()) != -1) {
            System.out.print((char) x);
        }
    }
}
