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
}
