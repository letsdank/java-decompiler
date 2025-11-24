package net.letsdank.jd.fixtures;

public class TypeFixtures {

    public boolean isString(Object o) {
        return o instanceof String;
    }

    public Number castToNumber(Object o) {
        return (Number) o;
    }

    public String safeCast(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        return null;
    }
}
