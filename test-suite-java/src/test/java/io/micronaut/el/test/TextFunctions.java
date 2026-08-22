package io.micronaut.el.test;

import io.micronaut.el.annotation.ELFunction;

public final class TextFunctions {

    private TextFunctions() {
    }

    public static int length(String value) {
        return value == null ? 0 : value.length();
    }

    @ELFunction("upper")
    public static String toUpperCase(String value) {
        return value == null ? null : value.toUpperCase();
    }
}
