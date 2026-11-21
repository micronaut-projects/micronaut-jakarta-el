package io.micronaut.el.test;

/**
 * Overloads a primitive and its wrapper tell apart. Kept identical to
 * {@code io.micronaut.el.interpreter.PrimitiveVariableBean}.
 */
public class PrimitiveVariableBean {

    public int getCount() {
        return 2;
    }

    public String which(int value) {
        return "int:" + value;
    }

    public String which(Integer value) {
        return "Integer:" + value;
    }

    public String which(long value) {
        return "long:" + value;
    }

    public String which(Long value) {
        return "Long:" + value;
    }

    public String repeat(int times) {
        return "ab".repeat(times);
    }
}
