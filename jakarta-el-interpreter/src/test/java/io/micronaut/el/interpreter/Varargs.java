package io.micronaut.el.interpreter;

import java.util.function.Function;
import java.util.function.Predicate;

public final class Varargs {

    public interface NotFunctional {
        String map(String value);

        Integer map(Integer value);
    }

    public sealed interface SealedFunction permits SealedFunctionImpl {
        String map(String value);
    }

    public static final class SealedFunctionImpl implements SealedFunction {
        @Override
        public String map(String value) {
            return value;
        }
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

    public String pick(Number value) {
        return "number";
    }

    public String pick(Integer value) {
        return "integer";
    }

    public String route(Predicate<String> predicate) {
        return "predicate";
    }

    public String route(Function<String, String> function) {
        return "function";
    }

    public String reject(NotFunctional function) {
        return "unexpected";
    }

    public String rejectSealed(SealedFunction function) {
        return "unexpected";
    }

    public String compatible(Object value) {
        return value.getClass().getSimpleName();
    }

    public String expanded(String... values) {
        return String.join(",", values);
    }

    public String emptyVarargs(String... values) {
        return "string";
    }

    public String emptyVarargs(Integer... values) {
        return "integer";
    }

    public String numeric(Integer value) {
        return "integer";
    }

    public String numeric(Double value) {
        return "double";
    }

    public String boxed(int value) {
        return "primitive";
    }

    public String boxed(Integer value) {
        return "wrapper";
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
