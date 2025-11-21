package net.letsdank.jd.fixtures;

public class SimpleMethods {

    private int value;
    private static int counter;

    public void setValue(int x) {
        this.value=x;
    }

    public void incCounter(){
        counter++;
    }

    public void staticCall() {
        int y = Math.abs(-1);
    }

    public void forLoop(int n){
        for(int i = 0; i < n; i++){
            System.out.println(i);
        }
    }

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
