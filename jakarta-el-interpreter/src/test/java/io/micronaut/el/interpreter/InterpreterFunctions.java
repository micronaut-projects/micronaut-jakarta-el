package io.micronaut.el.interpreter;

public final class InterpreterFunctions {
    public String map(TextMapper mapper, String value) {
        return mapper.decorate(value);
    }

    public String select(Integer value) {
        return "integer";
    }

    public String select(String value) {
        return "string";
    }

    public String argumentType(Object... values) {
        return values.length + ":" + values[0].getClass().getTypeName();
    }

    public int[] getNumbers() {
        return new int[]{1, 2};
    }

    public static boolean identity(boolean value) {
        return value;
    }

    public interface TextMapper {
        String apply(String value);

        default String decorate(String value) {
            return "default:" + apply(value);
        }
    }
}
