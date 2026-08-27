package io.micronaut.el.test;

public final class VarargsConstructor {

    private final String value;

    public VarargsConstructor(String... values) {
        value = String.join(",", values);
    }

    public String getValue() {
        return value;
    }
}
