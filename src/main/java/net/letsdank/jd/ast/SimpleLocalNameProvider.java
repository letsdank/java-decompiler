package net.letsdank.jd.ast;

public final class SimpleLocalNameProvider implements LocalNameProvider {
    @Override
    public String nameForLocal(int index) {
        // Позже можно использовать дескриптор метода, this и т.п.
        return "v" + index;
    }
}
