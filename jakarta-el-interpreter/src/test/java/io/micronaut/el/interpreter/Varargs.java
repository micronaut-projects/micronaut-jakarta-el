package io.micronaut.el.interpreter;

public final class Varargs {

    public interface NotFunctional {
        String map(String value);

        Integer map(Integer value);
    }
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

    public static String combine(String first, String... rest) {
        return first + String.join(",", rest);
    }

    public String argumentType(Object... values) {
        return values.length + ":" + values[0].getClass().getTypeName();
    }

    public String choose(Number first, Number second) {
        return "assignable";
    }

    public String choose(Long first, String second) {
        return "coercible";
    }

    public String specific(Number value) {
        return "number";
    }

    public String specific(Object value) {
        return "object";
    }

    public String reject(NotFunctional function) {
        return "unexpected";
    }

    public String numberText() {
        return "1";
    }

    public int[] getNumbers() {
        return new int[]{1, 2};
    }

    public String getValue() {
        return value;
    }
}
