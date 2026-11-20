package io.micronaut.el.test;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * The functions of the compiler regressions, declared statically. The interpreted counterpart is
 * {@code io.micronaut.el.interpreter.CompilerRegressionFunctions}.
 */
public final class CompilerRegressionFunctions {

    private CompilerRegressionFunctions() {
    }

    public static String hitS(String value) {
        CompilerRegressionProbe.LOG.add(value);
        return value;
    }

    public static void nothing() {
        CompilerRegressionProbe.LOG.add("nothing");
    }

    public static String vs(String... parts) {
        return String.join("+", parts) + "#" + parts.length;
    }

    public static String cmp(Comparable<?> value) {
        return "cmp:" + value;
    }

    public static String ser(Serializable value) {
        return "ser:" + value;
    }

    public static String list(List<?> value) {
        return "list:" + value;
    }

    public static String read(String value) throws IOException {
        if ("fail".equals(value)) {
            throw new IOException("checked");
        }
        return "read:" + value;
    }
}
