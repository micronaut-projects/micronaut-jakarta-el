package io.micronaut.el.test;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class InterpreterParityFunctions {

    private InterpreterParityFunctions() {
    }

    public static String join(CharSequence... values) {
        return Arrays.stream(values).map(CharSequence::toString).collect(Collectors.joining(","));
    }

    public static String joinDifferently(CharSequence... values) {
        return Arrays.stream(values).map(CharSequence::toString).collect(Collectors.joining(";"));
    }

    public static long twice(long value) {
        return value * 2;
    }

    public static boolean identity(boolean value) {
        return value;
    }

    public static String shadow(String value) {
        return "mapped:" + value;
    }
}
