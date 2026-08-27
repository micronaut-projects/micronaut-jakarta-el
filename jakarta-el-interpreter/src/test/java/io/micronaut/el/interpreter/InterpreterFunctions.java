package io.micronaut.el.interpreter;

public final class InterpreterFunctions {
    public String map(TextMapper mapper, String value) {
        return mapper.apply(value);
    }

    public interface TextMapper {
        String apply(String value);
    }
}
