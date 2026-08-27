package io.micronaut.el.interpreter;

public final class Varargs {
    private final String value;

    public Varargs(String... values) {
        value = String.join(",", values);
    }

    public String join(String... values) {
        return String.join(",", values);
    }

    public static String join(CharSequence... values) {
        return String.join(",", values);
    }

    public String argumentType(Object... values) {
        return values.length + ":" + values[0].getClass().getTypeName();
    }

    public int[] getNumbers() {
        return new int[]{1, 2};
    }

    public String getValue() {
        return value;
    }
}
