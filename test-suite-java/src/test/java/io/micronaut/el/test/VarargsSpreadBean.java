package io.micronaut.el.test;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The instance methods with a variable arity parameter that an array argument is spread into, section 1.2.1.2 of
 * the specification and section 15.12.2 of the JLS. Kept identical to
 * {@code io.micronaut.el.interpreter.VarargsSpreadBean}.
 */
public class VarargsSpreadBean {

    public String argumentType(Object... values) {
        return values.length + ":" + values[0].getClass().getTypeName();
    }

    public String join(String separator, Object... parts) {
        return Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining(separator));
    }

    public static String staticCount(Object... values) {
        return values.length + ":" + values[0].getClass().getTypeName();
    }
}
