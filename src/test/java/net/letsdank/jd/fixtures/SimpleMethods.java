package net.letsdank.jd.fixtures;

public class SimpleMethods {
    public int add(int a, int b) {
        return a + b;
    }

    public void empty() {
        // ничего
    }

    public int abs(int x) {
        if (x >= 0) {
            return x;
        } else {
            return -x;
        }
    }

    public void printHello() {
        System.out.println("Hello");
    }

    public void loop(int n) {
        int i = 0;
        while (i < n) {
            i++;
        }
    }
}
