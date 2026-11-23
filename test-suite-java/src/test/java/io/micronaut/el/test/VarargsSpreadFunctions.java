package io.micronaut.el.test;

/**
 * A static method with a variable arity parameter, declared as a function and statically imported. Kept identical
 * to {@code io.micronaut.el.interpreter.VarargsSpreadFunctions}.
 */
public final class VarargsSpreadFunctions {

    private VarargsSpreadFunctions() {
    }

    public static String count(Object... values) {
        return values.length + ":" + values[0].getClass().getTypeName();
    }
}
