package net.letsdank.jd.fixtures;

public class ArrayFixtures {

    // int[] + IALOAD/IASTORE/ARRAYLENGTH
    public int sum(int[] arr) {
        int s = 0;
        for (int i = 0; i < arr.length; i++) {
            s += arr[i];
        }
        return s;
    }

    // Новый ссылочный массив: ANEWARRAY
    public String[] makeString(int n) {
        return new String[n];
    }

    // Обращение к длине массива: ARRAYLENGTH
    public boolean hasLength(String[] arr, int expected) {
        return arr.length == expected;
    }

    // Запись в массив: AASTORE
    public void fillWithHello(String[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = "Hello";
        }
    }
}
