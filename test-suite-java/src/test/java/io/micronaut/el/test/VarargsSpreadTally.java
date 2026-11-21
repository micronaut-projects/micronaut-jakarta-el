package io.micronaut.el.test;

/**
 * A constructor with a variable arity parameter. Kept identical to
 * {@code io.micronaut.el.interpreter.VarargsSpreadTally}.
 */
public class VarargsSpreadTally {

    private final String summary;

    public VarargsSpreadTally(Object... values) {
        this.summary = values.length + ":" + values[0].getClass().getTypeName();
    }

    public String getSummary() {
        return summary;
    }
}
