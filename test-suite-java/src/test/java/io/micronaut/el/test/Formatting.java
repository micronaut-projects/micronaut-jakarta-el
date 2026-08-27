package io.micronaut.el.test;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

@Introspected
public class Formatting {

    public interface TextMapper {
        String apply(String value);

        default String decorate(String value) {
            return "default:" + apply(value);
        }
    }

    @Executable
    public String join(String separator, Object... parts) {
        return Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining(separator));
    }

    @Executable
    public String join(String separator) {
        return "only:" + separator;
    }

    @Executable
    public int size(Object[] values) {
        return values.length;
    }

    @Executable
    public int twice(int value) {
        return value * 2;
    }

    @Executable
    public String format(String format, Object... args) {
        return String.format(format, args);
    }

    @Executable
    public String argumentType(Object... values) {
        return values.length + ":" + values[0].getClass().getTypeName();
    }

    public int[] getNumbers() {
        return new int[]{1, 2};
    }

    @Executable
    public String map(TextMapper mapper, String value) {
        return mapper.decorate(value);
    }

    @Executable
    public String select(Integer value) {
        return "integer";
    }

    @Executable
    public String select(String value) {
        return "string";
    }

    @Executable
    public String ambiguous(Comparable<?> value) {
        return "comparable";
    }

    @Executable
    public String ambiguous(Serializable value) {
        return "serializable";
    }

    @Executable
    public String ambiguousCoercion(Integer value) {
        return "integer";
    }

    @Executable
    public String ambiguousCoercion(Long value) {
        return "long";
    }
}
