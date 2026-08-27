package io.micronaut.el.test;

public final class Varargs {

    private final String value;

    public Varargs(String... values) {
        value = String.join(",", values);
    }

    public String getValue() {
        return value;
    }
}
