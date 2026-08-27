package io.micronaut.el.test;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class InterpreterParityFunctions {

    private InterpreterParityFunctions() {
    }

    public static String join(CharSequence... values) {
        return Arrays.stream(values).map(CharSequence::toString).collect(Collectors.joining(","));
    }

    public static long twice(long value) {
        return value * 2;
    }
}
